# PyTorch Android Examples

A list of Android demo apps built on the powerful [PyTorch Mobile](https://pytorch.org/mobile) platform.

### HelloWorld

[HelloWorld](https://github.com/pytorch/android-demo-app/tree/master/HelloWorldApp) is a simple image classification application that demonstrates how to use the PyTorch Android API with the latest PyTorch 1.8, MobileNet v3, and [MemoryFormat.CHANNELS_LAST](https://pytorch.org/tutorials/intermediate/memory_format_tutorial.html).

### PyTorch demo app

The [PyTorch demo app](https://github.com/pytorch/android-demo-app/tree/master/PyTorchDemoApp) is a full-fledged app that contains two showcases. A camera app that runs a quantized model to classifiy images in real time. And a text-based app that uses a text classification model to predict the topic from the input text.

### D2go

[D2Go](https://github.com/pytorch/android-demo-app/tree/master/D2Go) demonstrates a Python script that creates the much lighter and much faster Facebook [D2Go](https://github.com/facebookresearch/d2go) model that is powered by PyTorch 1.8, torchvision 0.9, and Detectron2 with built-in SOTA networks for mobile, and an Android app that uses it to detect objects from pictures in your photos, taken with camera, or with live camera. This demo app also shows how to use the native pre-built torchvision-ops library.

### Image Segmentation

[Image Segmentation](https://github.com/pytorch/android-demo-app/tree/master/ImageSegmentation) demonstrates a Python script that converts the PyTorch [DeepLabV3](https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101/) model and an Android app that uses the model to segment images.

### Object Detection

[Object Detection](https://github.com/pytorch/android-demo-app/tree/master/ObjectDetection) demonstrates how to convert the popular [YOLOv5](https://pytorch.org/hub/ultralytics_yolov5/) model and use it in an Android app that detects objects from pictures in your photos, taken with camera, or with live camera.

### Neural Machine Translation

[Neural Machine Translation](https://github.com/pytorch/android-demo-app/tree/master/Seq2SeqNMT) demonstrates how to convert a sequence-to-sequence neural machine translation model trained with the code in the [PyTorch NMT tutorial](https://pytorch.org/tutorials/intermediate/seq2seq_translation_tutorial.html) and use the model in an Android app to do French-English translation.

### Question Answering

[Question Answering](https://github.com/pytorch/android-demo-app/tree/master/QuestionAnswering) demonstrates how to convert a powerful transformer QA model and use the model in an Android app to answer questions about PyTorch Mobile and more.

### Speech recognition

[Speech Recognition](https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition) demonstrates how to convert Facebook AI's wav2vec 2.0, one of the leading models in speech recognition, to TorchScript and how to use the scripted model in an Android app to perform speech recognition.

### Vision Transformer

[Vision Transformer](https://github.com/pytorch/android-demo-app/tree/master/ViT4MNIST) demonstrates how to use Facebook's latest Vision Transformer [DeiT](https://github.com/facebookresearch/deit) model to do image classification, and how convert another Vision Transformer model and use it in an Android app to perform handwritten digit recognition.

### Speech recognition

[Speech Recognition](https://github.com/pytorch/android-demo-app/tree/master/SpeechRecognition) demonstrates how to convert Facebook AI's wav2vec 2.0, one of the leading models in speech recognition, to TorchScript and how to use the scripted model in an Android app to perform speech recognition.

### Video Classification

[TorchVideo](https://github.com/pytorch/android-demo-app/tree/master/TorchVideo) demonstrates how to use a pre-trained video classification model, available at the newly released [PyTorchVideo](https://github.com/facebookresearch/pytorchvideo), on Android to see video classification results, updated per second while the video plays, on tested videos, videos from the Photos library, or even real-time videos.


## LICENSE

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
