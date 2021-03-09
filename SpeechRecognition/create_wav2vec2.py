import soundfile as sf
import torch
from transformers import Wav2Vec2ForCTC, Wav2Vec2Tokenizer
from torch.utils.mobile_optimizer import optimize_for_mobile

tokenizer = Wav2Vec2Tokenizer.from_pretrained("facebook/wav2vec2-base-960h")
model = Wav2Vec2ForCTC.from_pretrained("facebook/wav2vec2-base-960h")
model.eval()

audio_input, _ = sf.read("scent_of_a_woman_future.wav")
input_values = tokenizer(audio_input, return_tensors="pt").input_values
print(input_values.shape) # input_values is of 65024 long, matched INPUT_SIZE defined in Android code

logits = model(input_values).logits
predicted_ids = torch.argmax(logits, dim=-1)
transcription = tokenizer.batch_decode(predicted_ids)[0]
print(transcription)

traced_model = torch.jit.trace(model, input_values, strict=False)
model_dynamic_quantized = torch.quantization.quantize_dynamic(model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
traced_quantized_model = torch.jit.trace(model_dynamic_quantized, input_values, strict=False)

optimized_traced_quantized_model = optimize_for_mobile(traced_quantized_model)
optimized_traced_quantized_model.save("app/src/main/assets/wav2vec_traced_quantized.pt")
