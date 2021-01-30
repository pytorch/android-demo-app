package org.pytorch.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.Constants;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.YuvToRgbConverter;

import org.pytorch.demo.util.Util;
import org.pytorch.demo.vision.AbstractCameraXActivity;
import org.pytorch.demo.vision.Helper.GraphicOverlay;
import org.pytorch.demo.vision.Helper.DrawImageView;
import org.pytorch.demo.vision.Helper.RectOverlay;
import org.pytorch.demo.vision.view.ResultRowView;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCapture;
import androidx.core.app.ActivityCompat;


import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocket;
import org.json.*;

public class FaceDetectionActivity extends AbstractCameraXActivity<FaceDetectionActivity.AnalysisResult> {

    public static final String INTENT_MODULE_ASSET_NAME = "INTENT_MODULE_ASSET_NAME";
    public static final String INTENT_INFO_VIEW_TYPE = "INTENT_INFO_VIEW_TYPE";

    private static final int INPUT_TENSOR_WIDTH = 360;
    private static final int INPUT_TENSOR_HEIGHT = 480;
    private static final int TOP_K = 3;
    private static final int MOVING_AVG_PERIOD = 10;
    private static final String FORMAT_MS = "%dms";
    private static final String FORMAT_AVG_MS = "avg:%.0fms";
    private GraphicOverlay graphicOverlay;
    private DrawImageView drawImageView;
    private ViewStub viewStub;
    private static final String FORMAT_FPS = "%.1fFPS";
    public static final String SCORES_FORMAT = "%.2f";
    private WebSocketFactory webSocketFactory;
//    private Button local_remote_switcher;
    private Button record;


    static class AnalysisResult {

        private final String[] topNClassNames;
        private final float[] topNScores;
        private final long analysisDuration;
        private final long moduleForwardDuration;
        private final Bitmap bitmap_c;


        public AnalysisResult(String[] topNClassNames, float[] topNScores, Bitmap bitmap_c,
                              long moduleForwardDuration, long analysisDuration) {
            this.topNClassNames = topNClassNames;
            this.topNScores = topNScores;
            this.moduleForwardDuration = moduleForwardDuration;
            this.analysisDuration = analysisDuration;
            this.bitmap_c = bitmap_c;
        }
    }

    private boolean mAnalyzeImageErrorState;
    private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];
    private TextView mFpsText;
    private TextView mMsText;
    private TextView mMsAvgText;
    private Module mModule;
    private Module encoder;
    private String mModuleAssetName;
    private FloatBuffer mInputTensorBuffer;
    private Tensor mInputTensor;
    private long mMovingAvgSum = 0;
    private Queue<Long> mMovingAvgQueue = new LinkedList<>();
    private Bitmap bitmap_c = null;
    private float[] box_c = null;

    private ImageView imageView;

    private ArrayList<NamedBox> namedboxpool;
    private ArrayList<NamedEmbedding> namedEmbeddings;
    private int detect_mode;
    private final int LOCAL = 0;
    private final int REMOTE = 1;
    private BitmapToVideoEncoder bitmapToVideoEncoder = null;
    private int video_height = 1280;
    private int video_width = 960;
    private String record_mode = null;
//    private String deliminator = "\\$\\$\\$\\$\\$\\$\\$\\$\\$\\$";
    public class NamedEmbedding{
        public float[] embedding;
        public String id;

        NamedEmbedding(String jsonString){
            if (jsonString.length() < 10)
                return;
            try{
                jsonString = jsonString.replace("\\","");
                System.out.println("In NamedEmbedding json str is " + jsonString);
                JSONObject jsonObject = new JSONObject(jsonString);
                this.id = jsonObject.getString("name");
                JSONArray jsonArray = jsonObject.getJSONArray("embedding");
                this.embedding = new float[512];
                for(int i = 0; i < 512; i++)
                {
                    this.embedding[i] = (float) jsonArray.getDouble(i);
                }
            }catch (JSONException jsonException)
            {
                jsonException.printStackTrace();
                this.embedding = null;
                this.id = null;
            }


        }

        NamedEmbedding(){
            embedding = new float[512];
            id = null;
        }
    }

