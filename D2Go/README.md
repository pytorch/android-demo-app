# D2Go on Android Using Native torchvision Ops Library

## Introduction

[Detectron2](https://github.com/facebookresearch/detectron2) is one of the most widely adopted open source projects and implements state-of-the-art object detection, semantic segmentation, panoptic segmentation, and human pose prediction. [D2Go](https://github.com/facebookresearch/d2go) is powered by PyTorch 1.8, torchvision 0.9, and Detectron2 with built-in SOTA networks for mobile - the D2Go model is very small (only 2.7MB) and runs very fast on Android (~50ms per inference on Pixel 3, also due to the use of the native torchvision-ops library).

This D2Go Android demo app shows how to prepare and use the D2Go model on Android. The code is based on a previous PyTorch Android [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) that uses a pre-trained YOLOv5 model, with modified pre-processing and post-processing code required by the D2Go model.

## Prerequisites

* PyTorch 1.8.0 and torchvision 0.9.0 (Optional for Quick Start)
* Python 3.8 or above (Optional for Quick Start)
* Android Pytorch library 1.8.0, torchvision library 1.8.0, torchvision_ops library 0.9.0
* Android Studio 4.0.1 or later

## Quick Start

This section shows how to create and use the D2Go model and the pre-built torchvision-ops library in a completed Android app. If you are interested in more details of how to build the torchvision-ops library locally and use it with the D2Go model on Android, you can continue with the next section after this.

To just build and run the app without creating the D2Go model yourself, go directly to Step 4.

1. Install PyTorch 1.8.0 and torchvision 0.9.0, for example:

```
conda create -n d2go python=3.8.5
conda activate d2go
pip install torch torchvision
```

2. Install Detectron2, mobile_cv, and D2Go

```
python -m pip install 'git+https://github.com/facebookresearch/detectron2.git'
python -m pip install 'git+https://github.com/facebookresearch/mobile-vision.git'
git clone https://github.com/facebookresearch/d2go
cd d2go & python -m pip install .

```

3. Create the D2Go model

```
git clone https://github.com/pytorch/android-demo-app
cd android-demo-app/D2Go
python create_d2go.py
```
This will create the quantized D2Go model and save it at `android-demo-app/D2Go/ObjectDetection/app/src/main/assets/d2go.pt`.  

The size of the quantized D2Go model is only 2.6MB.

4. Build and run the D2Go Android app

If you have not gone through Step 3, simply run `git clone https://github.com/pytorch/android-demo-app` first.

In Android Studio, open android-demo-app/D2Go. If an error "Gradle’s dependency may be corrupt" occurs, go to Android Studio - File - Project Structure... , change Gradle Version to 4.10.1.

The main changes needed to use the D2Go model and the required and pre-built torchvision-ops library are adding
```
implementation 'org.pytorch:pytorch_android:1.8.0'
implementation 'org.pytorch:pytorch_android_torchvision:1.8.0'
implementation 'org.pytorch:torchvision_ops:0.9.0'
```
in the build.gradle file and
```
static {
    if (!NativeLoader.isInitialized()) {
        NativeLoader.init(new SystemDelegate());
    }
    NativeLoader.loadLibrary("pytorch_jni");
    NativeLoader.loadLibrary("torchvision_ops");
}
```
in the MainActivity.java.

Select an Android emulator or device to run the app. You can go through the included example test images to see the detection results. You can also select a picture from your Android device's Photos library, take a picture with the device camera, or even use live camera to do object detection - see this [video](https://drive.google.com/file/d/17TNzYiIkQGBLZrapqmxADvlUIdgM313k/view?usp=sharing) for a screencast of the app running.

Some example images and the detection results are as follows:

![](screenshot1.png)
![](screenshot2.png)

![](screenshot3.png)
![](screenshot4.png)

One quick note about the model performance. In the `MainActivity.java`, the following code snippet shows how fast the D2Go model runs:

```
final long startTime = SystemClock.elapsedRealtime();
IValue[] outputTuple = mModule.forward(IValue.listFrom(inputTensor)).toTuple();
final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
System.out.println("D2Go inference time(ms): " + inferenceTime);
```

On a Pixel 3 phone, it takes about 50ms to infer an image, compared with the 550ms taken by the YOLOv5 model in the [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection).

## [Advanced] Build torchvision ops Library Locally to Use D2Go on Android

Using the pre-built torchvision ops library will be typical for most Android developers. But occasionally one may need to build the torchvision ops library locally from the latest torchvision source, possibly due to a bug fix that has not been merged to the master branch or an enhancement to the source code. This section shows how to build the library locally, by converting the YOLOv5 Object Detection demo app in a step-by-step fashion.

First, make sure you have installed the required packages by following Steps 1 and 2 in the Quick Start section. Then follow the steps below.

1. Set up Android demo app and torchvision repos:

```
git clone https://github.com/pytorch/android-demo-app
git clone https://github.com/pytorch/vision
cd android-demo-app
ln -s ../vision/torchvision torchvision
```

Copy the `ops` and `gradle_scripts` folders from `android-demo-app\D2Go` to `android-demo-app\ObjectDetection`. The `ops/CMakeLists.txt` file refers to the torchvision ops implementation at `../../torchvision/csrc/ops/`, so to build the torchvision ops library locally, you need to clone the torchvision repo and create a link to the source.

2. Make the content of `settings.gradle` at the top level as follows:
```
include ':ops', ':ObjectDetection'
project(':ops').projectDir = file('ops')

project(':ObjectDetection').projectDir = file('ObjectDetection/app')
```

3. Move the original `ObjectDetection/build.gradle` outside `ObjectDetection`, and make it like:
```
allprojects {
    buildscript {
        ext {
            minSdkVersion = 21
            targetSdkVersion = 28
            compileSdkVersion = 28
            buildToolsVersion = '28.0.3'

            coreVersion = "1.2.0"
            extJUnitVersion = "1.1.1"
            runnerVersion = "1.2.0"
            rulesVersion = "1.2.0"
            junitVersion = "4.12"

            androidSupportAppCompatV7Version = "28.0.0"
            fbjniJavaOnlyVersion = "0.0.3"
            soLoaderNativeLoaderVersion = "0.8.0"
        }

        repositories {
            google()
            mavenCentral()
            jcenter()
        }

        dependencies {
            classpath 'com.android.tools.build:gradle:3.3.2'
        }
    }

    repositories {
        google()
        jcenter()
    }
}

ext.deps = [
        jsr305: 'com.google.code.findbugs:jsr305:3.0.1',
]
```

4. Change the `build.gradle` in `ObjectDetection` to contain:
```
tasks.all { task ->
    if (task.name.startsWith('externalNativeBuild')
            && !task.name.contains('NativeBuild')) {
        task.enabled = false
    }
}

dependencies {
    implementation 'com.facebook.soloader:nativeloader:0.8.0'
    implementation project(':ops')

    implementation 'org.pytorch:pytorch_android:1.8.0'
    implementation 'org.pytorch:pytorch_android_torchvision:1.8.0'
    implementation 'org.pytorch:torchvision_ops:0.9.0'
}
```

5. Change the model pre-processing and post-processing code as it differs from what YOLOv5 expects, and add the initialization and load of the native library code in `MainActivity.java`:
```
static {
    if (!NativeLoader.isInitialized()) {
        NativeLoader.init(new SystemDelegate());
    }
    NativeLoader.loadLibrary("pytorch_jni");
    NativeLoader.loadLibrary("torchvision_ops");
}
```

6. Copy the D2Go model file from `android-demo-app/D2Go/ObjectDetection/app/src/main/assets/d2go.pt` to `android-demo-app/ObjectDetection/app/src/main/assets/d2go.pt` and build and run the `android-demo-app\ObjectDetection` project uses the locally built torchvision-ops library. Voilà! You get to see the app running the same way as in the previous section, using the pre-built torchvision-ops library, with the benefit that you know when needed, you can build the library yourself.
