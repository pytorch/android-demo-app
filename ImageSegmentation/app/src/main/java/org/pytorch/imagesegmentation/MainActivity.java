package org.pytorch.imagesegmentation;

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
    private static final String imagename = "deeplab.jpg";

    // see http://host.robots.ox.ac.uk:8080/pascal/VOC/voc2007/segexamples/index.html for the list of classes with indexes
    private static final int CLASSNUM = 21;
    private static final int DOG = 12;
    private static final int PERSON = 15;
    private static final int SHEEP = 17;

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
        } catch (IOException e) {
            Log.e("ImageSegmentation", "Error reading assets", e);
            finish();
        }

        final ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);

        final Button buttonRestart = findViewById(R.id.restartButton);
        buttonRestart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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

                final float[] scores = outputTensor.getDataAsFloatArray();

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] intValues = new int[width * height];
                for (int i = 0; i < intValues.length; i++) {
                    intValues[i] = 0xFFFFFFFF;
                }

                for (int j = 0; j < width; j++) {
                    for (int k = 0; k < height; k++) {
                        int maxj = 0;
                        int maxk = 0;
                        int maxi = 0;

                        double maxnum = -100000.0;
                        for (int i=0; i < CLASSNUM; i++) {
                            if (scores[i*(width*height) + j*width + k] > maxnum) {
                                maxnum = scores[i*(width*height) + j*width + k];
                                maxj = j; maxk= k; maxi = i;
                            }
                        }

                        if (maxi == PERSON)
                            intValues[maxj*width + maxk] = 0xFFFF0000;
                        else if (maxi == DOG)
                            intValues[maxj*width + maxk] = 0xFF00FF00;
                        else if (maxi == SHEEP)
                            intValues[maxj*width + maxk] = 0xFF0000FF;
                        else
                            intValues[maxj*width + maxk] = 0xFF000000;
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
