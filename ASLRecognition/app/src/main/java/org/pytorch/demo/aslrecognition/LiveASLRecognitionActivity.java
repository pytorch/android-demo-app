package org.pytorch.demo.aslrecognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.demo.aslrecognition.MainActivity;
import org.pytorch.demo.aslrecognition.R;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;


public class LiveASLRecognitionActivity extends org.pytorch.demo.aslrecognition.AbstractCameraXActivity<LiveASLRecognitionActivity.AnalysisResult> {
        private Module mModule = null;
        private TextView mResultView;
        private int mFrameCount = 0;
        private FloatBuffer inTensorBuffer;


    static class AnalysisResult {
            private final String mResults;

            public AnalysisResult(String results) {
                mResults = results;
            }
        }

        @Override
        protected int getContentViewLayoutId() {
            return R.layout.activity_live_asl_recognition;
        }

        @Override
        protected TextureView getCameraPreviewTextureView() {
            mResultView = findViewById(R.id.resultView);
            return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                    .inflate()
                    .findViewById(R.id.object_detection_texture_view);
        }

        @Override
        protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
            mResultView.setText(result.mResults);
            mResultView.invalidate();
        }

        private Bitmap imgToBitmap(Image image) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }


        @Override
        @WorkerThread
        @Nullable
        protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
            if (mModule == null) {
                try {
                    mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "video_classification.ptl"));
                } catch (IOException e) {
                    return null;
                }
            }

            if (mFrameCount == 0)
                inTensorBuffer = Tensor.allocateFloatBuffer(111111);

            Bitmap bitmap = imgToBitmap(image.getImage());
            Matrix matrix = new Matrix();
            matrix.postRotate(90.0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 160.0f;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() / ratio), (int)(bitmap.getHeight() / ratio), true);

            Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, 160, 160});

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

//            String tops[] = new String[Constants.TOP_COUNT];
//            for (int j = 0; j < Constants.TOP_COUNT; j++)
//                tops[j] = MainActivity.getClasses()[scoresIdx[j]];
            final String result = "";//String.join(", ", tops);
            return new AnalysisResult(String.format("%s - %dms", result, inferenceTime));
        }
    }