//    private void get_embeddings(){
//        webSocket.sendText("get embeddings");
//    }
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
    private void update_embeddings(String str){
        String[] strings = str.split(new Util(this).deliminator);
        for (String s : strings){
            if(s.length() > 100){
                namedEmbeddings.add(new NamedEmbedding(s));
            }
        }
        ArrayList<NamedEmbedding> namedEmbeddings1 = new ArrayList<>();
        for (NamedEmbedding namedEmbedding : namedEmbeddings){
            if (namedEmbedding.id == null)
                continue;
            else
                namedEmbeddings1.add(namedEmbedding);
        }
        namedEmbeddings = namedEmbeddings1;
        System.out.println("namedEmbeddings.size() "+namedEmbeddings.size());

    }


    public class NamedBox{

        public float[] rect;
        public String[] id_k;
        public float[] prob_k;
        public String id;

        public String info;
        public boolean is_valid;
        NamedBox(){
            id_k = new String[TOP_K];
            prob_k = new float[TOP_K];
            rect = new float[4];
        }
        NamedBox(String info)
        {
            try {
                rect = new float[]{0,0,0,0};

                JSONObject jsonObject = new JSONObject(info);

                JSONArray jsonArray = jsonObject.getJSONArray("box");
                JSONArray jsonArray1 = jsonArray.getJSONArray(0);

                JSONArray jsonArray2 = jsonObject.getJSONArray("id");
                info = jsonArray2.getString(0);
                System.out.println("in namedbox info is " + info);
                for (int i = 0; i < jsonArray1.length(); i++)
                {
                    rect[i] = Float.parseFloat(jsonArray1.getString(i));
                }
                this.info = info;
                this.is_valid = true;
            }catch (JSONException exceptione)
            {
                exceptione.printStackTrace();
                this.info = null;
                this.rect = null;
                this.is_valid=false;
            }
        }
        NamedBox(String id, String[] id_k, float[] prob, float[] box, String info){
            this.id = id;
            this.id_k = id_k;
            this.prob_k = prob;
            this.rect = box;
            this.info = info;
        }
    }


    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_face_detection;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        return ((ViewStub) findViewById(R.id.image_classification_texture_view_stub))
                .inflate()
                .findViewById(R.id.image_classification_texture_view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ResultRowView headerResultRowView =
                findViewById(R.id.image_classification_result_header_row);
        headerResultRowView.nameTextView.setText(R.string.face_of_attention);
        headerResultRowView.scoreTextView.setText(R.string.prediction);
        imageView = findViewById(R.id.imageView);
//        detect_mode = LOCAL;
//        local_remote_switcher = findViewById(R.id.local_remote_detect_switcher);
//        local_remote_switcher.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view){
//                // Received a text message.
//                System.out.println("in onclick listener");
//                String mode;
//
//                if (detect_mode == REMOTE) {
//                    detect_mode = LOCAL;
//                    mode = "LOCAL";
//
//                }
//                else{
//                    detect_mode = REMOTE;
//                    mode = "REMOTE";
//                }
//                local_remote_switcher.setText(mode);
//
//                Toast.makeText(FaceDetectionActivity.this, "detect mode changed to " + mode, Toast.LENGTH_SHORT).show();
//
//
//            }
//        });
        record = findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
//            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view){

                System.out.println("in onclick record listener");

                //VideoCapture NOT SUPPORTED by current cameraX, wait till further implementation

//                // alternative
//                if ("recording".equals(record_mode)) {
//                    stopRecord();
//                    Toast.makeText(FaceDetectionActivity.this, "stop recording", Toast.LENGTH_SHORT).show();
//                    record_mode = null;
//                }
//                else{
//
//                    File sdDir = Environment.getExternalStorageDirectory();
//                    File video = new File(sdDir, "testcamerax.mp4");
//                    startRecord(video, FaceDetectionActivity.this);
//                    Toast.makeText(FaceDetectionActivity.this, "start recording", Toast.LENGTH_SHORT).show();
//                    record_mode = "recording";
//                }
//




                if ("recording".equals(record_mode)) {
                    bitmapToVideoEncoder.stopEncoding();
//                    bitmapToVideoEncoder = null;
                    record_mode = null;
                }
                else{
                    bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
                        @Override
                        public void onEncodingComplete(File outputFile) {
//                            Toast.makeText(FaceDetectionActivity.this, "Encoding complete!", Toast.LENGTH_LONG).show();
                            Log.d("in listener", "stopped recording");
                        }
                    });
                    File sdDir = Environment.getExternalStorageDirectory();
                    File video = new File(sdDir, "testpath.mp4");
                    bitmapToVideoEncoder.startEncoding(video_width, video_height, video);

                    Toast.makeText(FaceDetectionActivity.this, "start recording", Toast.LENGTH_SHORT).show();
                    record_mode = "recording";

                }

            }
        });
        namedboxpool = new ArrayList<>();
        namedEmbeddings = new ArrayList<>();
