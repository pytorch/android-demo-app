import pyaudio
import queue
import numpy as np
import torch
import torchaudio


def get_demo_wrapper():
    wrapper = torch.jit.load("scripted_wrapper_tuple.pt")
    return wrapper


wrapper = get_demo_wrapper()


################################################################


data_queue = queue.Queue()


def callback(in_data, frame_count, time_info, status):
    global data_queue
    data_queue.put(in_data)
    return in_data, pyaudio.paContinue


state = None
hypo = None


def transcribe(np_array, should_print=True):
    global state, hypo
    tensor = torch.tensor(np_array)
    transcript, hypo, state = wrapper(tensor, hypo, state)
    if should_print and transcript:
        print(transcript, end="", flush=True)


previous_right_context = None


def process(should_print=True):
    global previous_right_context
    if previous_right_context is None:
        previous_right_context = [
            np.frombuffer(data_queue.get(), dtype=np.float32) for _ in range(1)
        ]

    # Get 4 segments.
    segments = [
        np.frombuffer(data_queue.get(), dtype=np.float32) for _ in range(4)
    ]

    current_input = previous_right_context + segments

    with torch.no_grad():
        transcribe(np.concatenate(current_input), should_print=should_print)

    # Save right context.
    previous_right_context = current_input[-1:]


# Emformer is configured with input segment size of 4 and right context size of 1.
# Pre- time reduction with factor 4, then, we have an input segment size of 16 and
# right context size of 4 going into RNN-T.
# With a hop length of 160 samples, we then have 16 * 160 = 2560 samples in the input segment
# and 4 * 160 = 640 samples in the right context.
# Then, since the lowest common factor between 640 and 3600 is 640, we'll
# read from the stream in 640-sample increments.

p = pyaudio.PyAudio()

CHANNELS = 1
RATE = 16000

stream = p.open(
    format=pyaudio.paFloat32,
    channels=CHANNELS,
    rate=RATE,
    input=True,
    output=False,
    frames_per_buffer=640,
    stream_callback=callback,
)

stream.start_stream()

# We need to initialize the model by evaluating
# a few samples.
# If we skip this, evaluation latency will become
# prohibitively large.
print("Initializing model...")
for _ in range(10):
    process(should_print=False)

print("Initialization complete.")

data_queue = queue.Queue()
previous_right_context = None
state = None
prev_hypo = None

while stream.is_active():
    process(should_print=True)

stream.stop_stream()
stream.close()
