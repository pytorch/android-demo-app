package com.example.seq2seqnmt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Runnable{
    public static final String TAG = "Seq2SeqNMT";

    private Module mModuleEncoder;
    private Module mModuleDecoder;
    private Tensor mInputTensor;
    private LongBuffer mInputTensorBuffer;

    private EditText mEditText;
    private TextView mTextView;
    private Button mButton;


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

        final String result = translate(mEditText.getText().toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTranslationResult(result);
                mButton.setEnabled(true);
            }
        });
    }

    // run on worker thread
    private String translate(final String text) {
        final int MAX_LENGTH = 50; // need to be the same as Python code
        final int HIDDEN_SIZE = 256; // need to be the same as Python code
        // test NMT encoder and decoder
        if (mModuleEncoder == null) {
            final String moduleFileAbsoluteFilePath = new File(
                    assetFilePath(this, "optimized_encoder_150k.pth")).getAbsolutePath();
            mModuleEncoder = Module.load(moduleFileAbsoluteFilePath);
        }

        String json = null;
        JSONObject wrd2idx; // source - french
        JSONObject idx2wrd; // target - english
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


        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }

        //String french = "je cherche une chambre a louer";
        String french = text; // "je suis toujours fier de ma famille .";
        //long[] inputs =  new long [] {67, 350, 429, 87, 67, 125, 38, 1};
        long[] inputs =  new long [french.split(" ").length];
        try {
            for (int i = 0; i < french.split(" ").length; i++) {
                inputs[i] = wrd2idx.getLong(french.split(" ")[i]);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
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
            final String moduleFileAbsoluteFilePath = new File(
                    assetFilePath(this, "optimized_decoder_150k.pth")).getAbsolutePath();
            mModuleDecoder = Module.load(moduleFileAbsoluteFilePath);
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
            // find the top item in decoderOutputTensor
            float[] outputs = decoderOutputTensor.getDataAsFloatArray();
            int top_i = 0;
            float top_v = -100000.0f;
            for (int j=0; j<outputs.length; j++) {
                if (outputs[j] > top_v) {
                    top_v = outputs[j];
                    top_i = j;
                }
            }

            if (top_i == 1) // EOS_token, defined in Python
                break;

            result.add(top_i);
            mInputTensorBuffer = Tensor.allocateLongBuffer(1);
            mInputTensorBuffer.put(top_i);
            mInputTensor = Tensor.fromBlob(mInputTensorBuffer, decoderInputShape);
        }

        String english = "";
        try {
            for (int i = 0; i < result.size(); i++)
                english += " " + idx2wrd.getString("" + result.get(i));


        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return english;
    }

}