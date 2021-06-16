import torch
from torch.utils.mobile_optimizer import optimize_for_mobile

model = torch.hub.load('facebookresearch/deit:main', 'deit_base_patch16_224', pretrained=True)
quantized_model = torch.quantization.quantize_dynamic(model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
ts_model = torch.jit.script(quantized_model)
optimized_torchscript_model = optimize_for_mobile(ts_model)
optimized_torchscript_model.save("fbdeit.pt")
