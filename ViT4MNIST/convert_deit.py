import torch
from torch.utils.mobile_optimizer import optimize_for_mobile

model = torch.hub.load('facebookresearch/deit:main', 'deit_base_patch16_224', pretrained=True)
ts_model = torch.jit.script(model)
optimized_torchscript_model = optimize_for_mobile(ts_model)
optimized_torchscript_model.save("fbdeit.pt")
