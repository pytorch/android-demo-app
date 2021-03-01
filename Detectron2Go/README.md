# D2 Object Detection with Native Custom Op Library on Android

## Introduction

This demo app shows how to use the [D2Go](https://github.com/facebookresearch/d2go) model, powered by [Detectron2](https://github.com/facebookresearch/detectron2) and with built-in SOTA networks for mobile, and the native torchvision library with custom ops support to perform object detection quickly on Android. The code is based on a previous PyTorch Android [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) that uses a pre-trained Yolov5 model, with modified pre-processing and post-processing code required by the D2Go model.

## Prerequisites

* PyTorch 1.8 or later
* Python 3.8
* Android Pytorch library 1.8.0
* Android torchvision library 1.8.0
* torchvision_ops library 0.9.0
* Android Studio 4.0.1 or later

## Quick Start

Install the latest PyTorch and torchvision, for example:
```
conda create -n d2go python=3.8.5
conda activate d2go
pip install torch torchvision
```

Then install Detectron2, mobile_cv, and D2Go:
```
python -m pip install 'git+https://github.com/facebookresearch/detectron2.git'
python -m pip install 'git+https://github.com/facebookresearch/mobile-vision.git'
git clone https://github.com/facebookresearch/d2go
cd d2go & python -m pip install .

```

Get the D2Go Android demo app and create the D2Go model:
```
git clone https://github.com/jeffxtang/android-demo-app

# uncomment only for building the torchvision-ops library locally (see Use Prebuilt torchvision ops Library for details)
# git clone https://github.com/pytorch/vision

cd android-demo-app

# uncomment only for building the torchvision-ops library locally (see Use Prebuilt torchvision ops Library for details)
#ln -s ../vision/torchvision torchvision

cd D2Go

python create_d2go.py
```

In Android Studio, open android-demo-app/D2Go. If an error "Gradle’s dependency may be corrupt" occurs, go to Android Studio - File - Project Structure... , change Gradle Version to 4.10.1.

Build and run on emulator or device.

Some example images and the detection results are at the end.

## Use Prebuilt torchvision ops Library

The [Making Native Android Application that Uses PyTorch Prebuilt Libraries](https://pytorch.org/tutorials/recipes/android_native_app_with_custom_op.html) tutorial shows how to use the native OpenCV-based C++ code in an Android app. If you have tried the PyTorch Android YOLOv5-based [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection), you may have noticed the [Java implementation](https://github.com/pytorch/android-demo-app/blob/master/ObjectDetection/app/src/main/java/org/pytorch/demo/objectdetection/PrePostProcessor.java#L45) of nonMaxSuppression and intersection-over-union to post-process the model inference result.

Facebook AI's [Detectron2](https://github.com/facebookresearch/detectron2) is one of the most widely adopted open source projects and implements state-of-the-art object detection, semantic segmentation, panoptic segmentation, and human pose prediction. The latest 0.9.0 prebuilt [torchvision ops](https://pytorch.org/docs/stable/torchvision/ops.html) [library](https://oss.sonatype.org/#nexus-search;quick~torchvision_ops) provides the support for Detectron2 needed to run on mobile. The guide shows how to convert the YOLOv5 demo app to a D2Go app that uses the prebuilt torchvision ops library.

1. Create a folder called `D2Go`, clone the [ObjectDetection repo](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) inside `D2Go`.

Note that if you decide to build the torchvision-ops library locally instead of using the pre-built library as enabled by adding `implementation 'org.pytorch:torchvision_ops:...` shown in step 4 below, then copy the `ops` and `gradle_scripts` folders to the same level as `ObjectDetection`. The `ops/CMakeLists.txt` file refers to the torchvision ops implementation at `../../torchvision/csrc/ops/`, so to build the torchvision ops library, you need to clone the torchvision repo and create a link to the source as shown in Quick Start.

2. Make the content of `settings.gradle` at the top level as follows:
```
// Uncomment if building the torchvision-ops library locally
// include ':ops', ':ObjectDetection'
// project(':ops').projectDir = file('ops')

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

    // Uncomment if building the torchvision-ops library locally
    // implementation project(':ops')

    implementation 'org.pytorch:pytorch_android:1.8.0-SNAPSHOT'
    implementation 'org.pytorch:pytorch_android_torchvision:1.8.0-SNAPSHOT'

    // Comment if building the torchvision-ops library locally
    implementation 'org.pytorch:torchvision_ops:0.9.0-SNAPSHOT'
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

Now you're ready to build the project that uses the latest D2Go model created when running `create_d2go.py`.

## Example Images and Detections

![](screenshot1.png)
![](screenshot2.png)

![](screenshot3.png)
![](screenshot4.png)