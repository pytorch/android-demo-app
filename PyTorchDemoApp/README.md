## PyTorch Demo Application

This demo application does image classification from camera output and text classification in the [same github repo](https://github.com/pytorch/android-demo-app/tree/master/PyTorchDemoApp).

To get device camera output it uses [Android CameraX API](https://developer.android.com/training/camerax
).
All the logic that works with CameraX is separated to [`org.pytorch.demo.vision.AbstractCameraXActivity`](https://github.com/pytorch/android-demo-app/blob/master/PyTorchDemoApp/app/src/main/java/org/pytorch/demo/vision/AbstractCameraXActivity.java) class.


```
void setupCameraX() {
    final PreviewConfig previewConfig = new PreviewConfig.Builder().build();
    final Preview preview = new Preview(previewConfig);
    preview.setOnPreviewOutputUpdateListener(output -> mTextureView.setSurfaceTexture(output.getSurfaceTexture()));

    final ImageAnalysisConfig imageAnalysisConfig =
        new ImageAnalysisConfig.Builder()
            .setTargetResolution(new Size(224, 224))
            .setCallbackHandler(mBackgroundHandler)
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build();
    final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
    imageAnalysis.setAnalyzer(
        (image, rotationDegrees) -> {
          analyzeImage(image, rotationDegrees);
        });

    CameraX.bindToLifecycle(this, preview, imageAnalysis);
  }

  void analyzeImage(android.media.Image, int rotationDegrees)
```

Where the `analyzeImage` method process the camera output, `android.media.Image`.

It uses the aforementioned [`TensorImageUtils.imageYUV420CenterCropToFloat32Tensor`](https://github.com/pytorch/pytorch/blob/master/android/pytorch_android_torchvision/src/main/java/org/pytorch/torchvision/TensorImageUtils.java#L90) method to convert `android.media.Image` in `YUV420` format to input tensor.

After getting predicted scores from the model it finds top K classes with the highest scores and shows on the UI.

#### Language Processing Example

Another example is natural language processing, based on an LSTM model, trained on a reddit comments dataset.
The logic happens in [`TextClassificattionActivity`](https://github.com/pytorch/android-demo-app/blob/master/PyTorchDemoApp/app/src/main/java/org/pytorch/demo/nlp/TextClassificationActivity.java).

Result class names are packaged inside the TorchScript model and initialized just after initial module initialization.
The module has a `get_classes` method that returns `List[str]`, which can be called using method `Module.runMethod(methodName)`:
```
    mModule = Module.load(moduleFileAbsoluteFilePath);
    IValue getClassesOutput = mModule.runMethod("get_classes");
```
The returned `IValue` can be converted to java array of `IValue` using `IValue.toList()` and processed to an array of strings using `IValue.toStr()`:
```
    IValue[] classesListIValue = getClassesOutput.toList();
    String[] moduleClasses = new String[classesListIValue.length];
    int i = 0;
    for (IValue iv : classesListIValue) {
      moduleClasses[i++] = iv.toStr();
    }
```

Entered text is converted to java array of bytes with `UTF-8` encoding. `Tensor.fromBlobUnsigned` creates tensor of `dtype=uint8` from that array of bytes.
```
    byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
    final long[] shape = new long[]{1, bytes.length};
    final Tensor inputTensor = Tensor.fromBlobUnsigned(bytes, shape);
```

Running inference of the model is similar to previous examples:
```
Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor()
```

After that, the code processes the output, finding classes with the highest scores.
