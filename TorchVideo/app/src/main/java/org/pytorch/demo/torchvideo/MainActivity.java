package org.pytorch.demo.torchvideo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
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
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {
    private final String TAG = MainActivity.class.getSimpleName();
    private Button mButtonPauseResume;
    private Module mModule = null;
    private int mTestVideoIndex = 0;
    private final String[] mTestVideos = {"video1", "video2", "video3", "video4", "video5"};
    private String[] mClasses;
    private List<String> mResults = new ArrayList<>();
    private VideoView mVideoView;
    private TextView mTextView;
    private Uri mVideoUri;

    private Thread mThread;
    private boolean mStopThread;

    private final static float[] MEAN_RGB = new float[] {0.45f, 0.45f, 0.45f};
    private final static float[] STD_RGB = new float[] {0.225f, 0.225f, 0.225f};
    private final static int COUNT_OF_FRAMES_PER_INFERENCE = 4;
    private final static int TARGET_VIDEO_SIZE = 160;
    private final static int MODEL_INPUT_SIZE = COUNT_OF_FRAMES_PER_INFERENCE * 3 * TARGET_VIDEO_SIZE * TARGET_VIDEO_SIZE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                mTestVideoIndex = (mTestVideoIndex + 1) % mTestVideos.length;
                buttonTest.setText(String.format("Video %d/%d", mTestVideoIndex + 1, mTestVideos.length));
                mTextView.setText("");
                mTextView.setVisibility(View.INVISIBLE);
                mStopThread = true;
                mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
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

        final Button buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("video/*");
                startActivityForResult(pickIntent, 1);
            }
        });

        mVideoView = findViewById(R.id.videoView);
        mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
        setVideo();
    }

    private void setVideo() {
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
            String top5[] = new String[5];
            for (int j = 0; j < 5; j++)
                top5[j] = mClasses[scoresIdx[j]];
            final String result = String.join(", ", top5);
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

        if (!mStopThread) {
            runOnUiThread(() -> mButtonPauseResume.setVisibility(View.INVISIBLE));
        }
    }


    private Pair<Integer[], Long> getResult(int fromMs, int toMs, MediaMetadataRetriever mmr) {

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(MODEL_INPUT_SIZE);

        for (int i = 0; i < COUNT_OF_FRAMES_PER_INFERENCE; i++) {
            long timeUs = 1000 * (fromMs + (int) ((toMs - fromMs) * i / (COUNT_OF_FRAMES_PER_INFERENCE - 1.)));
            Bitmap bitmap = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) / (float)TARGET_VIDEO_SIZE;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() / ratio), (int)(bitmap.getHeight() / ratio), true);
            Bitmap centerCroppedBitmap = Bitmap.createBitmap(resizedBitmap,
                    resizedBitmap.getWidth() > resizedBitmap.getHeight() ? (resizedBitmap.getWidth() - resizedBitmap.getHeight()) / 2 : 0,
                    resizedBitmap.getHeight() > resizedBitmap.getWidth() ? (resizedBitmap.getHeight() - resizedBitmap.getWidth()) / 2 : 0,
                    TARGET_VIDEO_SIZE, TARGET_VIDEO_SIZE);

            TensorImageUtils.bitmapToFloatBuffer(centerCroppedBitmap, 0, 0,
                    TARGET_VIDEO_SIZE, TARGET_VIDEO_SIZE, MEAN_RGB, STD_RGB, inTensorBuffer,
                    (COUNT_OF_FRAMES_PER_INFERENCE - 1) * i * TARGET_VIDEO_SIZE * TARGET_VIDEO_SIZE);
        }

        Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, COUNT_OF_FRAMES_PER_INFERENCE, 160, 160});

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
                setVideo();

//
//
//                String[] filePathColumn = {MediaStore.Images.Media.DATA};
//                if (selectedMediaUri != null) {
//                    Cursor cursor = getContentResolver().query(selectedMediaUri,
//                            filePathColumn, null, null, null);
//                    if (cursor != null) {
//                        cursor.moveToFirst();
//                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//                        String picturePath = cursor.getString(columnIndex);
//                        cursor.close();
//                    }
//                }

            }
        }
    }
}