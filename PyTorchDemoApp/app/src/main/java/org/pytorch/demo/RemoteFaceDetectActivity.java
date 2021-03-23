package org.pytorch.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

//import android.support.v7.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.faucamp.simplertmp_1.FaucampRtmpHandler;
import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.valdesekamdem.library.mdtoast.MDToast;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.demo.util.Util;
import org.pytorch.demo.vision.Helper.GraphicOverlay;
import org.pytorch.demo.vision.Helper.RectOverlay;
import org.pytorch.demo.vision.view.ResultRowView;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Locale;


public class RemoteFaceDetectActivity extends AppCompatActivity implements FaucampRtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {
    private static final String TAG = "Yasea";
    public final static int RC_CAMERA = 100;

    public static final String SCORES_FORMAT = "%.2f";
    private Button btnPublish;
    private ImageView btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;
    private Button btnDetect;

    private String rtmpUrl;
    private String recPath;
    private String serverUri;
    private WebSocket webSocket;
    private SrsPublisher mPublisher;
    private ArrayList<NamedBox> namedboxpool;
    private SrsCameraView mCameraView;

    private int mWidth = 1080;
    private int mHeight = 2220;
    private int TOP_K = Utils.TOP_K;
    private boolean isPermissionGranted = false;
    private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];
    private ImageView imageView;

    private WebSocketFactory webSocketFactory;

    //mycode
    private GraphicOverlay graphicOverlay;

    private class NamedBox{

        public float[] rect;
        public String[] id_k;
        public float[] prob_k;
        public String id;

        public String info;
        public boolean is_valid;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("in on create");
        server_state_ready = false;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_remote_face_detect);

        // response screen rotation event
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        //init parameters
        Util util = new Util();
        rtmpUrl = util.getRTMPURL();
        serverUri = util.getWebsocket_TEMPLATE();
        recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";

        requestPermission_rfda();
    }

    private void requestPermission_rfda() {
        //1. 检查是否已经有该权限
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {
            //2. 权限没有开启，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_CAMERA);
        }else{
            //权限已经开启，做相应事情
            isPermissionGranted = true;
            System.out.println("in request permission before init");
            init();
        }
    }

    //3. 接收申请成功或者失败回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被用户同意,做相应的事情
                isPermissionGranted = true;
                init();
            } else {
                //权限被用户拒绝，做相应的事情
                finish();
            }
        }
    }

    private boolean server_state_ready;

    private void init() {
        // restore data.
//        sp = getSharedPreferences("FaceDetection", MODE_PRIVATE);
//        rtmpUrl = sp.getString("rtmpUrl", rtmpUrl);
        System.out.println("in init");
        // initialize url.
        final EditText efu = (EditText) findViewById(R.id.url);
        efu.setText(rtmpUrl);

        btnDetect = (Button) findViewById(R.id.detect);
        btnDetect.setText("检测");
        btnPublish = (Button) findViewById(R.id.publish);
        btnSwitchCamera = (ImageView) findViewById(R.id.swCam);
        btnSwitchCamera.bringToFront();
//        btnRecord = (Button) findViewById(R.id.record_yasea);
//        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);
//        btnPause = (Button) findViewById(R.id.pause);
//        btnPause.setEnabled(false);

        graphicOverlay = findViewById(R.id.graphicOverlay);
        graphicOverlay.bringToFront();
        imageView = findViewById(R.id.imageView);
        imageView.bringToFront();
        mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row);
        mResultRowViews[0].bringToFront();
        mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row);
        mResultRowViews[1].bringToFront();
        mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row);
        mResultRowViews[2].bringToFront();



        mCameraView = (SrsCameraView) findViewById(R.id.glsurfaceview_camera);


        mPublisher = new SrsPublisher(mCameraView);
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRtmpHandler(new FaucampRtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
//        mPublisher.setPreviewResolution(mWidth, mHeight);
//        mPublisher.setOutputResolution(mHeight, mWidth); // 这里要和preview反过来


        Camera.Size best_size= mPublisher.getmCameraView().get_best_size();
        if(best_size!=null)
        {
            Log.d(TAG,"************ Best size is "+best_size.width+" Height: "+best_size.height+" ********************");
            mPublisher.setPreviewResolution(best_size.width, best_size.height);
            mPublisher.setOutputResolution(best_size.height, best_size.width);
        }
        else
        {
            Log.d(TAG,"************ Best size is NULL ********************");
            mPublisher.setPreviewResolution(640, 480);
            mPublisher.setOutputResolution(480, 640);
        }

        mPublisher.getmCameraView().open_camera();
        mPublisher.setVideoHDMode();
        mPublisher.startCamera();

        mCameraView.setCameraCallbacksHandler(new SrsCameraView.CameraCallbacksHandler(){
            @Override
            public void onCameraParameters(Camera.Parameters params) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }
        });
