package org.pytorch.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.madgaze.smartglass.otg.AudioCallback;
import com.madgaze.smartglass.otg.CameraHelper;
import com.madgaze.smartglass.otg.RecordVideoCallback;
//import com.madgaze.smartglass.otg.SplitCamera;
import com.madgaze.smartglass.otg.SplitCameraCallback;
import com.madgaze.smartglass.otg.TakePictureCallback;
import com.madgaze.smartglass.view.SplitCameraView;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.util.Util;
import org.pytorch.demo.vision.Helper.GraphicOverlay;
import org.pytorch.demo.vision.Helper.RectOverlay;
import org.pytorch.demo.vision.view.ResultRowView;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static java.lang.Math.max;
import static java.lang.Math.min;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class GlassLocalActivity extends AppCompatActivity {

    public static final String SCORES_FORMAT = "%.2f";
    private static final int TOP_K = 3 ;
    SplitCameraView mSplitCameraView;
    ImageView imageView;
    private Module mModule;
    private Module encoder;
    String[] RequiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public int count;
    private ArrayList<Utils.NamedBox> namedboxpool;
    private ArrayList<Utils.NamedEmbedding> namedEmbeddings;
    private GraphicOverlay graphicOverlay;
    private TextToSpeech mSpeech;


//    private boolean isPermissionGranted = false;
    private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];
//    private ImageView imageView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glass_local);
        mSplitCameraView = (SplitCameraView) findViewById(R.id.splitCameraView);
        imageView =findViewById(R.id.imageView2);
        imageView.bringToFront();
        namedboxpool = new ArrayList<>();
        namedEmbeddings = new ArrayList<>();
        update_embeddings(get_embeddings_from_files());
        graphicOverlay = findViewById(R.id.graphicOverlay2);
        graphicOverlay.bringToFront();
        count = 0;
        init();
        init_speech();
    }

    public void init_speech(){
        mSpeech = new TextToSpeech(this, new OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    int result = mSpeech.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("lanageTag", "not use");
                    } else {
//                        btn.setEnabled(true);
                        mSpeech.speak("good day today", TextToSpeech.QUEUE_FLUSH,
                                null);
                    }
                }
            }

        });
    }


    public void init(){
        setViews();

        if (!permissionReady()) {
            askForPermission();
        } else {
            setVideo();
            setAudio();
        }
    }


    public void askForPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, RequiredPermissions,100);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean permissionReady(){
        ArrayList<String> PermissionsMissing = new ArrayList();

        for (int i = 0; i < RequiredPermissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(this, RequiredPermissions[i]) != PackageManager.PERMISSION_GRANTED) {
                PermissionsMissing.add(RequiredPermissions[i]);
            }
        }
        if (PermissionsMissing.size() > 0){
            MDToast.makeText(GlassLocalActivity.this, String.format("Permission [%s] not allowed, please allows in the Settings.", String.join(", ", PermissionsMissing)), MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
            return false;
        }
        return true;
    }



    public void setViews(){
        mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row);
        mResultRowViews[0].bringToFront();
        mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row);
        mResultRowViews[1].bringToFront();
        mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row);
        mResultRowViews[2].bringToFront();
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(view.getId()){
                    case R.id.cameraOnOffBtn:
                        toggleCameraOnOffBtn();
                        break;
                    case R.id.takePictureBtn:
                        findViewById(R.id.takePictureBtn).setEnabled(false);
                        takePicture();
                        break;
                    case R.id.videoBtn:
                        findViewById(R.id.videoBtn).setEnabled(false);
                        toogleVideoBtn();
                        break;
                }
            }
        };

        (findViewById(R.id.cameraOnOffBtn)).setOnClickListener(listener);
        (findViewById(R.id.takePictureBtn)).setOnClickListener(listener);
        (findViewById(R.id.videoBtn)).setOnClickListener(listener);

        (findViewById(R.id.takePictureBtn)).setEnabled(false);
        (findViewById(R.id.videoBtn)).setEnabled(false);
    }


    protected void analyzeImage(Bitmap bitmap, int rotationDegrees) {

        Utils.NamedBox midbox = null;
        Bitmap bitmap2show;
        final long moduleForwardDuration, moduleAnalysisDuration;
        try {

            if (mModule == null) {
                try {
                    final String moduleFileAbsoluteFilePath = new File(
                            Utils.assetFilePath(this, "mobile_model2.pt")).getAbsolutePath();
                    mModule = Module.load(moduleFileAbsoluteFilePath);
                    final String encoderFileAbsoluteFilePath = new File(
                            Utils.assetFilePath(this, "encoder1.pt")).getAbsolutePath();
                    encoder = Module.load(encoderFileAbsoluteFilePath);
                }catch (Exception e)
                {e.printStackTrace();}
            }

            bitmap2show = bitmap;
            final long startTime = SystemClock.elapsedRealtime();

            float [] mean = new float[] {0.49804f, 0.49804f, 0.49804f};
            float [] std = new float[] {0.501960f, 0.501960f, 0.501960f};
            float nms_threshold = 0.4f;
            int w1 = bitmap.getWidth();
            int h1 = bitmap.getHeight();

            Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap,480,360,true);
//            if (bitmap.getWidth() > bitmap.getHeight())
//                bitmap = Bitmap.createScaledBitmap(bitmap,480,360,true);
//            else
//                bitmap = Bitmap.createScaledBitmap(bitmap,360,480,true);
            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std);
            System.out.println(Arrays.toString(inputTensor.shape()));
            //Normalize input