//        webSocketFactory = new WebSocketFactory();
////        WebSocket webSocket;
//        try{
//            webSocket=webSocketFactory.createSocket(serverUri);
//            // Android 4.0 之后不能在主线程中请求HTTP请求
////            new Thread(new Runnable(){
////                @Override
////                public void run() {
////                    try{
////                        webSocket.connect();
////                    }catch (WebSocketException exception)
////                    {
////                        exception.printStackTrace();
////                    }
////
////                }
////            }).start();
//            // force main thread permitting network
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//
//            webSocket.connect();
//            webSocket.addListener(new WebSocketAdapter() {
//                @Override
//                public void onTextMessage(WebSocket websocket, String message) throws Exception {
//                    // Received a text message.
//                    System.out.println("in listener, text message received "+ message);
//
////                    System.out.println(message);
//                    //TODO info can be retrived here
//                    // need further operation
//                    // put box and info into boxpool
//
//                    if (message != null)
//                    {
////                        NamedBox namedBox = new NamedBox(message);
////                        namedboxpool.add(namedBox);
//                        update_embeddings(message);
//                    }
//                    else
//                        System.out.println("in onTextMessage namedbox is null");
//
//                }
//            });
//            webSocket.addListener(new WebSocketAdapter(){
//                @Override
//                public void onBinaryMessage(WebSocket webSocket, byte[] bytes) throws Exception{
//                    System.out.println("in binary message listener received bytes of size " + bytes.length);
//                }
//
//            });
////            webSocket.sendText("String websocket");
//        }catch (IOException ioe)
//        {
//            System.out.println(ioe.toString());
//        }
//        catch (OpeningHandshakeException e)
//        {
//            // A violation against the WebSocket protocol was detected
//            // during the opening handshake.
//        }
//        catch (HostnameUnverifiedException e)
//        {
//            // The certificate of the peer does not match the expected hostname.
//        }
//        catch (WebSocketException e)
//        {
//            // Failed to establish a WebSocket connection.
//        }

        //get_embeddings from server
//        get_embeddings();
        update_embeddings(get_embeddings_from_files());
        graphicOverlay = findViewById(R.id.graphic_overlay);
        graphicOverlay.bringToFront();

        viewStub = (ViewStub) findViewById(R.id.image_classification_texture_view_stub);
//        drawImageView = findViewById(R.id.drawImageView);
//        drawImageView.bringToFront();
        mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row);
        mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row);
        mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row);

        mFpsText = findViewById(R.id.image_classification_fps_text);
        mMsText = findViewById(R.id.image_classification_ms_text);
        mMsAvgText = findViewById(R.id.image_classification_ms_avg_text);

    }

//    public void send_croped_Image(Bitmap bitmap_c, float[] box_c)
//    {
//        System.out.println("in send cropped Image");
////                webSocket.sendText("{\"message\": \"on create\"}");
//        if (bitmap_c != null)
//        {
//            try {
//                String box = "["+box_c[0]+","+box_c[1]+","+box_c[2]+","+box_c[3]+ "]";
//                String string = "{\"message\": \"cropped image\",\"box\": "+ box +"}";
//                String deliminater = "$$$$$$$$$$";
//                string = string + deliminater;
//                byte[] bytes1 = string.getBytes("UTF-8");
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                bitmap_c.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                byte[] bytes = baos.toByteArray();
//                System.out.println("in onclick bytes len is " + bytes.length);
//                webSocket.sendBinary(byteMerger(bytes1, bytes));
//            }catch (java.io.UnsupportedEncodingException uee)
//            {
//                uee.printStackTrace();
//            }
//
//        }
//    }


    public void update_namedboxpool()
    {
        ArrayList<NamedBox> new_namedboxpool = new ArrayList<>();
        for (NamedBox nb : namedboxpool) {
            if (nb.is_valid)
                new_namedboxpool.add(nb);  //注意这个地方
        }
        namedboxpool = new_namedboxpool;
    }

    public byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }
