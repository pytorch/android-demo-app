# Question Answering on Android with Kotlin

## Introduction

Question Answering (QA) is one of the common and challenging Natural Language Processing tasks. With the revolutionary transformed-based [Bert](https://arxiv.org/abs/1810.04805) model coming out in October 2018, question answering models have reached their state of art accuracy by fine-tuning Bert-like models on QA datasets such as [Squad](https://rajpurkar.github.io/SQuAD-explorer). [Huggingface](https://huggingface.co)'s [DistilBert](https://huggingface.co/transformers/model_doc/distilbert.html) is a smaller and faster version of BERT - DistilBert "has 40% less parameters than bert-base-uncased, runs 60% faster while preserving over 95% of BERT’s performances as measured on the GLUE language understanding benchmark."

In this demo app, written in Kotlin, we'll show how to quantize and convert the Huggingface's DistilBert QA model to TorchScript and how to use the scripted model on an Android demo app to perform question answering.

## Prerequisites

* PyTorch 1.7 or later (Optional)
* Python 3.8 (Optional)
* Android Pytorch library 1.7 or later
* Android Studio 4.0.1 or later

## Quick Start

To Test Run the Android QA App, run the following commands on a Terminal:

### 1. Prepare the Model

If you don't have PyTorch installed or want to have a quick try of the demo app, you can download the scripted QA model compressed in a zip file [here](https://drive.google.com/file/d/1RWZa_5oSQg5AfInkn344DN3FJ5WbbZbq/view?usp=sharing), then unzip it to the assets folder, and continue to Step 2.

Be aware that the downloadable model file was created with PyTorch 1.7.0, matching the PyTorch Android library 1.7.0 specified in the project's `build.gradle` file as `implementation 'org.pytorch:pytorch_android:1.7.0'`. If you use a different version of PyTorch to create your model by following the instructions below, make sure you specify the same PyTorch Android library version in the `build.gradle` file to avoid possible errors caused by the version mismatch. Furthermore, if you want to use the latest PyTorch master code to create the model, follow the steps at [Building PyTorch Android from Source](https://pytorch.org/mobile/android/#building-pytorch-android-from-source) and [Using the PyTorch Android Libraries Built](https://pytorch.org/mobile/android/#using-the-pytorch-android-libraries-built-from-source-or-nightly) on how to use the model in Android.

With PyTorch 1.7 installed, first install the Huggingface `transformers` by running `pip install transformers` (the versions that have been tested are 4.0.0 and 4.1.1), then run `python convert_distilbert_qa.py`.

Note that a pre-defined question and text, resulting in the size of the input tokens (of question and text) being 360, is used in the `convert_distilbert_qa.py`, and 360 is the maximum token size for the user text and question in the app. If the token size of the inputs of the text and question is less than 360, padding will be needed to make the model work correctly.

After the script completes, copy the model file qa360_quantized.pt to the Android app's assets folder. [Dynamic quantization](https://pytorch.org/tutorials/intermediate/dynamic_quantization_bert_tutorial.html) is used to quantize the model to reduce its size to half, without causing inference difference in question answering - you can verify this by changing the last 4 lines of code in `convert_distilbert_qa.py` from:

```
model_dynamic_quantized = torch.quantization.quantize_dynamic(model, qconfig_spec={torch.nn.Linear}, dtype=torch.qint8)
traced_model = torch.jit.trace(model_dynamic_quantized, inputs['input_ids'], strict=False)
optimized_traced_model = optimize_for_mobile(traced_model)
torch.jit.save(optimized_traced_model, "qa360_quantized.pt")
```

to

```
traced_model = torch.jit.trace(model, inputs['input_ids'], strict=False)
optimized_traced_model = optimize_for_mobile(traced_model)
torch.jit.save(optimized_traced_model, "qa360.pt")
```

and rerun `python convert_distilbert_qa.py` to generate a non-quantized model `qa360.pt` and use it in the app to compare with the quantized version `qa360_quantized.pt`.


### 2. Build and run with Android Studio

Start Android Studio, open the project located in `android-demo-app/QuestionAnswering`, and run on your AVD or real Android device. See this [video](https://drive.google.com/file/d/10hwGNFo5tylalKwut_CWFPJmV7JRdDKF/view?usp=sharing) for a screencast of the app running. Some example translation results are:

![](screenshot1.png)
![](screenshot2.png)
![](screenshot3.png)