//        inputTensor = Normalize_tensor(inputTensor, 127, 128);
            // running the model
            final long moduleForwardStartTime = SystemClock.elapsedRealtime();
            final IValue[] output = mModule.forward(IValue.from(inputTensor)).toTuple();
            moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

//            final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();

            Tensor scores = output[0].toTensor();
            Tensor boxes = output[1].toTensor();
            float threshold = 0.8f;

            ArrayList<Float> possible_indexes = possible_score(scores, boxes, threshold);
            System.out.println("in onCreate len possible_indexes " + possible_indexes.size());

            ArrayList<float[]> nms_boxes = nms(boxes, scores, possible_indexes, nms_threshold);
            float ratio = 0.1f;
            nms_boxes = expand_box(nms_boxes, ratio);

            if (nms_boxes.size() > 0){

//
//                drawOnBitmap(bitmap, nms_boxes);

                int height = mSplitCameraView.getHeight();
                int width = mSplitCameraView.getWidth();
                System.out.println("in if width is "+width+" height is "+height);

                //TODO make http request here to get identity of picture
//                    send_unrecognized_image(bitmap, nms_boxes);
                get_unrecognized_face_embedding(bitmap, nms_boxes);
                set_namedboxpool_isvalid_false();
                midbox = drawFaceResults(nms_boxes, width, height);
//                set_prediction(bitmap, midbox);
                update_namedboxpool();

                System.out.println("nms boxes "+nms_boxes.size());

            }
            else
            {
                graphicOverlay.clear();
                update_namedboxpool();
            }
            moduleAnalysisDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;

            System.out.println("inference time is "+moduleForwardDuration);

        } catch (Exception e) {
            Log.e(Constants.TAG, "Error during image analysis", e);

            runOnUiThread(() -> {
                GlassLocalActivity.this.finish();
            });
            return;
        }
        if (midbox == null)
        {
            MDToast.makeText(GlassLocalActivity.this, "no midbox", MDToast.LENGTH_SHORT).show();
            return;
        }
        else
        {
//            String name = "null";
//            String career = "null";
//            String message = "null";
//            float prob = 1f;

            MyRunnable myRunnable = new MyRunnable();
            myRunnable.setData(midbox, bitmap);
            runOnUiThread(myRunnable);
//            try{
//                JSONObject jsonObject = new JSONObject(midbox.info);
//                name = jsonObject.getString("id");
//                career = jsonObject.getString("career");
//                message = jsonObject.getString("message");
//                prob = (float) jsonObject.getDouble("prob");
//                bitmap_c = cropBitmap(bitmap2show, midbox.rect);
//            }catch (JSONException jsonException) {jsonException.printStackTrace();}

        }
    }

    public class MyRunnable implements Runnable {
        private Utils.NamedBox data;
        private Bitmap bitmap;
        public void setData(Utils.NamedBox _data, Bitmap bitmap) {
            this.data = _data;
            this.bitmap = bitmap;
        }

        public void run() {
            updateUI(data, bitmap);
        }
    }

    private void updateUI(Utils.NamedBox namedBox, Bitmap bitmap){
        if (namedBox != null){

//            imageView.setImageBitmap(result.bitmap_c);
            mSpeech.speak(namedBox.info, TextToSpeech.QUEUE_FLUSH,
                    null);
            Bitmap bitmap_c = null;
            bitmap_c = cropBitmap(bitmap, namedBox.rect);
            imageView.setImageBitmap(bitmap_c);
            for (int i = 0; i < TOP_K; i++) {
                final ResultRowView rowView = mResultRowViews[i];
                rowView.nameTextView.setText(namedBox.id_k[i]);
                rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
                        namedBox.prob_k[i]));
                rowView.setProgressState(false);
            }
        }
    }
    private void set_namedboxpool_isvalid_false()
    {
        for (Utils.NamedBox namedBox: namedboxpool)
        {
            namedBox.is_valid = false;
        }
    }
    
    /*
     * 使用@param nms_box中的框截取@param bitmap，对于获得的人脸，计算embedding。
     *
     * */
    private void get_unrecognized_face_embedding(Bitmap bitmap, ArrayList<float[]> nms_boxes){
        for (int i = 0; i < nms_boxes.size(); i++)
        {
            float[] box_c = nms_boxes.get(i).clone();
            boolean flag = true;
//            for (NamedBox namedBox: namedboxpool)
//            {
//                if (IoU(box_c, namedBox.rect) > 0.5)
//                {
//                    flag = false;
//                }
//            }
            if (flag){
                Bitmap bitmap_c = cropBitmap(bitmap, box_c);
                if (bitmap_c == null)
                    continue;
                Utils.NamedEmbedding unnamed = encode_face(bitmap_c);
                find_topk_distance(unnamed, box_c, TOP_K);
            }
        }

    }
    private String get_embeddings_from_files(){
//        File Directory = getFilesDir();
        File[] files = new Util(this).GetLocalDatagramFiles();
        String embedding_str = "";
        if(files.length == 0)
            return "";
        for (File f: files){
            try{
                FileInputStream fileInputStream = new FileInputStream(f);
                int length = fileInputStream.available();
                byte bytes[] = new byte[length];
                fileInputStream.read(bytes);
                fileInputStream.close();
                String str =new String(bytes, StandardCharsets.UTF_8);
                embedding_str += str;

            }catch (FileNotFoundException fileNotFoundException){
                fileNotFoundException.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return embedding_str;
    }

    public void update_namedboxpool()
    {
        ArrayList<Utils.NamedBox> new_namedboxpool = new ArrayList<>();
        for (Utils.NamedBox nb : namedboxpool) {
            if (nb.is_valid)
                new_namedboxpool.add(nb);  //注意这个地方
        }
        namedboxpool = new_namedboxpool;
    }
    private Bitmap cropBitmap(Bitmap bitmap, float[] rect) {
        try{
            int x,y,w,h;
            x = (int) (bitmap.getWidth() * rect[0]);
            y = (int) (bitmap.getHeight() * rect[1]);

            w = (int) (bitmap.getWidth() * (rect[2]-rect[0]));
            h = (int) (bitmap.getHeight() * (rect[3]-rect[1]));

            return Bitmap.createBitmap(bitmap, x, y, w, h,null, false);
        }catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return null;

    }

    private void update_embeddings(String str){
        String[] strings = str.split(new Util(this).deliminator);
        for (String s : strings){
            if(s.length() > 100){
                namedEmbeddings.add(new Utils.NamedEmbedding(s));
            }
        }
        ArrayList<Utils.NamedEmbedding> namedEmbeddings1 = new ArrayList<>();
        for (Utils.NamedEmbedding namedEmbedding : namedEmbeddings){
            if (namedEmbedding.id == null)
                continue;
            else
                namedEmbeddings1.add(namedEmbedding);
        }
        namedEmbeddings = namedEmbeddings1;
        System.out.println("namedEmbeddings.size() "+namedEmbeddings.size());

    }

    /*
     * 使用@param unnamed 中的embedding和全局变量embeddings计算512维空间的欧式距离
     * 找到距离最近的@param topk个结果，将最近的结果打包成NamedBox放到全局变量namedboxpool中去用于画框。
     */
    private void find_topk_distance(Utils.NamedEmbedding unnamed, float[] box_c, int topk){
        int len = namedEmbeddings.size();
        float[] dists = new float[len];
        for(int i = 0; i < len; i++)
        {
            dists[i] = calc_dist(unnamed.embedding, namedEmbeddings.get(i).embedding);
        }
        int[] topk_index = get_min_topk(dists, topk);
        Utils.NamedBox namedBox;
        namedBox = new Utils.NamedBox();
        namedBox.id = namedEmbeddings.get(topk_index[0]).id;
        for(int i = 0; i < topk; i++){
            namedBox.id_k[i] = namedEmbeddings.get(topk_index[i]).id;
            namedBox.prob_k[i] = dists[topk_index[i]];
        }
        namedBox.rect = box_c;
        namedboxpool.add(namedBox);

    }
    /*
     * 计算@param x和@param y两个向量之间的距离平方
     * */
    private float calc_dist(float[] x, float[] y){
        float ret = 0;
        int len = x.length;
        for (int i = 0; i < len; i++){
            ret += Math.pow((x[i] - y[i]),2);
        }
        return ret;
    }

    /*
     * 将@param array中的最小的@param k个元素的下标返回@return int[k]
     * */
    public int[] get_min_topk(float[] array, int k) {
        if (array.length == 0){
            throw new ArrayIndexOutOfBoundsException("array len is 0");
        }
        float[] a = array.clone();
        int[] numbers = new int[array.length];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = i;
        }
        for (int i = 0; i < k; i++) {
            // 没有进行排序的）不断地与第i个相比，找到剩余中最小的，放在第i个
            int index = i;
            // find the minimum in the rest
            for (int j = i; j < a.length; j++) {
                if (a[index] > a[j]) {
                    index = j;  // 不断把找出的最小值的下标 赋值给index
                }
            }
//            a[i] = array[index]; // 最小值

            // change 得到正确下标数据的关键是，在交换的时候要把下标也换了
            float temp = a[i];
            a[i] = a[index];
            a[index] = temp;

            int temp2 = numbers[i];
            numbers[i] = numbers[index];
            numbers[index] = temp2;
        }
        int[] num_k = new int[k];
        System.arraycopy(numbers, 0, num_k, 0, k);
        return num_k;
    }



    private Utils.NamedEmbedding encode_face(Bitmap bitmap_c){
        final long startTime = SystemClock.elapsedRealtime();

        float [] mean = new float[] {0.49804f, 0.49804f, 0.49804f};
        float [] std = new float[] {0.501960f, 0.501960f, 0.501960f};

        Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap_c,160,160,true);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std);
        System.out.println(Arrays.toString(inputTensor.shape()));
        final long moduleForwardStartTime = SystemClock.elapsedRealtime();
        final Tensor outputTensor = encoder.forward(IValue.from(inputTensor)).toTensor();
