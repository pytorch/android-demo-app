import torch
from torch.utils.mobile_optimizer import optimize_for_mobile

model = torch.hub.load('pytorch/vision:v0.11.0', 'deeplabv3_resnet50', pretrained=True)
model.eval()

scripted_module = torch.jit.script(model)
optimized_scripted_module = optimize_for_mobile(scripted_module)

# Export full jit version model (not compatible with lite interpreter)
scripted_module.save("deeplabv3_scripted.pt")
# Export lite interpreter version model (compatible with lite interpreter)
scripted_module._save_for_lite_interpreter("deeplabv3_scripted.ptl")
# using optimized lite interpreter model makes inference about 60% faster than the non-optimized lite interpreter model, which is about 6% faster than the non-optimized full jit model
optimized_scripted_module._save_for_lite_interpreter("deeplabv3_scripted_optimized.ptl")
