# Detectron2Go Object Detection with Native Custom Op Library on Android

## Introduction

This demo app shows how to use the Detectron2Go model and the native torchvision library with custom ops support to perform object detection. The code is based on a previous PyTorch Android [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) that uses a pre-trained Yolov5 model.

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

Some example images and the detection results are at the end.

## Use Prebuilt TorchVision ops Library

The [Making Native Android Application that Uses PyTorch Prebuilt Libraries](https://pytorch.org/tutorials/recipes/android_native_app_with_custom_op.html) tutorial shows how to use the native OpenCV-based C++ code in an Android app. If you have tried the PyTorch Android YOLOv5-based [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection), you may have noticed the [Java implementation](https://github.com/pytorch/android-demo-app/blob/master/ObjectDetection/app/src/main/java/org/pytorch/demo/objectdetection/PrePostProcessor.java#L45) of nonMaxSuppression and intersection-over-union to post-process the model inference result.

Facebook AI's [Detectron2](https://github.com/facebookresearch/detectron2) is one of the most widely adopted open source projects and implements state-of-the-art object detection, semantic segmentation, panoptic segmentation, and human pose prediction. The latest prebuilt [torchvision ops](https://pytorch.org/docs/stable/torchvision/ops.html) [library](https://oss.sonatype.org/#nexus-search;quick~torchvision_ops) provides the support for Detectron2 needed to run on mobile. The guide shows how to convert the YOLOv5 demo app to a Detectron2 app that uses the prebuilt torchvision ops library.

1. Create a folder called `Detectron2Go`, clone the [ObjectDetection repo](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) inside `Detectron2Go`, and copy the `ops` and `gradle_scripts` folders to the same level as `ObjectDetection`. Notice the `ops/CMakeLists.txt` file refers to the torchvision ops implementation at `../../torchvision/csrc/ops/`, so to build the torchvision ops library, you need to clone the torchvision repo and create a link to the source as shown in Quick Start.

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
            classpath "com.jfrog.bintray.gradle:gradle-bintray-plugin:${GRADLE_BINTRAY_PLUGIN_VERSION}"
            classpath "com.github.dcendents:android-maven-gradle-plugin:${ANDROID_MAVEN_GRADLE_PLUGIN_VERSION}"
            classpath "org.jfrog.buildinfo:build-info-extractor-gradle:4.9.8"
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
repositories {
    jcenter()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
}

tasks.all { task ->
    if (task.name.startsWith('externalNativeBuild')
            && !task.name.contains('NativeBuild')) {
        task.enabled = false
    }
}

dependencies {
    implementation 'com.facebook.soloader:nativeloader:0.8.0'
    implementation project(':ops')

    implementation 'org.pytorch:pytorch_android:1.8.0-SNAPSHOT'
    implementation 'org.pytorch:pytorch_android_torchvision:1.8.0-SNAPSHOT'
    implementation 'org.pytorch:torchvision_ops:0.0.1-SNAPSHOT'
}
```

Now you're ready to build the project that uses the latest Detectron2 model created when running `create_model.py`. Of course, the model pre-processing and post-processing code needs to be updated as it differs from what YOLOv5 expects.

## Example Images and Detections

![](screenshot1.png)
![](screenshot2.png)

![](screenshot3.png)
![](screenshot4.png)
