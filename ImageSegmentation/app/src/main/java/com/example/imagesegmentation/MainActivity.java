package com.example.imagesegmentation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Bitmap bitmap = null;
    private Module module = null;
    private String imagename = "deeplab.jpg";

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
            bitmap = BitmapFactory.decodeStream(getAssets().open(imagename));
            //bitmap = BitmapFactory.decodeStream(getAssets().open("kitten.jpg"));

        } catch (IOException e) {
            Log.e("ImageSegmentation", "Error reading assets", e);
            finish();
        }

        // showing image on UI
        final ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);

        final Button buttonNext = findViewById(R.id.nextButton);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (imagename == "deeplab.jpg")
                    imagename = "kitten.jpg";
                else
                    imagename = "deeplab.jpg";

                try {
                    bitmap = BitmapFactory.decodeStream(getAssets().open(imagename));
                } catch (IOException e) {
                    Log.e("ImageSegmentation", "Error reading assets", e);
                    finish();
                }

                final ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
            }
        });


        final Button buttonSegment = findViewById(R.id.segmentButton);
        buttonSegment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSegment.setEnabled(false);
                try {
                    module = Module.load(MainActivity.assetFilePath(getApplicationContext(), "deeplabv3_scripted.pt"));
                } catch (IOException e) {
                    Log.e("ImageSegmentation", "Error reading assets", e);
                    finish();
                }
                final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

                Map<String, IValue> outTensors = module.forward(IValue.from(inputTensor)).toDictStringKey();
                final Tensor outputTensor = outTensors.get("out").toTensor();

                // getting tensor content as java array of floats
                final float[] scores = outputTensor.getDataAsFloatArray();

                int classnum = 21;
                int width = 179;
                int height = 179;
                int[] intValues = new int[width * height];
                for (int i = 0; i < intValues.length; ++i) {
                    intValues[i] = 0xFFFFFFFF;
                }

                for (int j=0; j<width; j++) {
                    for (int k=0; k<height; k++) {
                        int maxj = 0;
                        int maxk = 0;
                        int maxi = 0;

                        double maxnum = -100000.0;
                        for (int i=0; i < classnum; i++) {
                            if (scores[i*(width*height) + j*width + k] > maxnum) {
                                maxnum = scores[i*(width*height) + j*width + k];
                                maxj = j; maxk= k; maxi = i;
                            }
                        }

                        if (maxi==15) // for both kitten and deeplab
                            intValues[maxj*width + maxk] = 0xFFFF0000;
                        else if (maxi==17) // 17 - deeplab, 8 - kitten
                            intValues[maxj*width + maxk] = 0xFF0000FF;
                        else if (maxi==8) // 17 - deeplab, 8 - kitten
                            intValues[maxj*width + maxk] = 0xFF0000FF;

                    }
                }

                Bitmap bmpSegmentation = Bitmap.createScaledBitmap(bitmap, width, height, true);

                Bitmap outputBitmap = bmpSegmentation.copy( bmpSegmentation.getConfig() , true);
                outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
                Bitmap mTransferredBitmap = Bitmap.createScaledBitmap(outputBitmap, bitmap.getWidth(), bitmap.getHeight(), true);
                imageView.setImageBitmap(mTransferredBitmap);
                buttonSegment.setEnabled(true);
            }
        });
    }
}
