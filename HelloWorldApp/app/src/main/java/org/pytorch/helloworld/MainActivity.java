package org.pytorch.helloworld;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import org.pytorch.torchvision.TensorImageUtils;
import java.nio.FloatBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bitmap bitmap = null;
        Module module = null;
        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
            bitmap = BitmapFactory.decodeStream(getAssets().open("RACHEL4.jpg"));
            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            module = Module.load(assetFilePath(this, "mobile_model1.pt"));
        } catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            finish();
        }

        float [] mean = new float[] {0.49804f, 0.49804f, 0.49804f};
        float [] std = new float[] {0.501960f, 0.501960f, 0.501960f};
        float nms_threshold = 0.4f;

        Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap,480,360,true);
//        Bitmap bitmap1;
//        if (bitmap.getWidth() > bitmap.getHeight())
//            bitmap1 = Bitmap.createScaledBitmap(bitmap,480,360,true);
//        else
//            bitmap1 = Bitmap.createScaledBitmap(bitmap,360,480,true);
//    Bitmap bitmap1 = getResizedBitmap(bitmap,480,360);
        // preparing input tensor
//        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1,
//                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std);
        System.out.println(Arrays.toString(inputTensor.shape()));
        //Normalize input
//        inputTensor = Normalize_tensor(inputTensor, 127, 128);
        // running the model
        final IValue[] output = module.forward(IValue.from(inputTensor)).toTuple();
//    for (int i = 0; i < output.length; i++)
//    {
//      IValue tupleElement = output[i];
//
//    }
        Tensor scores = output[0].toTensor();
        Tensor boxes = output[1].toTensor();
        float threshold = (float) 0.99;

        ArrayList<Float> possible_indexes = possible_score(scores, boxes, threshold);
        System.out.println("in onCreate len possible_indexes " + possible_indexes.size());

        ArrayList<float[]> nms_boxes = nms(boxes, scores, possible_indexes, nms_threshold);


        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setAlpha(100);
        for(int i = 0; i < nms_boxes.size(); i++)
        {
            float[] xyxy = {nms_boxes.get(i)[0]*bmp.getWidth(),nms_boxes.get(i)[1]*bmp.getHeight(),nms_boxes.get(i)[2]*bmp.getWidth(), nms_boxes.get(i)[3]*bmp.getHeight()};
            canvas.drawRect(xyxy[0], xyxy[1], xyxy[2], xyxy[3], paint);
        }


//        canvas.drawLine(0, 0, bmp.getWidth(), bmp.getHeight(), paint);
        //canvas.drawBitmap(bmp,0, 0, paint);
        // showing image on UI
        ImageView imageView = findViewById(R.id.image);
        imageView.setImageBitmap(bmp);
        // getting tensor content as java array of floats
//    final float[] scores = outputTensor.getDataAsFloatArray();
//
//    // searching for the index with maximum score
//    float maxScore = -Float.MAX_VALUE;
//    int maxScoreIdx = -1;
//    for (int i = 0; i < scores.length; i++) {
//      if (scores[i] > maxScore) {
//        maxScore = scores[i];
//        maxScoreIdx = i;
//      }
//    }
//
//    String className = ImageNetClasses.IMAGENET_CLASSES[maxScoreIdx];

        // showing className on UI
        TextView textView = findViewById(R.id.text);
        textView.setText("res");
    }

    /*
     *
     * box format: xyxy (topleft.x topleft.y bottomright.x bottomright.y)
     */
    float IoU(float[] rec1, float[] rec2)
    {

        float S_rec1 = (rec1[2] - rec1[0]) * (rec1[3] - rec1[1]);
        float S_rec2 = (rec2[2] - rec2[0]) * (rec2[3] - rec2[1]);

//    computing the sum_area
        float sum_area = S_rec1 + S_rec2;

//     find the each edge of intersect rectangle
        float left_line = max(rec1[1], rec2[1]);
        float right_line = min(rec1[3], rec2[3]);
        float top_line = max(rec1[0], rec2[0]);
        float bottom_line = min(rec1[2], rec2[2]);

//     judge if there is an intersect
        float intersect = 0;
        if (left_line >= right_line || top_line >= bottom_line)
            return 0;
        else
            intersect = (right_line - left_line) * (bottom_line - top_line);
            return (intersect / (sum_area - intersect));
    }

    ArrayList<float[]> nms(Tensor boxes, Tensor scores, ArrayList<Float> possible_indexes, float nms_threshold)
    {
//        float[] sfloatArray = scores.getDataAsFloatArray();
//        int slen = sfloatArray.length;
//        long snum = scores.shape()[2];
//        slen = (int)(slen / snum);

        ArrayList<float[]> nms_boxes = new ArrayList<>();
        float[] bfloatArray = boxes.getDataAsFloatArray();
        int blen = bfloatArray.length;
        int bnum = (int) boxes.shape()[2];
        blen = (blen / bnum);


        float[] box2 = {1,1,1,1,1};
        for(int i = 0; i < possible_indexes.size() / 2; i++)
        {
            float[] box1 = {0,0,0,0,0};
            int index = (int) (float) possible_indexes.get(i * 2);
            for(int j = 0; j < bnum; j++)
            {
                box1[j] = bfloatArray[index * bnum + j];
            }
            box1[bnum] = possible_indexes.get(i * 2 + 1);
            boolean flag = true;
            for(int j = 0; j < nms_boxes.size(); j++)
            {
                box2 = nms_boxes.get(j);
                if(IoU(box1, box2) > nms_threshold) {
                    if (box2[bnum] > box1[bnum]) { //prob of box2 > box1
                        nms_boxes.remove(j);
                        nms_boxes.add(box1);
                        flag = false;
                    } else {
                        flag = false;
                        break;
                    }
                }
            }
            if (flag)
                nms_boxes.add(box1);


        }

        return nms_boxes;
    }


    public Tensor Normalize_tensor(Tensor inputTensor, int mean, int std)
    {
        float[] floatArray = inputTensor.getDataAsFloatArray();
        for (int i = 0; i < floatArray.length; i++)
        {
            floatArray[i] = (floatArray[i] - mean) / std;
        }
        return Tensor.fromBlob(floatArray,inputTensor.shape());
    }
    public ArrayList<Float> possible_score(Tensor scores, Tensor boxes, float threshold)
    {
        ArrayList<Float> list_index_prob = new ArrayList<>();
        float[] floatArray = scores.getDataAsFloatArray();
        int len = floatArray.length;
        long num = scores.shape()[2];
        len = (int)(len / num);
        System.out.println(len);


        for (int i = 0; i < len; i++)
        {
            for (int j = 1; j < num; j++)
                if (floatArray[(int) (i * num + j)] > threshold)
                {
                    list_index_prob.add((float)i);
                    list_index_prob.add(floatArray[(int) (i * num + j)]);
                    System.out.println("porb is " + floatArray[(int) (i * num)] + " and " + floatArray[(int) (i * num + 1)]);
                }

        }

        return list_index_prob;
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
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
}
