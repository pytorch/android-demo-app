# Neural Machine Translation on Android

## Introduction

The PyTorch tutorial [NLP FROM SCRATCH: TRANSLATION WITH A SEQUENCE TO SEQUENCE NETWORK AND ATTENTION](https://pytorch.org/tutorials/intermediate/seq2seq_translation_tutorial.html) is an easy-to-follow tutorial that has step-by-step instructions on how to train sequence to sequence encoder and decode networks with an attention mechanism for neural machine translation. Although the quality of the translation between English and French using the model in the tutorial may not be as good as a leading-edge transformer based model, it presents a great example of how to deploy an interesting model trained from scratch in an Android app.

This Android demo app shows:

* The Python code that saves the encoder and attention-enhanced decoder models, trained based on the code in the PyTorch NMT tutorial;
* The Python code that loads the saved encode and decoder models, optimizes them and saves again for mobile apps to use;
* The Android app that uses the encoder and decoder models to do French-English translation.

## Prerequisites

* PyTorch 1.7 or later (Optional)
* Python 3.8 (Optional)
* Android Pytorch library 1.6, 1.7 or later
* Android Studio 4.0.1 or later

## Quick Start

To Test Run the Object Detection Android App, follow the steps below:

### 1. Prepare the Model

If you have a good GPU and want to train your model from scratch, uncomment the line `trainIters(encoder, decoder, 450100, print_every=5000)` in `seq2seq_nmt.py` before running `python seq2seq2_nmt.py` to go through the whole process of training, saving, loading and optimizing and saving the final mobile-ready models. After `optimized_encoder_150k.pth` and `optimized_encoder_150k.pth` are generated, copy them to the Android app's assets folder.

You can also download the PyTorch trained and optimized NMT encoder and decoder models compressed in a zip [here](https://drive.google.com/file/d/1TxB5oStgShrNvlSVlVaGylNUi4PTtufQ/view?usp=sharing), unzip it and then copy to the Android app's assets folder.

### 2. Build and run with Android Studio

Start Android Studio, open the project located in `android-demo-app/Seq2SeqNMT`, and run on your AVD or real Android device. See this [video](https://drive.google.com/file/d/110KN3Pa9DprkBWnzj8Ppa8KMymhmBI61/view?usp=sharing) for a screencast of the app running. Some example translation results are:

![](screenshot1.png)
![](screenshot2.png)
![](screenshot3.png)