//
//    @Override
//    public void onRequestPermissionsResult(
//            int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == 2077) {
//            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't use image classification example without granting INTERNET permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//            else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't store video without granting Write external storage permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//            else if (grantResults[2] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't record video without granting camera permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//        }
//
//    }

//
//    private void output(final String txt) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                output.setText(output.getText().toString() + "\n\n" + txt);
//                //TODO change text of boxes here
//            }
//        });
//    }



    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        mMovingAvgSum += result.moduleForwardDuration;
        mMovingAvgQueue.add(result.moduleForwardDuration);
        if (mMovingAvgQueue.size() > MOVING_AVG_PERIOD) {
            mMovingAvgSum -= mMovingAvgQueue.remove();
        }
        if (result.bitmap_c != null){
            imageView.setImageBitmap(result.bitmap_c);

            for (int i = 0; i < TOP_K; i++) {
                final ResultRowView rowView = mResultRowViews[i];
                rowView.nameTextView.setText(result.topNClassNames[i]);
                rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
                        result.topNScores[i]));
                rowView.setProgressState(false);
            }
        }

        mMsText.setText(String.format(Locale.US, FORMAT_MS, result.moduleForwardDuration));
        if (mMsText.getVisibility() != View.VISIBLE) {
            mMsText.setVisibility(View.VISIBLE);
        }
        mFpsText.setText(String.format(Locale.US, FORMAT_FPS, (1000.f / result.analysisDuration)));
        if (mFpsText.getVisibility() != View.VISIBLE) {
            mFpsText.setVisibility(View.VISIBLE);
        }

        if (mMovingAvgQueue.size() == MOVING_AVG_PERIOD) {
            float avgMs = (float) mMovingAvgSum / MOVING_AVG_PERIOD;
            mMsAvgText.setText(String.format(Locale.US, FORMAT_AVG_MS, avgMs));
            if (mMsAvgText.getVisibility() != View.VISIBLE) {
                mMsAvgText.setVisibility(View.VISIBLE);
            }
        }
    }

    protected String getModuleAssetName() {
        if (!TextUtils.isEmpty(mModuleAssetName)) {
            return mModuleAssetName;
        }
        final String moduleAssetNameFromIntent = getIntent().getStringExtra(INTENT_MODULE_ASSET_NAME);
        mModuleAssetName = !TextUtils.isEmpty(moduleAssetNameFromIntent)
                ? moduleAssetNameFromIntent
                : "mobile_model2.pt";

        return mModuleAssetName;
    }

    @Override
    protected String getInfoViewAdditionalText() {
        return getModuleAssetName();
    }

    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 裁剪
     *
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
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

    private Bitmap drawOnBitmap(Bitmap bitmap, ArrayList<float[]> nms_boxes) {
        Canvas canvas = new Canvas(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);//不填充
        paint.setStrokeWidth(1);  //线的宽度
        int left,top,right,bottom;
        for (int i = 0; i < nms_boxes.size(); i++) {
            left = (int) (nms_boxes.get(i)[0] * width);
            top = (int) (nms_boxes.get(i)[1] * height);
            right = (int) (nms_boxes.get(i)[2] * width);
            bottom = (int) (nms_boxes.get(i)[3] * height);

            canvas.drawRect(left, top, right, bottom, paint);
        }
        return bitmap;
    }

