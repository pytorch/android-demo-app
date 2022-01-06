package org.pytorch.demo.streamingasr;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.util.Vector;

import org.pytorch.LiteModuleLoader;


public class MainActivity extends AppCompatActivity implements Runnable {
    private static final String TAG = MainActivity.class.getName();

    private Module mModuleEncoder;
    private TextView mTextView;
    private Button mButton;
    private boolean mListening;

    private final static int REQUEST_RECORD_AUDIO = 13;
    private final static int AUDIO_LEN_IN_SECOND = 5;
    private final static int SAMPLE_RATE = 16000;
    private final static int RECORDING_LENGTH = SAMPLE_RATE * AUDIO_LEN_IN_SECOND;

    private IValue hypo = null;
    private IValue state = null;

    private double[] _mean =  {
            16.462461471557617,
            17.020158767700195,
            17.27733039855957,
            17.273637771606445,
            17.78028678894043,
            18.112783432006836,
            18.322141647338867,
            18.3536319732666,
            18.220436096191406,
            17.93610191345215,
            17.650646209716797,
            17.505868911743164,
            17.450956344604492,
            17.420780181884766,
            17.36254119873047,
            17.24843978881836,
            17.073762893676758,
            16.893953323364258,
            16.62371826171875,
            16.279895782470703,
            16.046218872070312,
            15.789617538452148,
            15.458984375,
            15.335075378417969,
            15.103074073791504,
            14.993032455444336,
            14.818647384643555,
            14.713132858276367,
            14.576343536376953,
            14.482580184936523,
            14.431093215942383,
            14.392385482788086,
            14.357626914978027,
            14.335031509399414,
            14.344644546508789,
            14.341029167175293,
            14.338135719299316,
            14.311485290527344,
            14.266831398010254,
            14.205205917358398,
            14.159194946289062,
            14.07589054107666,
            14.02244758605957,
            13.954248428344727,
            13.897454261779785,
            13.856722831726074,
            13.80321216583252,
            13.75955867767334,
            13.718783378601074,
            13.67695426940918,
            13.626880645751953,
            13.554975509643555,
            13.465453147888184,
            13.372663497924805,
            13.269320487976074,
            13.184920310974121,
            13.094778060913086,
            12.998514175415039,
            12.891039848327637,
            12.765382766723633,
            12.638651847839355,
            12.50733470916748,
            12.345802307128906,
            12.195826530456543,
            12.019110679626465,
            11.842704772949219,
            11.680868148803711,
            11.518675804138184,
            11.37252426147461,
            11.252099990844727,
            11.12936019897461,
            11.029287338256836,
            10.927411079406738,
            10.825841903686523,
            10.717211723327637,
            10.499553680419922,
            9.722028732299805,
            8.256664276123047,
            7.897761344909668,
            7.252806663513184
    };

    private double[] _invstddev = {
            0.2532021571066031,
            0.2597563367511928,
            0.2579079373215276,
            0.2416085222005694,
            0.23003407153886749,
            0.21714598348479108,
            0.20868966256973892,
            0.20397882792073063,
            0.20346486748979434,
            0.20568288111895272,
            0.20795624145573485,
            0.20848980415063503,
            0.20735096423640872,
            0.2060772210458722,
            0.20577174595523076,
            0.20655349986725383,
            0.2080547906859301,
            0.21015748217276387,
            0.2127639989370032,
            0.2156462785763535,
            0.21848300746868443,
            0.22174608140608748,
            0.22541974458780933,
            0.22897465119671973,
            0.23207484606149037,
            0.2353556049061462,
            0.23820711835547867,
            0.24016651485087528,
            0.24200318561465783,
            0.2435905301766702,
            0.24527147180928432,
            0.2493368450351618,
            0.25120444993308483,
            0.2521961451825939,
            0.25358032484699955,
            0.25349767201088286,
            0.2534676894845623,
            0.25149125467665234,
            0.25001929593946776,
            0.25064096375066197,
            0.25194505955280033,
            0.25270402089338095,
            0.2535205901701615,
            0.25363568106276674,
            0.2535307075541985,
            0.25315144026701186,
            0.2523683857532224,
            0.25200854739575596,
            0.2516561583169735,
            0.25147053419035553,
            0.25187638352086095,
            0.25176343344798546,
            0.25256615785525305,
            0.25310796555079107,
            0.2535568871416053,
            0.2542411936874833,
            0.2544978632482573,
            0.2553210332506536,
            0.2567248511819892,
            0.2559665595456875,
            0.2564729970835735,
            0.2585267417223537,
            0.2573770145474615,
            0.2585495460828127,
            0.2593605768768532,
            0.25906572100606984,
            0.26026752519153573,
            0.2609952847918467,
            0.26222905157170767,
            0.26395874733435604,
            0.26404203898769246,
            0.26501581381370537,
            0.2666259054856709,
            0.2676190865432322,
            0.26813030555166134,
            0.26873271506658997,
            0.2624062353014993,
            0.2289515918968408,
            0.22755587298227964,
            0.24719513536827162
    };

