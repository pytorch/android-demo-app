# Semantic Image Segmentation DeepLabV3 with Mobile Interpreter on Android

## Introduction

This repo offers a Python script that converts the [PyTorch DeepLabV3 model](https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101) to the Lite Interpreter version of model, also optimized for mobile, and an Android app that uses the model to segment images.

## Prerequisites

* PyTorch 1.10.0 and torchvision 0.11.1 (Optional)
* Python 3.8 or above (Optional)
* Android Pytorch library pytorch_android_lite:1.10.0, pytorch_android_torchvision_lite:1.10.0
* Android Studio 4.0.1 or later

## Quick Start

To Test Run the Image Segmentation Android App, follow the steps below:

### 1. Prepare the Model

If you don't have the PyTorch 1.10.0 environment set up, you can download the optimized-for-mobile Mobile Interpreter version of model file to the `android-demo-app/ImageSegmentation/app/src/main/assets` folder using the link [here](https://pytorch-mobile-demo-apps.s3.us-east-2.amazonaws.com/deeplabv3_scripted.pt).

Otherwise, open a terminal window, first install PyTorch 1.10.0 and torchvision 0.11.1 using command like `pip install torch torchvision`, then run the following commands:

```
git clone https://github.com/pytorch/android-demo-app
cd android-demo-app/ImageSegmentation
python deeplabv3.py
```

The Python script `deeplabv3.py` is used to generate the TorchScript-formatted models for mobile apps. For comparison, three versions of the model are generated: a full JIT version of the model, a Mobile Interpreter version of the model which is not optimized for mobile, and a Mobile Interpreter version of the model which is optimized for mobile, named as `deeplabv3_scripted_optimized.ptl`. The last one is what should be used in mobile apps, as its inference speed is over 60% faster than the non-optimized Mobile Interpreter model, which is about 6% faster than the non-optimized full JIT model.

### 2. Use Android Studio

Open the ImageSegmentation project using Android Studio. Note the app's `build.gradle` file has the following lines:

```
implementation 'org.pytorch:pytorch_android_lite:1.10.0'
implementation 'org.pytorch:pytorch_android_torchvision_lite:1.10.0'
```

and in the MainActivity.java, the code below is used to load the model:

```
mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "deeplabv3_scripted_optimized.ptl"));
```

### 3. Run the app
Select an Android emulator or device and build and run the app. The example image and its segmented result are as follows:

![](screenshot1.png)
![](screenshot2.png)

Note that the example image used in the repo is pretty large (400x400) so the segmentation process may take about 10 seconds. You may use an image of smaller size but the segmentation result may be less accurate.

## Tutorial

Read the tutorial [here](https://pytorch.org/tutorials/beginner/deeplabv3_on_android.html) for detailed step-by-step instructions of how to prepare and run the [PyTorch DeepLabV3 model](https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101) on Android, as well as practical tips on how to successfully use a pre-trained PyTorch model on Android and avoid common pitfalls.

For more information on using Mobile Interpreter in Android, see the tutorial [here](https://pytorch.org/tutorials/recipes/mobile_interpreter.html).