//    private void sendByOKHttp() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder().url("https://www.baidu.com").build();
//                try {
//                    Response response = client.newCall(request).execute();//发送请求
//                    String result = response.body().string();
//                    Log.d("tag in send ok http ", "result: " + result);
//                    System.out.println("in run result is ");
//                    System.out.println(result);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
    @Override
    @WorkerThread
    @Nullable
    protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (mAnalyzeImageErrorState) {
            return null;
        }

        NamedBox midbox = null;
        Bitmap bitmap2show = null;
        long moduleForwardDuration = 0;
        long moduleAnalysisDuration = 0;
        try {
            Bitmap bitmap;
            System.out.println("in analyze original image size is w "+image.getWidth() + "  h "+image.getHeight());
            bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            YuvToRgbConverter yuvToRgbConverter = new YuvToRgbConverter(this);
            yuvToRgbConverter.yuvToRgb(image.getImage(), bitmap);
            bitmap = rotateBitmap(bitmap, rotationDegrees);

            if (detect_mode == LOCAL)
            {
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
                    int width = graphicOverlay.getWidth();
                    int height = (int) (width * h1/(1.0f * w1));
                    System.out.println("in if width is "+width+" height is "+height);

                    //TODO make http request here to get identity of picture
//                    send_unrecognized_image(bitmap, nms_boxes);
                    get_unrecognized_face_embedding(bitmap, nms_boxes);
                    set_namedboxpool_isvalid_false();
                    midbox = drawFaceResults(nms_boxes, width, height-100);
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
            }
//            else //remote detect: send whole image to server
//            {
//
//                long moduleForwardStartTime = SystemClock.elapsedRealtime();
////                send_whole_image(bitmap);
//                set_namedboxpool_isvalid_false();
//                update_namedboxpool();
//                moduleForwardDuration = moduleAnalysisDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;
//                bitmap2show = bitmap;
//
//                int width = graphicOverlay.getWidth();
//                int w1 = bitmap.getWidth();
//                int h1 = bitmap.getHeight();
//                int height = (int) (width * h1/(1.0f * w1));
//                midbox = drawFaceResults_nbp(width, height-100);
////                set_prediction(bitmap, midbox);
//                if (bitmapToVideoEncoder != null) {
//                    bitmapToVideoEncoder.queueFrame(bitmap);
//                }
//            }


//            Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//            Canvas canvas = new Canvas(bmp);
//            Paint paint = new Paint();
//            paint.setColor(Color.BLUE);
//            paint.setAlpha(100);
//            for(int i = 0; i < nms_boxes.size(); i++)
//            {
//                float[] xyxy = {nms_boxes.get(i)[0]*bmp.getWidth(),nms_boxes.get(i)[1]*bmp.getHeight(),nms_boxes.get(i)[2]*bmp.getWidth(), nms_boxes.get(i)[3]*bmp.getHeight()};
//                canvas.drawRect(xyxy[0], xyxy[1], xyxy[2], xyxy[3], paint);
//            }
//
//            ImageView imageView = findViewById(R.id.image);
//            imageView.setImageBitmap(bmp);

//            final float[] scores = outputTensor.getDataAsFloatArray();
//            final int[] ixs = Utils.topK(scores, TOP_K);
//
//            final String[] topKClassNames = new String[TOP_K];
//            final float[] topKScores = new float[TOP_K];
//            for (int i = 0; i < TOP_K; i++) {
//                final int ix = ixs[i];
//                topKClassNames[i] = Constants.IMAGENET_CLASSES[ix];
//                topKScores[i] = scores[ix];
//            }
//            final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
//            return new AnalysisResult(topKClassNames, topKScores, moduleForwardDuration, analysisDuration);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error during image analysis", e);
            mAnalyzeImageErrorState = true;
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    showErrorDialog(v -> FaceDetectionActivity.this.finish());
                }
            });
            return null;
        }
        if (midbox == null)
        {
            return new AnalysisResult(new String[] {"NO INFO", "NO INFO", "NO INFO"}, new float[] {1f,1f,1f}, null,
                    moduleForwardDuration, moduleAnalysisDuration) ;
        }
        else
        {
//            String name = "null";
//            String career = "null";
//            String message = "null";
//            float prob = 1f;
            Bitmap bitmap_c = null;
            bitmap_c = cropBitmap(bitmap2show, midbox.rect);
//            try{
//                JSONObject jsonObject = new JSONObject(midbox.info);
//                name = jsonObject.getString("id");
//                career = jsonObject.getString("career");
//                message = jsonObject.getString("message");
//                prob = (float) jsonObject.getDouble("prob");
//                bitmap_c = cropBitmap(bitmap2show, midbox.rect);
//            }catch (JSONException jsonException) {jsonException.printStackTrace();}

            return new AnalysisResult(new String[] {midbox.id_k[0], midbox.id_k[1], midbox.id_k[2]}, new float[] {midbox.prob_k[0],midbox.prob_k[1],midbox.prob_k[2]}, bitmap_c,
                    moduleForwardDuration, moduleAnalysisDuration) ;

        }
    }

