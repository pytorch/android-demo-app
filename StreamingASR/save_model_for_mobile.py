import torch
import torchaudio
from torch.utils.mobile_optimizer import optimize_for_mobile

def get_demo_wrapper():
    wrapper = torch.jit.load("scripted_wrapper_tuple.pt")
    return wrapper

wrapper = get_demo_wrapper()
scripted_model = torch.jit.script(wrapper)
optimized_model = optimize_for_mobile(scripted_model)
optimized_model._save_for_lite_interpreter("streaming_asrv2.ptl")
print("Done _save_for_lite_interpreter")
