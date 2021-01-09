package org.pytorch.demo.vision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import org.pytorch.demo.BaseModuleActivity;
import org.pytorch.demo.FaceDetectionActivity;
import org.pytorch.demo.StatusBarUtils;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfig;
import androidx.core.app.ActivityCompat;

import java.io.File;

public abstract class AbstractCameraXActivity<R> extends BaseModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final int REQUEST_CODE_INTERNET_PERMISSION = 202;

    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    protected VideoCapture videoCapture;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.setStatusBarOverlay(getWindow(), true);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }
//    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            != PackageManager.PERMISSION_GRANTED) {
//      ActivityCompat.requestPermissions(
//              this,
//              new String[] {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
//              2077);
//    }else
//      setupCameraX();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't use image classification example without granting CAMERA permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            else if (grantResults[2] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't store video without granting Write external storage permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            else if (grantResults[1] == PackageManager.PERMISSION_DENIED)
            {
                Toast.makeText(
                        this,
                        "You can't use fr example without granting Internet permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            else if (grantResults[3] == PackageManager.PERMISSION_DENIED)
            {
                Toast.makeText(
                        this,
                        "You can't use fr example without granting read storage permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
        }
        else
            setupCameraX();

    }

    @SuppressLint("RestrictedApi")
    private void setupCameraX() {
        final TextureView textureView = getCameraPreviewTextureView();
        final PreviewConfig previewConfig = new PreviewConfig.Builder()
                .build();

        final Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));

        //VideoCapture NOT SUPPORTED by current cameraX, wait till further implementation
//        final VideoCaptureConfig videoCaptureConfig =
//                new VideoCaptureConfig.Builder()
//                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
//                        .setLensFacing(CameraX.LensFacing.FRONT)
//                        .build();
//        videoCapture = new VideoCapture(videoCaptureConfig);
        final ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setTargetResolution(new Size(1024, 1024))
                        .setCallbackHandler(mBackgroundHandler)
                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                        .build();
        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(
                (image, rotationDegrees) -> {
//                    if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 50) {
//                        return;
//                    }
                    System.out.println("BitmapToVideoEncoder in cameraX analyze time is "+ (SystemClock.elapsedRealtime() - mLastAnalysisResultTime));
                    final R result = analyzeImage(image, rotationDegrees);
                    if (result != null) {
                        mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                        runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
                    }
                });
        //VideoCapture NOT SUPPORTED by current cameraX, wait till further implementation
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @WorkerThread
    @Nullable
    protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(R result);

    @SuppressLint("RestrictedApi")
    @WorkerThread
    protected void startRecord(File file, FaceDetectionActivity faceDetectionActivity)
    {
        videoCapture.startRecording(file, new VideoCapture.OnVideoSavedListener(){
            @Override public void onVideoSaved(File file){
                Toast.makeText(faceDetectionActivity, "file saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                Log.i("in onclick listener", "file saved to " + file.getAbsolutePath());
            }
            @Override public void onError(VideoCapture.VideoCaptureError vce, String s, Throwable t){
                Log.i("in onclick listener", "file save failed error is "+vce.toString());
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @WorkerThread
    protected void stopRecord()
    {
        videoCapture.stopRecording();
    }
}
