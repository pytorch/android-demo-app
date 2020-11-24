package org.pytorch.objectdetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ObjectDetectionActivity extends AbstractCameraXActivity<ObjectDetectionActivity.AnalysisResult> {
    private static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    public static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};

    private Module mModule = null;
    private ResultView mResultView;

    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_object_detection;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                .inflate()
                .findViewById(R.id.object_detection_texture_view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        for (Result rslt: result.mResults) {
            Log.d("<<<!!!", ResultView.classes[rslt.classIndex]);
        }


        mResultView.setResults(result.mResults);
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

        try {
            if (mModule == null) {
                mModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "yolov5s.torchscript.pt"));
            }
            Bitmap bitmap = imgToBitmap(image.getImage());
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.inputWidth, PrePostProcessor.inputHeight, true);

            long tmstart = SystemClock.elapsedRealtime();

            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, NO_MEAN_RGB, NO_STD_RGB);
            //final float[] inputs = inputTensor.getDataAsFloatArray();
            IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
            final Tensor outputTensor = outputTuple[0].toTensor();
            final float[] outputs = outputTensor.getDataAsFloatArray();

            float imgScaleX = (float)bitmap.getWidth() / PrePostProcessor.inputWidth;
            float imgScaleY = (float)bitmap.getHeight() / PrePostProcessor.inputHeight;

//            mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageView.getWidth() / mBitmap.getWidth() : (float)mImageView.getHeight() / mBitmap.getHeight());
//            mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageView.getHeight() / mBitmap.getHeight() : (float)mImageView.getWidth() / mBitmap.getWidth());
//
//            mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
//            mStartY = (mImageView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;



            final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, 1.0f, 1.0f, 0, 0);
            for (Result result: results) {
                Log.d("<<<", ResultView.classes[result.classIndex]);
            }

            Log.d(">>>>>>>>>INFERENCE", "" + (SystemClock.elapsedRealtime() - tmstart));


            return new AnalysisResult(results);

        }
        catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
