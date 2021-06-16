// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.
// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.


package org.pytorch.demo.vit4mnist;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;

import java.nio.FloatBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {

    private Module mModule;

    private TextView mResultTextView;
    private Button mRecognizeButton;
    private Button mClearButton;
    private HandWrittenDigitView mDrawView;

    private static final float MNISI_STD = 0.1307f;
    private static final float MNISI_MEAN = 0.3081f;
    private static final float BLANK = - MNISI_STD / MNISI_MEAN;
    private static final float NON_BLANK = (1.0f - MNISI_STD) / MNISI_MEAN;
    private static final int MNIST_IMAGE_SIZE = 28;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTextView = findViewById(R.id.resultTextView);
        mDrawView = findViewById(R.id.drawview);
        mRecognizeButton = findViewById(R.id.recognizeButton);
        mClearButton = findViewById(R.id.clearButton);

        mRecognizeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultTextView.setText("");
                mDrawView.clearAllPointsAndRedraw();
            }
        });

        mModule = PyTorchAndroid.loadModuleFromAsset(getAssets(), "vit4mnist.pth");
    }

    public void run() {
        final int result = recognize();
        if (result == -1) return;
        runOnUiThread(() -> {
            mResultTextView.setText(mResultTextView.getText().toString() + " " + result);
            mDrawView.clearAllPoints();
        });
    }

    private int recognize() {
        // convert drawn points to the model input of size MNIST_IMAGE_SIZE*MNIST_IMAGE_SIZE
        List<List<Pair<Float, Float>>> allPoints = mDrawView.getAllPoints();
        if (allPoints.size() == 0) return -1;

        double[] inputs = new double[MNIST_IMAGE_SIZE * MNIST_IMAGE_SIZE];
        for (int i = 0; i < inputs.length; i++)
            inputs[i] = BLANK;

        final int width = mDrawView.getWidth();
        final int height = mDrawView.getHeight();
        // loop through each stroke
        for (List<Pair<Float, Float>> cp : allPoints) {
            // loop through each point in the stroke
            for (Pair<Float, Float> p : cp) {
                if (p.first.intValue() > width || p.second.intValue() > height ||
                    p.first.intValue() < 0 || p.second.intValue() < 0)
                    continue;
                int x = MNIST_IMAGE_SIZE * p.first.intValue() / width;
                int y = MNIST_IMAGE_SIZE * p.second.intValue() / height;
                int loc = y * MNIST_IMAGE_SIZE + x;
                inputs[loc] = NON_BLANK;
            }
        }

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(28 * 28);
        for (double val : inputs)
            inTensorBuffer.put((float) val);

        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 1, 28, 28});

        final Tensor outTensor = mModule.forward(IValue.from(inTensor)).toTensor();
        final float[] outputs = outTensor.getDataAsFloatArray();

        float sum = 0.0f;
        for (float val : outputs)
            sum += Math.exp(val);
        for (int i = 0; i < outputs.length; i++)
            outputs[i] = (float)(Math.exp(outputs[i]) / sum);

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] > maxScore) {
                maxScore = outputs[i];
                maxScoreIdx = i;
            }
        }

        return maxScoreIdx;
    }
}