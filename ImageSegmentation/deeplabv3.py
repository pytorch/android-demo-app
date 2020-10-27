import torch

model = torch.hub.load('pytorch/vision:v0.7.0', 'deeplabv3_resnet50', pretrained=True)
model.eval()

scriptedm = torch.jit.script(model)
torch.jit.save(scriptedm, "deeplabv3_scripted.pt")