//    private void send_whole_image(Bitmap bitmap){
//        System.out.println("in send whole Image");
////                webSocket.sendText("{\"message\": \"on create\"}");
//        if (bitmap != null)
//        {
//            try {
//                String string = "{\"message\": \"whole image\",\"box\": 0}";
//                String deliminater = "$$$$$$$$$$";
//                string = string + deliminater;
//                byte[] bytes1 = string.getBytes("UTF-8");
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                byte[] bytes = baos.toByteArray();
//                System.out.println("in onclick bytes len is " + bytes.length);
//                webSocket.sendBinary(byteMerger(bytes1, bytes));
//            }catch (java.io.UnsupportedEncodingException uee)
//            {
//                uee.printStackTrace();
//            }
//
//        }
//    }

    private void set_prediction(Bitmap bitmap, NamedBox nb){
        if (nb == null)
            return;

        String name = "null";
        float prob = 1f;
        try{
            JSONObject jsonObject = new JSONObject(nb.info);
            name = jsonObject.getString("id");
            prob = (float) jsonObject.getDouble("prob");
        }catch (JSONException jsonException) {jsonException.printStackTrace();}
        mResultRowViews[0].nameTextView.setText(name);
        mResultRowViews[0].scoreTextView.setText(Float.toString(prob));
        mResultRowViews[0].setProgressState(false);

//
//        for (int i = 0; i < TOP_K; i++) {
//            final ResultRowView rowView = mResultRowViews[i];
//            rowView.nameTextView.setText(result.topNClassNames[i]);
//            rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
//                    result.topNScores[i]));
//            rowView.setProgressState(false);
//        }
    }

    private void set_namedboxpool_isvalid_false()
    {
        for (NamedBox namedBox: namedboxpool)
        {
            namedBox.is_valid = false;
        }
    }

//    private void send_unrecognized_image(Bitmap bitmap, ArrayList<float[]> nms_boxes){
//        for (int i = 0; i < nms_boxes.size(); i++)
//        {
//            float[] box_c = nms_boxes.get(i).clone();
//            boolean flag = true;
//            for (NamedBox namedBox: namedboxpool)
//            {
//                if (IoU(box_c, namedBox.rect) > 0.5)
//                {
//                    flag = false;
//                }
//            }
//            if (flag){
//                Bitmap bitmap_c = cropBitmap(bitmap, box_c);
//                send_croped_Image(bitmap_c, box_c);
//            }
//        }
//
//    }

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
                NamedEmbedding unnamed = encode_face(bitmap_c);
                find_topk_distance(unnamed, box_c, TOP_K);
            }
        }

    }

    /*
     * 使用@param unnamed 中的embedding和全局变量embeddings计算512维空间的欧式距离
     * 找到距离最近的@param topk个结果，将最近的结果打包成NamedBox放到全局变量namedboxpool中去用于画框。
     */
    private void find_topk_distance(NamedEmbedding unnamed, float[] box_c, int topk){
        int len = namedEmbeddings.size();
        float[] dists = new float[len];
        for(int i = 0; i < len; i++)
        {
            dists[i] = calc_dist(unnamed.embedding, namedEmbeddings.get(i).embedding);
        }
        int[] topk_index = get_min_topk(dists, topk);
        NamedBox namedBox = new NamedBox();
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



    private NamedEmbedding encode_face(Bitmap bitmap_c){
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
        NamedEmbedding unNamedEmbedding = new NamedEmbedding();

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



    private NamedBox drawFaceResults_nbp(int width, int height){
        graphicOverlay.clear();
        System.out.println("in draw face results NBP");

        int color;
        double least_dist = 100;
        NamedBox least_index = null;
        for (NamedBox namedBox : namedboxpool){
            double dist = distance2middle(namedBox.rect);
            if (dist < least_dist)
            {
                least_index = namedBox;
                least_dist = dist;
            }
        }


        for (NamedBox namedBox : namedboxpool)
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

    private NamedBox drawFaceResults( ArrayList<float[]> nms_boxes, int width, int height) {
        int counter =0;
        graphicOverlay.clear();
        System.out.println("in draw face results");
        NamedBox ret_named_box = null;
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
            

            for (NamedBox namedBox : namedboxpool)
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


    @Override
    protected int getInfoViewCode() {
        return getIntent().getIntExtra(INTENT_INFO_VIEW_TYPE, -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mModule != null) {
            mModule.destroy();
        }
    }
}
