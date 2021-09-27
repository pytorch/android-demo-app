// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.seq2seqnmt;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.pytorch.LiteModuleLoader;

public class MainActivity extends AppCompatActivity implements Runnable{
    // to be consistent with the model inputs defined in seq2seq_nmt.py, based on
    // https://pytorch.org/tutorials/intermediate/seq2seq_translation_tutorial.html
    private static final int HIDDEN_SIZE = 256;
    private static final int EOS_TOKEN = 1;
    private static final int MAX_LENGTH = 50;
    private static final String TAG = MainActivity.class.getName();

    private Module mModuleEncoder;
    private Module mModuleDecoder;
    private Tensor mInputTensor;
    private LongBuffer mInputTensorBuffer;

    private EditText mEditText;
    private TextView mTextView;
    private Button mButton;

    private static String assetFilePath(Context context, String assetName) throws IOException {
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

        mButton = findViewById(R.id.btnTranslate);
        mEditText = findViewById(R.id.etFrom);
        mTextView = findViewById(R.id.tvTo);

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mButton.setEnabled(false);
                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });
    }

    private void showTranslationResult(String result) {
        mTextView.setText(result);
    }

    public void run() {
        final String result = translate(mEditText.getText().toString());
        runOnUiThread(() -> {
            showTranslationResult(result);
            mButton.setEnabled(true);
        });
    }

    private String translate(final String text) {
        if (mModuleEncoder == null) {
            try {
                mModuleEncoder = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "optimized_encoder_150k.ptl"));
            } catch (IOException e) {
                Log.e(TAG, "Error reading assets", e);
                finish();
            }
        }

        String json;
        JSONObject wrd2idx;
        JSONObject idx2wrd;
        try {
            InputStream is = getAssets().open("target_idx2wrd.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
            idx2wrd = new JSONObject(json);

            is = getAssets().open("source_wrd2idx.json");
            size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
            wrd2idx = new JSONObject(json);
        } catch (JSONException | IOException e) {
            android.util.Log.e(TAG, "JSONException | IOException ", e);
            return null;
        }

        String french = text;
        long[] inputs =  new long [french.split(" ").length];
        try {
            for (int i = 0; i < french.split(" ").length; i++) {
                inputs[i] = wrd2idx.getLong(french.split(" ")[i]);
            }
        }
        catch (JSONException e) {
            android.util.Log.e(TAG, "JSONException ", e);
            return null;
        }

        final long[] inputShape = new long[]{1};
        final long[] hiddenShape = new long[]{1, 1, 256};
        final FloatBuffer hiddenTensorBuffer =
                Tensor.allocateFloatBuffer(1 * 1 * 256);
        Tensor hiddenTensor = Tensor.fromBlob(hiddenTensorBuffer, hiddenShape);

        final long[] outputsShape = new long[]{MAX_LENGTH, HIDDEN_SIZE};
        final FloatBuffer outputsTensorBuffer =
                Tensor.allocateFloatBuffer(MAX_LENGTH  * HIDDEN_SIZE);

        for (int i=0; i<inputs.length; i++) {
            LongBuffer inputTensorBuffer = Tensor.allocateLongBuffer(1);
            inputTensorBuffer.put(inputs[i]);
            Tensor inputTensor = Tensor.fromBlob(inputTensorBuffer, inputShape);
            final IValue[] outputTuple = mModuleEncoder.forward(IValue.from(inputTensor), IValue.from(hiddenTensor)).toTuple();
            final Tensor outputTensor = outputTuple[0].toTensor();
            outputsTensorBuffer.put(outputTensor.getDataAsFloatArray());
            hiddenTensor = outputTuple[1].toTensor();
        }

        Tensor outputsTensor = Tensor.fromBlob(outputsTensorBuffer, outputsShape);
        final long[] decoderInputShape = new long[]{1, 1};
        if (mModuleDecoder == null) {
            try {
                mModuleDecoder = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "optimized_decoder_150k.ptl"));
            } catch (IOException e) {
                Log.e(TAG, "Error reading assets", e);
                finish();
            }
        }

        mInputTensorBuffer = Tensor.allocateLongBuffer(1);
        mInputTensorBuffer.put(0);
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, decoderInputShape);
        ArrayList<Integer> result = new ArrayList<>(MAX_LENGTH);
        for (int i=0; i<MAX_LENGTH; i++) {
            final IValue[] outputTuple = mModuleDecoder.forward(
                IValue.from(mInputTensor),
                IValue.from(hiddenTensor),
                IValue.from(outputsTensor)).toTuple();
            final Tensor decoderOutputTensor = outputTuple[0].toTensor();
            hiddenTensor = outputTuple[1].toTensor();
            float[] outputs = decoderOutputTensor.getDataAsFloatArray();
            int topIdx = 0;
            double topVal = -Double.MAX_VALUE;
            for (int j=0; j<outputs.length; j++) {
                if (outputs[j] > topVal) {
                    topVal = outputs[j];
                    topIdx = j;
                }
            }

            if (topIdx == EOS_TOKEN) break;

            result.add(topIdx);
            mInputTensorBuffer = Tensor.allocateLongBuffer(1);
            mInputTensorBuffer.put(topIdx);
            mInputTensor = Tensor.fromBlob(mInputTensorBuffer, decoderInputShape);
        }

        String english = "";
        try {
            for (int i = 0; i < result.size(); i++)
                english += " " + idx2wrd.getString("" + result.get(i));
        }
        catch (JSONException e) {
            android.util.Log.e(TAG, "JSONException ", e);
        }
        return english;
    }
}