    private double _decibel = 2 * 20 * Math.log10(32767);
    private double _gain = Math.pow(10, 0.05 * _decibel);

    private final static String LOG_TAG = MainActivity.class.getSimpleName();

    private int mStart = 1;
    private HandlerThread mTimerThread;
    private Handler mTimerHandler;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerHandler.postDelayed(mRunnable, 1000);

            MainActivity.this.runOnUiThread(
                    () -> {
                        mButton.setText(String.format("Listening... Stop")); //, AUDIO_LEN_IN_SECOND - mStart));
                        mStart += 1;
                    });
        }
    };

    public native Vector<Vector<Float>> melSpectrogram(double[] data);

    static {
        System.loadLibrary("MainActivityJNI");
    }


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
                if (mButton.getText().equals("Start")) {
                    mButton.setText("Listening... Stop"); // - %ds left", AUDIO_LEN_IN_SECOND));
                    mListening = true;
                }
                else {
                    mButton.setText("Start"); // - %ds left", AUDIO_LEN_IN_SECOND));
                    mListening = false;
                }

                //mButton.setEnabled(false);

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


        //while (shortsRead < RECORDING_LENGTH) {
        int chunkToRead = 5;
        int recordingOffset = 0;
        short[] recordingBuffer = new short[5*640]; //RECORDING_LENGTH];
        while (mListening) {
            long shortsRead = 0;
            short[] audioBuffer = new short[bufferSize / 2];

            // TODO Jan2&3: verify the pre-processing data is correct - compared with the Python audio processing
            while (shortsRead < chunkToRead*640) {
                // for every segment of 5 chunks of data, we perform transcription
                // each successive segment’s first chunk is exactly the preceding segment’s last chunk
                int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                shortsRead += numberOfShort;
                System.out.println(String.format("numberOfShort=%d, shortsRead=%d", numberOfShort, shortsRead));
                int x = (int) (numberOfShort - (shortsRead - chunkToRead*640));
                if (shortsRead > chunkToRead*640)
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, (int) (numberOfShort - (shortsRead - chunkToRead*640)));
                else
                    System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, numberOfShort);

                recordingOffset += numberOfShort;
            }
            double[] floatInputBuffer = new double[5 * 640];

            for (int i = 0; i < 5 * 640; ++i) {
                floatInputBuffer[i] = recordingBuffer[i] / (float)Short.MAX_VALUE;
            }

            final String result = recognize(floatInputBuffer);

            chunkToRead = 4;
            recordingOffset = 640;
            System.arraycopy(recordingBuffer, 4*640, recordingBuffer, 0, 640);

            runOnUiThread(() -> showTranslationResult(result));


        }

        record.stop();
        record.release();
        stopTimerThread();

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mButton.setText("Recognizing...");
//            }
//        });

    }

    private String recognize(double[] inputBuffer) {

        double[][] spectrogram = new double[21][80];
        Vector<Vector< Float >> result = melSpectrogram(inputBuffer);
        for (int i = 0; i < result.size(); i++) {
            for (int j = 0; j < result.get(i).size(); j++) {
                spectrogram[i][j] = result.get(i).get(j);
            }
        }

        for (int i = 0; i < spectrogram.length; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                spectrogram[i][j] *= _gain;
                if (spectrogram[i][j] > Math.E)
                    spectrogram[i][j] = Math.log(spectrogram[i][j]);
                else
                    spectrogram[i][j] /= Math.E;
            }
        }

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(1600); //1*20*80);

        // get rid of the last row and transform the others
        for (int i = 0; i < spectrogram.length - 1; i++) {
            for (int j = 0; j < spectrogram[i].length; j++) {
                spectrogram[i][j] -= _mean[j];
                spectrogram[i][j] *= _invstddev[j];
                inTensorBuffer.put((float) spectrogram[i][j]);
            }
        }


        if (mModuleEncoder == null) {
            mModuleEncoder = LiteModuleLoader.load(assetFilePath(getApplicationContext(), "streaming_asr.ptl"));
        }

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 20, 80});
        final long startTime = SystemClock.elapsedRealtime();
        IValue[] outputTuple;
        if (hypo == null && state == null)
            outputTuple = mModuleEncoder.forward(IValue.from(inTensor)).toTuple();
        else
            outputTuple = mModuleEncoder.forward(IValue.from(inTensor), hypo, state).toTuple();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        System.out.println("inference time (ms): " + inferenceTime);
        final String transcript = outputTuple[0].toStr();
        hypo = outputTuple[1];
        state = outputTuple[2];
        if (transcript.length() > 0)
            System.out.println("transcript=" + transcript);
        return transcript;
    }
}