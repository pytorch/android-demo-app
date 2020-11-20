// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements Runnable {
    private int mImageIndex = 0;
    private String[] mTestImages = {"test1.png", "test2.jpg", "test3.png"};
    private static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    public static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};

    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;

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
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);

        final Button buttonTest = findViewById(R.id.testButton);
        buttonTest.setText(("Test Image 1/3"));
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);
                mImageIndex = (mImageIndex + 1) % mTestImages.length;
                buttonTest.setText(String.format("Text Image %d/%d", mImageIndex + 1, mTestImages.length));

                try {
                    mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
                    mImageView.setImageBitmap(mBitmap);
                } catch (IOException e) {
                    Log.e("Object Detection", "Error reading assets", e);
                    finish();
                }
            }
        });

        mButtonDetect = findViewById(R.id.detectButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonDetect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButtonDetect.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonDetect.setText(getString(R.string.run_model));

                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });

        try {
            mModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "yolov5s.torchscript.pt"));
        } catch (IOException e) {
            Log.e("ImageSegmentation", "Error reading assets", e);
            finish();
        }

    }

    @Override
    public void run() {

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, 640, 640, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, NO_MEAN_RGB, NO_STD_RGB);

        final float[] inputs = inputTensor.getDataAsFloatArray();
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int[] intValues = new int[width * height];
//        for (int j = 0; j < width; j++) {
//            for (int k = 0; k < height; k++) {
//                int maxi = 0, maxj = 0, maxk = 0;
//                double maxnum = -Double.MAX_VALUE;
//                for (int i = 0; i < CLASSNUM; i++) {
//                    if (outputs[i * (width * height) + j * width + k] > maxnum) {
//                        maxnum = outputs[i * (width * height) + j * width + k];
//                        maxi = i; maxj = j; maxk = k;
//                    }
//                }
//                if (maxi == PERSON)
//                    intValues[maxj * width + maxk] = 0xFFFF0000;
//                else if (maxi == DOG)
//                    intValues[maxj * width + maxk] = 0xFF00FF00;
//                else if (maxi == SHEEP)
//                    intValues[maxj * width + maxk] = 0xFF0000FF;
//                else
//                    intValues[maxj * width + maxk] = 0xFF000000;
//            }
//        }
//
//        Bitmap bmpSegmentation = Bitmap.createScaledBitmap(mBitmap, width, height, true);
//        Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
//        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
//        final Bitmap transferredBitmap = Bitmap.createScaledBitmap(outputBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mImageView.setImageBitmap(transferredBitmap);
                mButtonDetect.setEnabled(true);
                mButtonDetect.setText(getString(R.string.detect));
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                mResultView.setResults(mImageIndex*100 + 10);
                mResultView.invalidate();
                mResultView.setVisibility(View.VISIBLE);
            }
        });
    }
}
