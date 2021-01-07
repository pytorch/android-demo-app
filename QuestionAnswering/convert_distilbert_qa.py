import torch
from transformers import DistilBertTokenizer, DistilBertForQuestionAnswering
from torch.utils.mobile_optimizer import optimize_for_mobile

tokenizer = DistilBertTokenizer.from_pretrained('distilbert-base-uncased-distilled-squad')
model = DistilBertForQuestionAnswering.from_pretrained('distilbert-base-uncased-distilled-squad')
model.eval()

question, text = "When will support for GPU be available?!", "There is a growing need to execute ML models on edge devices to reduce latency, preserve privacy and enable new interactive use cases. In the past, engineers used to train models separately. They would then go through a multi-step, error prone and often complex process to transform the models for execution on a mobile device. The mobile runtime was often significantly different from the operations available during training leading to inconsistent developer and eventually user experience. PyTorch Mobile removes these friction surfaces by allowing a seamless process to go from training to deployment by staying entirely within the PyTorch ecosystem. It provides an end-to-end workflow that simplifies the research to production environment for mobile devices. In addition, it paves the way for privacy-preserving features via Federated Learning techniques. PyTorch Mobile is in beta stage right now and in wide scale production use. It will soon be available as a stable release once the APIs are locked down. Key features of PyTorch Mobile: Available for iOS, Android and Linux; Provides APIs that cover common preprocessing and integration tasks needed for incorporating ML in mobile applications; Support for tracing and scripting via TorchScript IR; Support for XNNPACK floating point kernel libraries for Arm CPUs; Integration of QNNPACK for 8-bit quantized kernels. Includes support for per-channel quantization, dynamic quantization and more; Build level optimization and selective compilation depending on the operators needed for user applications, i.e., the final binary size of the app is determined by the actual operators the app needs; Support for hardware backends like GPU, DSP, NPU will be available soon."
inputs = tokenizer(question, text, return_tensors='pt')
model_dynamic_quantized = torch.quantization.quantize_dynamic(model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
traced_model = torch.jit.trace(model_dynamic_quantized, inputs['input_ids'], strict=False)
optimized_traced_model = optimize_for_mobile(traced_model)
torch.jit.save(optimized_traced_model, "qa360_quantized.pt")
# 360 is the length of model input, i.e. the length of the tokenized ids of question+text