//        final IValue[] output = encoder.forward(IValue.from(inputTensor)).toTuple();
        final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;
        System.out.println("output tensor shape is "+ Arrays.toString(outputTensor.shape()));
        Utils.NamedEmbedding unNamedEmbedding = new Utils.NamedEmbedding();

        // put encoded face into unNamedEmbedding
        float[] tensorDataAsFloatArray = outputTensor.getDataAsFloatArray();
        for (int i = 0; i< 512; i++)
            unNamedEmbedding.embedding[i] = tensorDataAsFloatArray[i];
        return unNamedEmbedding;

    }


    private ArrayList<float[]> expand_box(ArrayList<float[]> boxes, float ratio)
    {
        for (int i = 0; i < boxes.size(); i++)
        {
            float w = boxes.get(i)[2] - boxes.get(i)[0];
            float h = boxes.get(i)[3] - boxes.get(i)[1];

            if (boxes.get(i)[0] - w * ratio > 0)
                boxes.get(i)[0] -= w*ratio;
            if (boxes.get(i)[1] - h * ratio > 0)
                boxes.get(i)[1] -= h*ratio;
            if (boxes.get(i)[2] + w * ratio < 1)
                boxes.get(i)[2] += w*ratio;
            if (boxes.get(i)[3] + w * ratio < 1)
                boxes.get(i)[3] += w*ratio;
        }
        return boxes;
    }

    private double distance2middle(float[] box)
    {
        float midx = (box[0] + box[2]) / 2;
        float midy = (box[1] + box[3]) / 2;

        double dist = Math.pow((midx - 0.5), 2) + Math.pow((midy - 0.5), 2);
        return dist;
    }



    private Utils.NamedBox drawFaceResults_nbp(int width, int height){
        graphicOverlay.clear();
        System.out.println("in draw face results NBP");

        int color;
        double least_dist = 100;
        Utils.NamedBox least_index = null;
        for (Utils.NamedBox namedBox : namedboxpool){
            double dist = distance2middle(namedBox.rect);
            if (dist < least_dist)
            {
                least_index = namedBox;
                least_dist = dist;
            }
        }


        for (Utils.NamedBox namedBox : namedboxpool)
        {
            if (namedBox == least_index) {
                color = Color.BLUE;
            }
            else
                color = Color.RED;
            float[] xyxy = {namedBox.rect[0]*width,namedBox.rect[1]*height,namedBox.rect[2]*width,namedBox.rect[3]*height};
            Rect rect = new Rect((int)xyxy[0], (int)xyxy[1], (int)xyxy[2], (int)xyxy[3]);

            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, rect, namedBox.info, color);
            graphicOverlay.add(rectOverlay);
        }
        return least_index;
    }

    private Utils.NamedBox drawFaceResults(ArrayList<float[]> nms_boxes, int width, int height) {
        int counter =0;
        graphicOverlay.clear();
        System.out.println("in draw face results");
        Utils.NamedBox ret_named_box = null;
        //find index of box nearest to middle, its color will be blue instead of red

        double least_dist = 100;
        int least_index = 0;
        for (int i = 0; i<nms_boxes.size(); i++){
            double dist = distance2middle(nms_boxes.get(i));
            if (dist < least_dist)
            {
                least_index = i;
                least_dist = dist;
            }
        }


        int color = Color.RED;
        for (int i = 0; i<nms_boxes.size(); i++){
            float[] xyxy1 = nms_boxes.get(i);
            String info = null;


            for (Utils.NamedBox namedBox : namedboxpool)
            {
                if (IoU(xyxy1, namedBox.rect) > 0.5)
                {
                    info = namedBox.id;
                    namedBox.is_valid = true;

                    if (i == least_index) {
                        ret_named_box = namedBox;

                    }

                    break;
                }
            }

            if (i == least_index) {
                color = Color.BLUE;

            }
            else
                color = Color.RED;


            float[] xyxy = {xyxy1[0]*width,xyxy1[1]*height,xyxy1[2]*width,xyxy1[3]*height};
            Rect rect = new Rect((int)xyxy[0], (int)xyxy[1], (int)xyxy[2], (int)xyxy[3]);

            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, rect, info, color);
            graphicOverlay.add(rectOverlay);

//            drawImageView.setRectF(rect);
//            drawImageView.postInvalidate();
            counter = counter +1;
        }
        return ret_named_box;