//        mHeight = graphicOverlay.getHeight();
//        mWidth = graphicOverlay.getWidth();
//        System.out.println("in init, h: "+ mHeight + ", w: "+mWidth);
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("推流")) {
                    rtmpUrl = efu.getText().toString();
//                    SharedPreferences.Editor editor = sp.edit();
//                    editor.putString("rtmpUrl", rtmpUrl);
//                    editor.apply();

                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();
//


//                    if (btnSwitchEncoder.getText().toString().contentEquals("软解")) {
//                        Toast.makeText(getApplicationContext(), "使用硬件编码", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(getApplicationContext(), "使用软件编码", Toast.LENGTH_SHORT).show();
//                    }
                    btnPublish.setText("停止");
                    btnDetect.setEnabled(true);
//                    btnSwitchEncoder.setEnabled(false);
//                    btnPause.setEnabled(true);
                } else if (btnPublish.getText().toString().contentEquals("停止")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btnDetect.setEnabled(false);
                    btnPublish.setText("推流");
//                    btnRecord.setText("录像");
//                    btnSwitchEncoder.setEnabled(true);
//                    btnPause.setEnabled(false);
                }
            }
        });

        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = mPublisher.getBitmap();
                imageView.setImageBitmap(bitmap);
                String msg = "{\"event\": \"detect\"}";
//                    webSocket.sendText(msg);
                if (server_state_ready){
                    webSocket.sendText(msg);
                    btnDetect.setText("检测中");
                    btnDetect.setEnabled(false);
//                    bitmap.getBy
                    System.out.println("detect sent");
                }
                else{
                    Toast.makeText(RemoteFaceDetectActivity.this, "server还没有准备好", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % Camera.getNumberOfCameras());
                Toast.makeText(RemoteFaceDetectActivity.this, "cameras : " + Camera.getNumberOfCameras(), Toast.LENGTH_SHORT).show();
            }
        });

//        btnRecord.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (btnRecord.getText().toString().contentEquals("录像")) {
//                    if (mPublisher.startRecord(recPath)) {
//                        btnRecord.setText("暂停");
//                    }
//                } else if (btnRecord.getText().toString().contentEquals("暂停")) {
//                    mPublisher.pauseRecord();
//                    btnRecord.setText("继续");
//                } else if (btnRecord.getText().toString().contentEquals("继续")) {
//                    mPublisher.resumeRecord();
//                    btnRecord.setText("暂停");
//                }
//            }
//        });

//        btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (btnSwitchEncoder.getText().toString().contentEquals("软解")) {
//                    mPublisher.switchToSoftEncoder();
//                    btnSwitchEncoder.setText("硬解");
//                } else if (btnSwitchEncoder.getText().toString().contentEquals("硬解")) {
//                    mPublisher.switchToHardEncoder();
//                    btnSwitchEncoder.setText("软解");
//                }
//            }
//        });


        namedboxpool = new ArrayList<>();
        webSocketFactory = new WebSocketFactory();
