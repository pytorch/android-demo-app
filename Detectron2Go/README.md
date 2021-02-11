# Detectron2Go Object Detection with Native Custom Op Library on Android

## Introduction

This demo app shows how to use the Detectron2Go model and the native torchvision library with custom ops support to perform object detection. The code is based on a previous demo app that uses a pre-trained Yolov5 model.

## Prerequisites

* PyTorch 1.8 or later
* Python 3.8
* Android Pytorch library 1.8.0
* Android Studio 4.0.1 or later

## Quick Start

```
git clone https://github.com/jeffxtang/android-demo-app
git clone https://github.com/pytorch/vision
cd android-demo-app
ln -s ../vision/torchvision torchvision
git checkout d2go
cd Detectron2Go
```

Assuming you have the nightly built PyTorch and TorchVision installed, or do:
```
conda create -n pt_nightly python=3.8.5
conda activate pt_nightly
pip install -U --pre torch torchvision -f https://download.pytorch.org/whl/nightly/cpu/torch_nightly.html
```
Then run:
```
python create_model.py
```

In Android Studio, open android-demo-app/Detectron2Go. If an error "Gradle’s dependency may be corrupt" occurs, go to Android Studio - File - Project Structure... , change Gradle Version to 4.10.1.

Build and run on emulator or device.

Some example images and the detection results are as follows:

![](screenshot1.png)
![](screenshot2.png)

![](screenshot3.png)
![](screenshot4.png)
