package org.pytorch.demo.torchvideo;

import android.Manifest;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {
    private final String TAG = MainActivity.class.getSimpleName();
    private Button mButtonPauseResume;
    private Module mModule = null;
    private int mTestVideoIndex = 0;
    private String[] mTestVideos = {"video3"};//{"video1", "video2", "video3"};
    private String[] mClasses;
    private List<String> mResults = new ArrayList<>();
    private VideoView mVideoView;
    private TextView mTextView;

    private Thread mThread;
    private boolean mStopThread;

    private final static float[] MEAN_RGB = new float[] {0.45f, 0.45f, 0.45f};
    private final static float[] STD_RGB = new float[] {0.225f, 0.225f, 0.225f};
    private final static int INPUT_SIZE = 3 * 4 * 160 * 160;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                1000);

        try {
            mModule = PyTorchAndroid.loadModuleFromAsset(getAssets(), "video_classification.pt");

            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            mClasses = new String[classes.size()];
            classes.toArray(mClasses);
        } catch (IOException e) {
            Log.e(TAG, "Error reading model file", e);
            finish();
        }

        mTextView = findViewById(R.id.textView);
        mTextView.setVisibility(View.INVISIBLE);

        final Button buttonTest = findViewById(R.id.testButton);
        buttonTest.setText(String.format("Video 1/%d", mTestVideos.length));
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //mVideoView.stopPlayback();
                mTestVideoIndex = (mTestVideoIndex + 1) % mTestVideos.length;
                buttonTest.setText(String.format("Video %d/%d", mTestVideoIndex + 1, mTestVideos.length));
                mTextView.setText("");
                mTextView.setVisibility(View.INVISIBLE);
                mStopThread = true;
                setVideo();
            }
        });


        mButtonPauseResume = findViewById(R.id.pauseResumeButton);
        mButtonPauseResume.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mVideoView.isPlaying()) {
                    mButtonPauseResume.setText(getString(R.string.resume));
                    mVideoView.pause();
                }
                else {
                    mButtonPauseResume.setText(getString(R.string.pause));
                    mVideoView.start();
                }
            }
        });

        mVideoView = findViewById(R.id.videoView);
        setVideo();

    }

    private void setVideo() {
        Uri videoUri = getMedia(mTestVideos[mTestVideoIndex]);
        mVideoView.setVideoURI(videoUri);
        mVideoView.start();
        mButtonPauseResume.setText(getString(R.string.pause));

        if (mThread != null && mThread.isAlive()) {
            try {
                mThread.join();
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        mStopThread = false;
        mThread = new Thread(MainActivity.this);
        mThread.start();
    }


    private void releasePlayer() {
        mVideoView.stopPlayback();
    }

    private Uri getMedia(String mediaName) {
        return Uri.parse("android.resource://" + getPackageName() + "/raw/" + mediaName);
    }

    @Override
    protected void onStop() {
        super.onStop();

        releasePlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            mVideoView.pause();
        }
    }

    @Override
    public void run() {

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this.getApplicationContext(), getMedia(mTestVideos[mTestVideoIndex]));
        String stringDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        double durationMs = Double.parseDouble(stringDuration);

        // for each second of the video, make inference to get the class label
        int durationTo = (int) Math.ceil(durationMs / 1000);

        mResults.clear();
        for (int i = 0; !mStopThread && i < durationTo; i++) {
            int from = i * 1000;
            int to = (i + 1) * 1000;
            if (i == durationTo - 1)
                to = (int) Math.ceil(durationMs) - (i * 1000);

            final long startTime = SystemClock.elapsedRealtime();
            final String result = getResult(from, to, mmr);
            final long inferenceTime = SystemClock.elapsedRealtime() - startTime;

            if (i * 1000 > mVideoView.getCurrentPosition()) {
                try {
                    Thread.sleep(i*1000 - mVideoView.getCurrentPosition());
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }

            while (!mVideoView.isPlaying()) {
                if (mStopThread || (mVideoView.getCurrentPosition() >= mVideoView.getDuration())) break;
                try {
                    Log.d(">>>>>", "sleep 100" +  mVideoView.getCurrentPosition()+","+ mVideoView.getDuration());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }
            //if (mStopThread) break;

            final int finalI = i;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setVisibility(View.VISIBLE);
                    mTextView.setText(String.format("%ds: %s - %dms", finalI +1, result, inferenceTime));
                }
            });
            mResults.add(result);
        }
    }


    public static void saveBitmap(String name, Bitmap bitmap) {

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/" + name + ".png");
        try {
            f.createNewFile();
            FileOutputStream fOut = null;
            fOut = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getResult(int from, int to, MediaMetadataRetriever mmr) {
        int diff = to - from;

        long startTime = SystemClock.elapsedRealtime();

        Bitmap bitmap1 = mmr.getFrameAtTime(from, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        Bitmap bitmap2 = mmr.getFrameAtTime(1000 * (from + (int)(diff * 0.33)), MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        Bitmap bitmap3= mmr.getFrameAtTime(1000 * (from + (int)(diff * 0.67)),MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        Bitmap bitmap4 = mmr.getFrameAtTime(1000 * to, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

        Log.d("!!!!!!!", "getFrame time: " + (SystemClock.elapsedRealtime() - startTime));

        saveBitmap(String.format("%s_%d_%d", mTestVideos[mTestVideoIndex], from/1000, 0), bitmap1);
        saveBitmap(String.format("%s_%d_%d", mTestVideos[mTestVideoIndex], from/1000, 1), bitmap2);
        saveBitmap(String.format("%s_%d_%d", mTestVideos[mTestVideoIndex], from/1000, 2), bitmap3);
        saveBitmap(String.format("%s_%d_%d", mTestVideos[mTestVideoIndex], from/1000, 3), bitmap4);

        startTime = SystemClock.elapsedRealtime();

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(INPUT_SIZE);

        Bitmap resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1, 160, 160, true);
        Bitmap resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2, 160, 160, true);
        Bitmap resizedBitmap3 = Bitmap.createScaledBitmap(bitmap3, 160, 160, true);
        Bitmap resizedBitmap4 = Bitmap.createScaledBitmap(bitmap4, 160, 160, true);

//        Log.d("!!!!!!!", "resizedBitmap time: " + (SystemClock.elapsedRealtime() - startTime));
//        startTime = SystemClock.elapsedRealtime();

        TensorImageUtils.bitmapToFloatBuffer(resizedBitmap1, 0,0,resizedBitmap1.getWidth(),resizedBitmap1.getHeight(), MEAN_RGB, STD_RGB, inTensorBuffer, 0);
        TensorImageUtils.bitmapToFloatBuffer(resizedBitmap2, 0,0,resizedBitmap2.getWidth(),resizedBitmap2.getHeight(), MEAN_RGB, STD_RGB, inTensorBuffer, 3 * resizedBitmap1.getWidth() * resizedBitmap1.getHeight());
        TensorImageUtils.bitmapToFloatBuffer(resizedBitmap3, 0,0,resizedBitmap3.getWidth(),resizedBitmap3.getHeight(), MEAN_RGB, STD_RGB, inTensorBuffer, 3 * 2 * resizedBitmap1.getWidth() * resizedBitmap1.getHeight());
        TensorImageUtils.bitmapToFloatBuffer(resizedBitmap4, 0,0,resizedBitmap4.getWidth(),resizedBitmap4.getHeight(), MEAN_RGB, STD_RGB, inTensorBuffer, 3 * 3 * resizedBitmap1.getWidth() * resizedBitmap1.getHeight());

//        Log.d("!!!!!!!", "bitmapToFloatBuffer time: " + (SystemClock.elapsedRealtime() - startTime));


        Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, 4, 160, 160});

        final float[] inputs = inputTensor.getDataAsFloatArray();

        startTime = SystemClock.elapsedRealtime();
        Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
        long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d("!!!!!!!", "inferenceTime="+inferenceTime);

        final float[] scores = outputTensor.getDataAsFloatArray();

        double maxnum = -Double.MAX_VALUE;
        int maxn = 0;
        for (int i = 0; i < 400; i++) {
            if (scores[i] > maxnum) {
                maxnum = scores[i];
                maxn = i;
            }
        }

        return mClasses[maxn];
    }
}