//        WebSocket webSocket;
        try{
//            serverUri.replace("{RTMP}", rtmpUrl);

            String rtmp = Utils.extract_rtmp_string(efu.getText().toString());
//            Util util = new Util();
//            token = util.GetToken();
//            System.out.println("in util ws is " + ws);
//            if (token != null)
//                serverUri=serverUri.replace("{TOKEN}", token);
            serverUri = serverUri.replace("{RTMP}", rtmp);
            System.out.println("in rfda, serveruri " + serverUri);
            webSocket=webSocketFactory.createSocket(serverUri);
            // Android 4.0 之后不能在主线程中请求HTTP请求
//            new Thread(new Runnable(){
//                @Override
//                public void run() {
//                    try{
//                        webSocket.connect();
//                    }catch (WebSocketException exception)
//                    {
//                        exception.printStackTrace();
//                    }
//
//                }
//            }).start();
            // force main thread permitting network
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            webSocket.connect();
            webSocket.addListener(new WebSocketAdapter() {


                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    // Received a text message.
                    System.out.println("in listener, text message received "+ message);

//                    System.out.println(message);
                    //TODO info can be retrived here
                    // need further operation
                    // put box and info into boxpool

                    btnDetect.setText("检测");
                    btnDetect.setEnabled(true);

//                    NamedBox namedBox = new NamedBox(message);
//                    namedboxpool.clear();
                    if (message != null)
                    {

                        if (message.contains("ready"))
                        {
                            server_state_ready = true;
                            Toast.makeText(RemoteFaceDetectActivity.this, "ws连接成功", Toast.LENGTH_SHORT).show();
                        }
//                        System.out.println("in listener message is " + message);
                        if (server_state_ready)
                            updateNamedboxpool(message);
//                        namedboxpool.add(namedBox);
                    }
                    else
                        System.out.println("in onTextMessage message is null");
                    int w = mCameraView.getMeasuredWidth();
                    int h = mCameraView.getMeasuredHeight();
                    System.out.println("in onTextMessage w:"+w+", h:"+h);
                    NamedBox center = drawFaceResults_nbp(w, h);
                    runOnUiThread(() -> updateUI(center));
                }
            });
            webSocket.addListener(new WebSocketAdapter(){
                @Override
                public void onBinaryMessage(WebSocket webSocket, byte[] bytes) throws Exception {
                    System.out.println("in binary message listener received bytes of size " + bytes.length);
                }

            });
//            webSocket.addListener(new WebSocketAdapter(){
//                @Override
//                public void on(WebSocket webSocket, byte[] bytes) throws Exception {
//                    System.out.println("in binary message listener received bytes of size " + bytes.length);
//                }

