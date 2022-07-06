package org.pytorch.demo.streamingasr;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
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

import org.pytorch.LiteModuleLoader;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static final String TAG = MainActivity.class.getName();

    private Module mModuleEncoder;
    private TextView mTextView;
    private Button mButton;
    private boolean mListening;
    private String all_result = "";

    private final static int REQUEST_RECORD_AUDIO = 13;
    private final static int SAMPLE_RATE = 16000;
    private final static int CHUNK_TO_READ = 5;
    private final static int CHUNK_SIZE = 640;
    private final static int INPUT_SIZE = 3200;

    private IValue hypo = null;
    private IValue state = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.btnRecognize);
        mTextView = findViewById(R.id.tvResult);

        if (mModuleEncoder == null) {
            mModuleEncoder = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "streaming_asrv2.ptl"));
        }

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mButton.getText().equals("Start")) {
                    mButton.setText("Listening... Stop");
                    mListening = true;
                }
                else {
                    mButton.setText("Start");
                    mListening = false;
                }

                Thread thread = new Thread(MainActivity.this);
                thread.start();
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

        final int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        int chunkToRead = CHUNK_TO_READ;
        int recordingOffset = 0;
        short[] recordingBuffer = new short[CHUNK_TO_READ*CHUNK_SIZE];
        double[] floatInputBuffer = new double[CHUNK_TO_READ * CHUNK_SIZE];

        while (mListening) {
            long shortsRead = 0;
            short[] audioBuffer = new short[bufferSize / 2];

            while (shortsRead < chunkToRead * CHUNK_SIZE) {
                // for every segment of 5 chunks of data, we perform transcription
                // each successive segment’s first chunk is exactly the preceding segment’s last chunk
                int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                shortsRead += numberOfShort;
                int x = (int) (numberOfShort - (shortsRead - chunkToRead * CHUNK_SIZE));
                if (shortsRead > chunkToRead * CHUNK_SIZE)
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, (int) (numberOfShort - (shortsRead - chunkToRead*640)));
                else
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);

                recordingOffset += numberOfShort;
            }

            for (int i = 0; i < CHUNK_TO_READ * CHUNK_SIZE; ++i) {
                floatInputBuffer[i] = recordingBuffer[i] / (float)Short.MAX_VALUE;
            }

            final String result = recognize(floatInputBuffer);
            if (result.length() > 0)
                all_result = String.format("%s %s", all_result, result);

            chunkToRead = CHUNK_TO_READ - 1;
            recordingOffset = CHUNK_SIZE;
            System.arraycopy(recordingBuffer, chunkToRead * CHUNK_SIZE, recordingBuffer, 0, CHUNK_SIZE);

            runOnUiThread(() -> showTranslationResult(all_result));
        }

        record.stop();
        record.release();
    }

    private String recognize(double[] inputBuffer) {
        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(INPUT_SIZE);
        for (int i = 0; i < inputBuffer.length - 1; i++) {
            inTensorBuffer.put((float) inputBuffer[i]);
        }

        final Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{INPUT_SIZE});
        final long startTime = SystemClock.elapsedRealtime();
        IValue[] outputTuple;
        if (hypo == null && state == null)
            outputTuple = mModuleEncoder.forward(IValue.from(inTensor)).toTuple();
        else
            outputTuple = mModuleEncoder.forward(IValue.from(inTensor), hypo, state).toTuple();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(TAG, "inference time (ms): " + inferenceTime);
        final String transcript = outputTuple[0].toStr().replace("▁", "");;
        hypo = outputTuple[1];
        state = outputTuple[2];
        if (transcript.length() > 0)
            Log.d(TAG, "transcript=" + transcript);
        return transcript;
    }
}