import torch
from torch import Tensor
from torch.utils.mobile_optimizer import optimize_for_mobile
import torchaudio
from torchaudio.models.wav2vec2.utils.import_huggingface import import_huggingface_model
from transformers import Wav2Vec2ForCTC

# Wav2vec2 model emits sequences of probability (logits) distributions over the characters
# The following class adds steps to decode the transcript (best path)
class SpeechRecognizer(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model
        self.labels = [
            "<s>", "<pad>", "</s>", "<unk>", "|", "E", "T", "A", "O", "N", "I", "H", "S",
            "R", "D", "L", "U", "M", "W", "C", "F", "G", "Y", "P", "B", "V", "K", "'", "X",
            "J", "Q", "Z"]

    def forward(self, waveforms: Tensor) -> str:
        """Given a single channel speech data, return transcription.

        Args:
            waveforms (Tensor): Speech tensor. Shape `[1, num_frames]`.

        Returns:
            str: The resulting transcript
        """
        logits, _ = self.model(waveforms)  # [batch, num_seq, num_label]
        best_path = torch.argmax(logits[0], dim=-1)  # [num_seq,]
        prev = ''
        hypothesis = ''
        for i in best_path:
            char = self.labels[i]
            if char == prev:
                continue
            if char == '<s>':
                prev = ''
                continue
            hypothesis += char
            prev = char
        return hypothesis.replace('|', ' ')


# Load Wav2Vec2 pretrained model from Hugging Face Hub
model = Wav2Vec2ForCTC.from_pretrained("facebook/wav2vec2-base-960h")
# Convert the model to torchaudio format, which supports TorchScript.
model = import_huggingface_model(model)
# Remove weight normalization which is not supported by quantization.
model.encoder.transformer.pos_conv_embed.__prepare_scriptable__()
model = model.eval()
# Attach decoder
model = SpeechRecognizer(model)

# Apply quantization / script / optimize for motbile
quantized_model = torch.quantization.quantize_dynamic(
    model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
scripted_model = torch.jit.script(quantized_model)
optimized_model = optimize_for_mobile(scripted_model)

# Sanity check
waveform , _ = torchaudio.load('scent_of_a_woman_future.wav')
print('Result:', optimized_model(waveform))

optimized_model._save_for_lite_interpreter("wav2vec2.ptl")

