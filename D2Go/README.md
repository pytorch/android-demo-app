# D2Go on Android Using Native torchvision Ops Library

## Introduction

[Detectron2](https://github.com/facebookresearch/detectron2) is one of the most widely adopted open source projects and implements state-of-the-art object detection, semantic segmentation, panoptic segmentation, and human pose prediction. [D2Go](https://github.com/facebookresearch/d2go) is powered by PyTorch 1.8, torchvision 0.9, and Detectron2 with built-in SOTA networks for mobile.

This D2Go Android demo app shows how to prepare and use the much lighter and faster D2Go model on Android. The code is based on a previous PyTorch Android [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) that uses a pre-trained Yolov5 model, with modified pre-processing and post-processing code required by the D2Go model.

## Prerequisites

* PyTorch 1.8.0 and torchvision 0.9.0
* Python 3.8 or above
* Android Pytorch library 1.8.0, torchvision library 1.8.0, torchvision_ops library 0.9.0
* Android Studio 4.0.1 or later

## Quick Start

1. Install PyTorch 1.8.0 and torchvision 0.9.0, for example:

```
conda create -n d2go python=3.8.5
conda activate d2go
pip install torch torchvision
```

2. Install Detectron2, mobile_cv, and D2Go:

```
python -m pip install 'git+https://github.com/facebookresearch/detectron2.git'
python -m pip install 'git+https://github.com/facebookresearch/mobile-vision.git'
git clone https://github.com/facebookresearch/d2go
cd d2go & python -m pip install .

```

3. Get the D2Go Android demo app and create the D2Go model:

```
git clone https://github.com/jeffxtang/android-demo-app

# uncomment only for building the torchvision-ops library locally (see Use Prebuilt torchvision ops Library for details)
# git clone https://github.com/pytorch/vision

cd android-demo-app

# uncomment only for building the torchvision-ops library locally (see Use Prebuilt torchvision ops Library for details)
# ln -s ../vision/torchvision torchvision

cd D2Go

python create_d2go.py
```

This will create the D2Go model saved at `ObjectDetection/app/src/main/assets/d2go.pt`, which is also saved in the repo. The model size is only 7.5MB, a 75% reduction in size of the 30.5MB YOLOv5s model. For the model inference speed comparison, see the note at the end of the next step.

4. Build and run the D2Go Android app.

In Android Studio, open android-demo-app/D2Go. If an error "Gradle’s dependency may be corrupt" occurs, go to Android Studio - File - Project Structure... , change Gradle Version to 4.10.1.

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

On a Pixel 3 phone, it takes about 350ms to infer an image, more than 1/3 reduction in time from the 550ms taken by the YOLOv5 model in the [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection).

## Use the Prebuilt or Self-built torchvision ops Library

The [Making Native Android Application that Uses PyTorch Prebuilt Libraries](https://pytorch.org/tutorials/recipes/android_native_app_with_custom_op.html) tutorial shows how to use the native OpenCV-based C++ code in an Android app. If you have tried the PyTorch Android YOLOv5-based [Object Detection demo app](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection), you may have noticed the [Java implementation](https://github.com/pytorch/android-demo-app/blob/master/ObjectDetection/app/src/main/java/org/pytorch/demo/objectdetection/PrePostProcessor.java#L45) of nonMaxSuppression and intersection-over-union to post-process the model inference result. The guide shows how to convert the YOLOv5 demo app to a D2Go app that uses the prebuilt torchvision ops library, which will be used by most Android developers. For those who may need to build the torchvision ops library themselves from the latest torchvision source, the Android project is also set up for that, with comments shown below demonstrating how.

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
