package org.pytorch.demo.speechrecognition;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements Runnable {
    private static final String TAG = MainActivity.class.getName();

    private Module mModuleEncoder;
    private TextView mTextView;
    private Button mButton;

    private final static String[] tokens = {"<s>", "<pad>", "</s>", "<unk>", "|", "E", "T", "A", "O", "N", "I", "H", "S", "R", "D", "L", "U", "M", "W", "C", "F", "G", "Y", "P", "B", "V", "K", "'", "X", "J", "Q", "Z"};
    private final static int INPUT_SIZE = 65024;
    private final static int AUDIO_LEN_LIMIT = 6;

    private final static int REQUEST_RECORD_AUDIO = 13;
    private final static int SAMPLE_RATE = 16000;
    private final static int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_LIMIT;

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private int mStart = 1;
    HandlerThread mTimerThread;
    Handler mTimerHandler;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerHandler.postDelayed(mRunnable, 1000);

            MainActivity.this.runOnUiThread(
                    () -> {
                        mButton.setText(String.format("Listening - %ds left", AUDIO_LEN_LIMIT - mStart));
                        mStart += 1;
                    });
        }
    };

    @Override
    protected void onDestroy() {
        stopTimerThread();
        super.onDestroy();
    }

    protected void stopTimerThread() {
        mTimerThread.quitSafely();
        try {
            mTimerThread.join();
            mTimerThread = null;
            mTimerHandler = null;
            mStart = 1;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error on stopping background thread", e);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.btnRecognize);
        mTextView = findViewById(R.id.tvResult);

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButton.setText(String.format("Listening - %ds left", AUDIO_LEN_LIMIT));
                mButton.setEnabled(false);

                Thread thread = new Thread(MainActivity.this);
                thread.start();

                mTimerThread = new HandlerThread("Timer");
                mTimerThread.start();
                mTimerHandler = new Handler(mTimerThread.getLooper());
                mTimerHandler.postDelayed(mRunnable, 1000);

            }
        });
        requestMicrophonePermission();
    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    private String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, assetName + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    private void showTranslationResult(String result) {
        mTextView.setText(result);
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        long shortsRead = 0;
        int recordingOffset = 0;
        short[] audioBuffer = new short[bufferSize / 2];
        short[] recordingBuffer = new short[RECORDING_LENGTH];

        while (shortsRead < RECORDING_LENGTH) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;
            System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);
            recordingOffset += numberOfShort;
        }

        record.stop();
        record.release();
        stopTimerThread();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButton.setText("Recognizing...");
            }
        });

        float[] floatInputBuffer = new float[RECORDING_LENGTH];

        // feed in float values between -1.0f and 1.0f by dividing the signed 16-bit inputs.
        for (int i = 0; i < RECORDING_LENGTH; ++i) {
            floatInputBuffer[i] = recordingBuffer[i] / (float)Short.MAX_VALUE;
        }

        final String result = recognize(floatInputBuffer);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTranslationResult(result);
                mButton.setEnabled(true);
                mButton.setText("Start");
            }
        });
    }

    private String recognize(float[] floatInputBuffer) {
        if (mModuleEncoder == null) {
            final String moduleFileAbsoluteFilePath = new File(
                    assetFilePath(this, "wav2vec_traced_quantized.pt")).getAbsolutePath();
            mModuleEncoder = Module.load(moduleFileAbsoluteFilePath);
        }

        double wav2vecinput[] = new double[INPUT_SIZE];
        for (int n = 0; n < INPUT_SIZE; n++)
            wav2vecinput[n] = floatInputBuffer[n];

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(INPUT_SIZE);
        for (double val : wav2vecinput)
            inTensorBuffer.put((float)val);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, INPUT_SIZE});

        final Map<String, IValue> map = mModuleEncoder.forward(IValue.from(inTensor)).toDictStringKey();
        final Tensor logitsTensor = map.get("logits").toTensor();
        final float[] values = logitsTensor.getDataAsFloatArray();

        String result = "";
        float row[] = new float[tokens.length];
        for (int i = 0; i < values.length; i++) {
            row[i % tokens.length] = values[i];
            if (i > 0 && i % tokens.length == 0) {
                int tid = argmax(row);
                if (tid > 4) result = String.format("%s%s", result, tokens[tid]);
                else if (tid == 4) result = String.format("%s ", result);
            }
        }
        return result;
    }

    private int argmax(float[] array) {
        int maxIdx = 0;
        double maxVal = -Double.MAX_VALUE;
        for (int j=0; j<array.length; j++) {
            if (array[j] > maxVal) {
                maxVal = array[j];
                maxIdx = j;
            }
        }
        return maxIdx;
    }
}