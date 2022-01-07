# Streaming Speech Recognition on Android with Emformer-RNNT-based Model

## Introduction

In the Speech Recognition Android demo app, we showed how to use the [wav2vec 2.0](https://github.com/pytorch/fairseq/tree/master/examples/wav2vec) model on an Android demo app to perform non-continuous speech recognition. Here we're going one step further, using a torchaudio [Emformer-RNNT-based ASR](https://pytorch.org/audio/main/prototype.pipelines.html#torchaudio.prototype.pipelines.EMFORMER_RNNT_BASE_LIBRISPEECH) model in Android to perform streaming speech recognition.

## Prerequisites

* PyTorch 1.11 nightly and torchaudio 0.11 nightly (Optional)
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

If you don't have PyTorch 1.11 nightly and torchaudio 0.11 nightly installed or want to have a quick try of the demo app, you can download the optimized scripted model file [streaming_asr.ptl](), then drag and drop it to the `app/src/main/assets` folder inside  `android-demo-app/StreamingASR`, and continue to Step 3.

### 2. Prepare the Model

To install PyTorch 1.11 and torchaudio 0.11 nightly, you can do something like this:

```
conda create -n torch_nightly python=3.8.5
conda activate torch_nightly
pip install -U --pre torch torchaudio -f https://download.pytorch.org/whl/nightly/cpu/torch_nightly.html
```

After that, run the following commands to test the streaming ASR works in your computer:

```
conda install pyaudio
python run_sasr.py
```

After you see:
```
Initializing model...
Initialization complete.
```
you can say something like "good afternoon happy new year", and you'll likely see the streaming recognition results `▁good ▁afternoon ▁happy ▁new ▁year` while you speak. Hit Ctrl-C to end.

To optimize and convert the model to the format that can run on Android, run the following commands:
```
mkdir -p StreamingASR/app/src/main/assets
python save_model_for_mobile.py
mv streaming_asr.ptl StreamingASR/app/src/main/assets
```

### 2. Build and run with Android Studio

Start Android Studio, open the project located in `android-demo-app/SpeechRecognition`, build and run the app on an Android device. After the app runs, tap the Start button and start saying something; after 6 seconds (you can change `private final static int AUDIO_LEN_IN_SECOND = 6;` in `MainActivity.java` for a shorter or longer recording length), the model will infer to recognize your speech. Some example recognition results are:

![](screenshot1.png)
![](screenshot2.png)
![](screenshot3.png)
