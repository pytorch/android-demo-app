# Streaming Speech Recognition on Android with Emformer-RNNT-based Model

## Introduction

In the Speech Recognition Android demo app, we showed how to use the [wav2vec 2.0](https://github.com/pytorch/fairseq/tree/master/examples/wav2vec) model on an Android demo app to perform non-continuous speech recognition. Here we're going one step further, using a torchaudio [Emformer-RNNT-based ASR](https://pytorch.org/audio/main/prototype.pipelines.html#torchaudio.prototype.pipelines.EMFORMER_RNNT_BASE_LIBRISPEECH) model in Android to perform streaming speech recognition.

## Prerequisites

* PyTorch 1.10.0 and torchaudio 0.10.0 (Optional)
* Python 3.8 (Optional)
* Android Pytorch library org.pytorch:pytorch_android_lite:1.10.0
* Android Studio 4.0.1 or later

## Quick Start

### 1. Get the Repo

Simply run the commands below:

```
git clone https://github.com/pytorch/android-demo-app
cd android-demo-app/StreamingASR
```

If you don't have PyTorch 1.10.0 and torchaudio 0.10.0 installed or want to have a quick try of the demo app, you can download the quantized scripted wav2vec2 model file [here](https://drive.google.com/file/d/1xMh-BZMSIeoohBfZvQFYcemmh5zUn_gh/view?usp=sharing), then drag and drop it to the `app/src/main/assets` folder inside  `android-demo-app/SpeechRecognition`, and continue to Step 3.

### 2. Prepare the Model

To install PyTorch 1.10.0, torchaudio 0.10.0 and the Hugging Face transformers, you can do something like this:

```
conda create -n wav2vec2 python=3.8.5
conda activate wav2vec2
pip install torch torchaudio transformers
```

Now with PyTorch 1.10.0 and torchaudio 0.10.0 installed, run the following commands on a Terminal:

```
python create_wav2vec2.py
```
This will create the PyTorch mobile interpreter model file `wav2vec2.ptl`. Copy it to the Android app:
```

mkdir -p app/src/main/assets
cp wav2vec2.ptl app/src/main/assets
```

### 2. Build and run with Android Studio

Start Android Studio, open the project located in `android-demo-app/SpeechRecognition`, build and run the app on an Android device. After the app runs, tap the Start button and start saying something; after 6 seconds (you can change `private final static int AUDIO_LEN_IN_SECOND = 6;` in `MainActivity.java` for a shorter or longer recording length), the model will infer to recognize your speech. Some example recognition results are:

![](screenshot1.png)
![](screenshot2.png)
![](screenshot3.png)