//        graphicOverlay.postInvalidate();

    }

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


    public void setVideo(){
        MySplitCamera.getInstance(this).setFrameFormat(CameraHelper.FRAME_FORMAT_MJPEG);
        MySplitCamera.getInstance(this).setPreviewSize(MySplitCamera.CameraDimension.DIMENSION_1280_720);
        MySplitCamera.getInstance(this).start(findViewById(R.id.splitCameraView));

        //////

        /* Insert code segment below if you want to monitor the USB Camera connection status */
        MySplitCamera.getInstance(this).setCameraCallback(new SplitCameraCallback() {
            @Override
            public void onConnected() {
                MySplitCamera.getInstance(GlassLocalActivity.this).startPreview();
                MDToast.makeText(GlassLocalActivity.this, "Camera connected", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                updateUI(true);
            }

            @Override
            public void onDisconnected() {
                MDToast.makeText(GlassLocalActivity.this, "Camera disconnected", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
                updateUI(false);
            }

            @Override
            public void onError(int code) {
                if (code == -1)
                    MDToast.makeText(GlassLocalActivity.this, "There is no connecting MAD Gaze Cameras.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                else
                    MDToast.makeText(GlassLocalActivity.this, "MAD Gaze Camera Init Failure (Error=" + code + ")", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                updateUI(false);
            }
        });

        //////

        /* Insert code segment below if you want to retrieve the video frames in nv21 format */
        MySplitCamera.getInstance(this).setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21bytearray) {
                int width = 1280;
                int height = 720;
                YuvImage yuvImage = new YuvImage(nv21bytearray, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
                byte[] jpegByteArray = os.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        analyzeImage(bitmap,0);
//                    }
//                });//卡死啦
                analyzeImage(bitmap, 0);

            }
        });

        //////

        /* Insert code segment below if you want to record the video */
        MySplitCamera.getInstance(this).setRecordVideoCallback(new RecordVideoCallback() {
            @Override
            public void onVideoSaved(String path) {
                MDToast.makeText(GlassLocalActivity.this, "Video saved success in (" + path + "), and count is "+count, MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((Button)findViewById(R.id.videoBtn)).setText("START");
                        ((findViewById(R.id.videoBtn))).setEnabled(true);
                    }
                });
            }

            @Override
            public void onError(int code) {
                MDToast.makeText(GlassLocalActivity.this, "Video saved (Error=" + code +")", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((Button)findViewById(R.id.videoBtn)).setText("START");
                        ((findViewById(R.id.videoBtn))).setEnabled(true);
                    }
                });
            }
        });
    }

    public void setAudio(){
        MySplitCamera.getInstance(this).setAudioSamFreq(MySplitCamera.SamFreq.SamFreq_48000);//default 48000

        /* Insert code segment below if you want to retrieve the audio data */
        MySplitCamera.getInstance(this).setAudioCallback(                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           new AudioCallback() {
            @Override
            public void onAudioReceived(byte[] decodedAudio) {
                //decodedAudio is audio byte data.
            }
        });
    }

    public void toggleCameraOnOffBtn(){
        if (MySplitCamera.getInstance(GlassLocalActivity.this).isPreviewStarted()) {
            updateUI(false);
            MySplitCamera.getInstance(GlassLocalActivity.this).stopPreview();
        } else {
            updateUI(true);
            MySplitCamera.getInstance(GlassLocalActivity.this).startPreview();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void toogleVideoBtn(){
        if (!permissionReady()) return;
        if (MySplitCamera.getInstance(this).isRecording()) {
            MDToast.makeText(this, "Stop Recording", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
            MySplitCamera.getInstance(this).stopRecording();

        } else {
            MDToast.makeText(this, "Start Recording", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
            MySplitCamera.getInstance(this).startRecording();
            ((Button)(findViewById(R.id.videoBtn))).setText("STOP VIDEO");
            ((findViewById(R.id.videoBtn))).setEnabled(true);

        }
    }

    public void takePicture() {
        if (!permissionReady()) return;
        MySplitCamera.getInstance(this).takePicture(new TakePictureCallback() {
            @Override
            public void onImageSaved(String path) {
                MDToast.makeText(GlassLocalActivity.this, "Image saved success in (" + path + "), and count is "+count, MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                ((findViewById(R.id.takePictureBtn))).setEnabled(true);
            }

            @Override
            public void onError(int code) {
                MDToast.makeText(GlassLocalActivity.this, "Image saved failure (Error="+code+")", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                ((findViewById(R.id.takePictureBtn))).setEnabled(true);
            }
        });
    }

    public void updateUI(boolean on){
        if (on){
            (findViewById(R.id.cameraOnOffBtn)).setEnabled(true);
            ((Button)findViewById(R.id.cameraOnOffBtn)).setText("STOP");
            (findViewById(R.id.videoBtn)).setEnabled(true);
            (findViewById(R.id.takePictureBtn)).setEnabled(true);

        } else {
            (findViewById(R.id.cameraOnOffBtn)).setEnabled(true);
            ((Button)findViewById(R.id.cameraOnOffBtn)).setText("START");
            (findViewById(R.id.videoBtn)).setEnabled(false);
            (findViewById(R.id.takePictureBtn)).setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MySplitCamera.getInstance(this).onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MySplitCamera.getInstance(this).onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MySplitCamera.getInstance(this).onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MySplitCamera.getInstance(this).onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MySplitCamera.getInstance(this).onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (!permissionReady()) {
                askForPermission();
            } else {
                setVideo();
                setAudio();
            }
        }
    }

}
