package org.pytorch.demo.torchvideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements Runnable {
    private final String TAG = MainActivity.class.getSimpleName();
    private final String[] mTestVideos = {"video1", "video2", "video3"};

    private Button mButtonPauseResume;
    private Button mButtonTest;
    private Module mModule = null;
    private int mTestVideoIndex = 0;
    private static String[] mClasses;
    private List<String> mResults = new ArrayList<>();
    private VideoView mVideoView;
    private TextView mTextView;
    private Uri mVideoUri;
    private Thread mThread;
    private boolean mStopThread;

    public static String assetFilePath(Context context, String assetName) throws IOException {
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
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "video_classification.ptl"));

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

        mButtonTest = findViewById(R.id.testButton);
        mButtonTest.setText(String.format("Test 1/%d", mTestVideos.length));
        mButtonTest.setEnabled(false);
        mButtonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mTestVideoIndex = (mTestVideoIndex + 1) % mTestVideos.length;
                mButtonTest.setText(String.format("Test %d/%d", mTestVideoIndex + 1, mTestVideos.length));
                mButtonTest.setEnabled(false);
                mTextView.setText("");
                mTextView.setVisibility(View.INVISIBLE);
                mStopThread = true;
                mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
                startVideo();
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

        final Button buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mStopThread = true;
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("video/*");
                startActivityForResult(pickIntent, 1);
            }
        });

        final Button buttonLive = findViewById(R.id.liveButton);
        buttonLive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mStopThread = true;
                final Intent intent = new Intent(MainActivity.this, LiveVideoClassificationActivity.class);
                startActivity(intent);
            }
        });

        mVideoView = findViewById(R.id.videoView);
        mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
        startVideo();
    }

    private void startVideo() {
        mVideoView.setVideoURI(mVideoUri);
        mVideoView.start();
        mButtonPauseResume.setVisibility(View.VISIBLE);
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

    private void stopVideo() {
        mVideoView.stopPlayback();
        mButtonPauseResume.setVisibility(View.INVISIBLE);
        mButtonTest.setEnabled(true);
        mStopThread = true;
    }

    private Uri getMedia(String mediaName) {
        return Uri.parse("android.resource://" + getPackageName() + "/raw/" + mediaName);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mVideoView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopVideo();
    }

    @Override
    public void run() {

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this.getApplicationContext(), mVideoUri);
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

            final Pair<Integer[], Long> pair = getResult(from, to, mmr);
            final Integer[] scoresIdx = pair.first;
            String tops[] = new String[Constants.TOP_COUNT];
            for (int j = 0; j < Constants.TOP_COUNT; j++)
                tops[j] = mClasses[scoresIdx[j]];
            final String result = String.join(", ", tops);
            final long inferenceTime = pair.second;

            if (i * 1000 > mVideoView.getCurrentPosition()) {
                try {
                    Thread.sleep(i * 1000 - mVideoView.getCurrentPosition());
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }

            while (!mVideoView.isPlaying()) {
                if (mStopThread || (mVideoView.getCurrentPosition() >= mVideoView.getDuration())) break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }

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

        // video playing is completed
        if (!mStopThread) {
            runOnUiThread(() -> mButtonPauseResume.setVisibility(View.INVISIBLE));
            runOnUiThread(() -> mButtonTest.setEnabled(true));
        }
    }


    private Pair<Integer[], Long> getResult(int fromMs, int toMs, MediaMetadataRetriever mmr) {

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(Constants.MODEL_INPUT_SIZE);

        // extract 4 frames for each second of the video and pack them to a float buffer to be converted to the model input tensor
        for (int i = 0; i < Constants.COUNT_OF_FRAMES_PER_INFERENCE; i++) {
            long timeUs = 1000 * (fromMs + (int) ((toMs - fromMs) * i / (Constants.COUNT_OF_FRAMES_PER_INFERENCE - 1.)));
            Bitmap bitmap = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) / (float)Constants.TARGET_VIDEO_SIZE;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() / ratio), (int)(bitmap.getHeight() / ratio), true);
            Bitmap centerCroppedBitmap = Bitmap.createBitmap(resizedBitmap,
                    resizedBitmap.getWidth() > resizedBitmap.getHeight() ? (resizedBitmap.getWidth() - resizedBitmap.getHeight()) / 2 : 0,
                    resizedBitmap.getHeight() > resizedBitmap.getWidth() ? (resizedBitmap.getHeight() - resizedBitmap.getWidth()) / 2 : 0,
                    Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE);

            TensorImageUtils.bitmapToFloatBuffer(centerCroppedBitmap, 0, 0,
                    Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE, Constants.MEAN_RGB, Constants.STD_RGB, inTensorBuffer,
                    (Constants.COUNT_OF_FRAMES_PER_INFERENCE - 1) * i * Constants.TARGET_VIDEO_SIZE * Constants.TARGET_VIDEO_SIZE);
        }

        Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, Constants.COUNT_OF_FRAMES_PER_INFERENCE, 160, 160});

        final long startTime = SystemClock.elapsedRealtime();
        Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;

        final float[] scores = outputTensor.getDataAsFloatArray();
        Integer scoresIdx[] = new Integer[scores.length];
        for (int i = 0; i < scores.length; i++)
            scoresIdx[i] = i;

        Arrays.sort(scoresIdx, new Comparator<Integer>() {
            @Override public int compare(final Integer o1, final Integer o2) {
                return Float.compare(scores[o2], scores[o1]);
            }
        });

        return new Pair<>(scoresIdx, inferenceTime);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri.toString().contains("video")) {
                mVideoUri = selectedMediaUri;
                startVideo();
            }
        }
    }

    public static String[] getClasses() {
        return mClasses;
    }
}