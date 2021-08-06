import torch
import joblib
import cnn_models
from torch.utils.mobile_optimizer import optimize_for_mobile

lb = joblib.load('lb.pkl')
model = cnn_models.CustomCNN()
model.load_state_dict(torch.load('asl.pth'))

scripted_module = torch.jit.script(model)
optimized_scripted_module = optimize_for_mobile(scripted_module)
optimized_scripted_module._save_for_lite_interpreter("asl.ptl")
