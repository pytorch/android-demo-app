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

With PyTorch 1.7 installed, run:
```
pip install transformers
python convert_distilbert_qa.py
```

Then copy the model file qa360_quantized.pt to the Android app's assets folder. [Dynamic quantization](https://pytorch.org/tutorials/intermediate/dynamic_quantization_bert_tutorial.html) is used to quantize the model to reduce its size to half without causing inference difference in question answering.

### 2. Build and run with Android Studio

Start Android Studio, open the project located in `android-demo-app/QuestionAnswering`, and run on your AVD or real Android device. See this [video](https://drive.google.com/file/d/10hwGNFo5tylalKwut_CWFPJmV7JRdDKF/view?usp=sharing) for a screencast of the app running. Some example translation results are:

![](screenshot1.png)
![](screenshot2.png)
![](screenshot3.png)