//            });
//            webSocket.sendText("String websocket");
        }catch (IOException ioe)
        {
            System.out.println(ioe.toString());
        }
        catch (OpeningHandshakeException e)
        {e.printStackTrace();
            // A violation against the WebSocket protocol was detected
            // during the opening handshake.
        }
        catch (HostnameUnverifiedException e)
        {e.printStackTrace();
            // The certificate of the peer does not match the expected hostname.
        }
        catch (WebSocketException e)
        {
            e.printStackTrace();
            // Failed to establish a WebSocket connection.
        }catch (RuntimeException re){
            Toast.makeText(RemoteFaceDetectActivity.this, "远程服务器错误", Toast.LENGTH_LONG).show();
            finishActivity(11);
        }

    }

    // [{"topk": ["TN", "DH", "LJJ"], "box": [-24.315364837646484, 207.6920166015625, 189.6284942626953, 450.4494934082031], "distances": [0.2739424407482147, 0.33132535219192505, 0.35604554414749146]}]
    private void updateNamedboxpool(String jsonString){
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            int count = jsonObject.getInt("count");
//            int count = jsonObject.length();
            JSONArray id_array = jsonObject.getJSONArray("id");
            JSONArray id_k_array = jsonObject.getJSONArray("id_k");
            JSONArray prob_array = jsonObject.getJSONArray("prob");
            JSONArray box_array = jsonObject.getJSONArray("box");
            //TODO change career to contain more info ALSO: format of career not right.
            String info = jsonObject.getString("career");
            for(int i = 0; i < count; i++)
            {
                String[] id_k = new String[TOP_K];
                float[] prob = new float[TOP_K];
                float[] box = new float[4];
                for(int j = 0; j < TOP_K; j++){
                    id_k[j] = id_k_array.getJSONArray(i).getString(j);
                    prob[j] = (float)prob_array.getJSONArray(i).getDouble(j);
                }
                for(int j = 0; j < 4; j++){
                    box[j] = (float) box_array.getJSONArray(i).getDouble(j);
                }
                NamedBox namedBox = new NamedBox(
                        id_array.getString(i),
                        id_k,
                        prob,
                        box,
                        info
                );
                namedboxpool.add(namedBox);

            }
        }catch (JSONException jsonException)
        {
            jsonException.printStackTrace();
        }
    }
    private void updateUI(NamedBox namedBox){
        if (namedBox != null){

//            imageView.setImageBitmap(result.bitmap_c);

            for (int i = 0; i < TOP_K; i++) {
                final ResultRowView rowView = mResultRowViews[i];
                rowView.nameTextView.setText(namedBox.id_k[i]);
                rowView.scoreTextView.setText(String.format(Locale.US, SCORES_FORMAT,
                        namedBox.prob_k[i]));
                rowView.setProgressState(false);
            }
        }
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

            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, rect, namedBox.id, color);
            graphicOverlay.add(rectOverlay);
            graphicOverlay.add(rectOverlay);
        }
        namedboxpool.clear();
        return least_index;
    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        } else {
//            switch (id) {
//                case R.id.cool_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
//                    break;
//                case R.id.beauty_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
//                    break;
//                case R.id.early_bird_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
//                    break;
//                case R.id.evergreen_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
//                    break;
//                case R.id.n1977_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
//                    break;
//                case R.id.nostalgia_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
//                    break;
//                case R.id.romance_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
//                    break;
//                case R.id.sunrise_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
//                    break;
//                case R.id.sunset_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
//                    break;
//                case R.id.tender_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
//                    break;
//                case R.id.toast_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
//                    break;
//                case R.id.valencia_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
//                    break;
//                case R.id.walden_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
//                    break;
//                case R.id.warm_filter:
//                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
//                    break;
//                case R.id.original_filter:
//                default:
//                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
//                    break;
//            }
//        }
//        setTitle(item.getTitle());
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("in on start");
        if(mPublisher == null)
        {
            System.out.println("mPublisher is null");
            return;
        }
        if(mPublisher.getCamera() == null && isPermissionGranted){
            //if the camera was busy and available again
            mPublisher.startCamera();
        }
//        String msg = "{\"message\": \"rtmp stream\", \"rtmp\": \""+rtmpUrl+"\"}";
//        webSocket.sendText(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPublisher == null)
        {
            System.out.println("mPublisher is null");
            return;
        }
        final Button btn = (Button) findViewById(R.id.publish);
        btn.setEnabled(true);
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPublisher == null)
        {
            System.out.println("mPublisher is null");
            return;
        }
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            webSocket.sendClose();
            webSocket.disconnect();
            if(mPublisher == null)
            {
                System.out.println("mPublisher is null");
                return;
            }
            mPublisher.stopPublish();
            mPublisher.stopRecord();
        }catch(NullPointerException nullPointerException){
            nullPointerException.printStackTrace();
        }


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mPublisher == null)
        {
            System.out.println("mPublisher is null");
            return;
        }
        mPublisher.stopEncode();
        mPublisher.stopRecord();
//        btnRecord.setText("录像");
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (btnPublish.getText().toString().contentEquals("stop")) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }



    private void handleException(Exception e) {
        try {
            MDToast.makeText(getApplicationContext(), e.getMessage(), MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setText("推流");
//            btnRecord.setText("录像");
//            btnSwitchEncoder.setEnabled(true);
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        MDToast.makeText(getApplicationContext(), msg, MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        MDToast.makeText(getApplicationContext(), msg, MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        MDToast.makeText(getApplicationContext(), "rtmp中断", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRtmpDisconnected() {
        MDToast.makeText(getApplicationContext(), "rtmp断开", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        MDToast.makeText(getApplicationContext(), "暂停录像", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRecordResume() {
        MDToast.makeText(getApplicationContext(), "继续录像", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        MDToast.makeText(getApplicationContext(), "录像: " + msg, MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        MDToast.makeText(getApplicationContext(), "MP4文件已保存: " + msg, MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        MDToast.makeText(getApplicationContext(), "网络差", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onNetworkResume() {
        MDToast.makeText(getApplicationContext(), "网络已连接", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

}
