package org.pytorch.demo.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.pytorch.demo.AccountLoginActivity;
import org.pytorch.demo.SettingContent;
import org.pytorch.demo.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class Util {
    public String project_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FaceRecogApp";
    public String datagram_path = project_path + "/datagrams";
    public String video_path = project_path + "/videos";
    public String config_path = project_path + "/config";
    public String default_ws = "10.138.118.224";//websocket测试地址
    public String default_rtmp = "120.27.241.217";
    private WebSocketFactory webSocketFactory;
    private WebSocket webSocket;
    private String available_datagrams;
    private SharedPreferences sharedPreferences;
    public String ws_uri = null;
    public String rtmp_uri = null;
    public SettingContent settingContent;
    public double upload_progress;
    public Util(Activity activity)  {
        try{
            settingContent = GetSettingContent();
            ws_uri = settingContent.getServer_addr();
            rtmp_uri = settingContent.getRtmp_addr();
        }catch (NullPointerException xception) {
            xception.printStackTrace();
        }
        if (ws_uri == null){
            ws_uri = default_ws;
        }
        if (rtmp_uri == null)
        {
            rtmp_uri = default_rtmp;
        }
    }


    public Util(){
        try{
            settingContent = GetSettingContent();
            ws_uri = settingContent.getServer_addr();
            rtmp_uri = settingContent.getRtmp_addr();
        }catch (NullPointerException xception) {
            xception.printStackTrace();
        }
        if (ws_uri == null){
            ws_uri = default_ws;
        }
        if (rtmp_uri == null)
        {
            rtmp_uri = default_rtmp;
        }
    }

    public String deliminator = "\\${10,}";

    public String generateFileName(){
        return "" + System.currentTimeMillis();
    }


    public String UploadVideoByName(String s){
        upload_progress = 0;
        webSocketFactory = new WebSocketFactory();

//        WebSocket webSocket;
        try{
            webSocket=webSocketFactory.createSocket(ws_uri);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            webSocket.connect();
            webSocket.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    System.out.println("in listener, text message received "+ message);

                    //TODO info can be retrived here
                    // need further operation
                    // put box and info into boxpool

                    if (message != null)
                    {
                        update_datagram(message);
                    }
                    else
                        System.out.println("in onTextMessage namedbox is null");

                }
            });
            webSocket.addListener(new WebSocketAdapter(){
                @Override
                public void onBinaryMessage(WebSocket webSocket, byte[] bytes) throws Exception{
                    System.out.println("in binary message listener received bytes of size " + bytes.length);
                }

            });
//            webSocket.sendText("String websocket");
            upload_video_by_name(s);


            File video_dir = new File(video_path);
            if (!video_dir.exists()){
                video_dir.mkdir();
                return "no such local file";
            }

            File video_file = new File(video_dir, s);

            FileInputStream fileInputStream = new FileInputStream(video_file);

            int total = fileInputStream.available();
            byte[] bytes = new byte[total];
            while(fileInputStream.available() > 0){
                System.out.println("in while available is " + fileInputStream.available());
                this.upload_progress = 1 - (fileInputStream.available()* 1.0 / total);
                fileInputStream.read(bytes);
                webSocket.sendBinary(bytes);
            }
            System.out.println("out while available is " + fileInputStream.available());
            this.upload_progress = 1 - (fileInputStream.available()* 1.0 / total);
            webSocket.sendText("{\"message\": \"upload video done\", \"name\": \""+s+"\"}");
            fileInputStream.close();

        }
        catch (IOException ioe)
        {
            System.out.println(ioe.toString());
        }
        catch (OpeningHandshakeException e)
        {
            // A violation against the WebSocket protocol was detected
            // during the opening handshake.
        }
        catch (HostnameUnverifiedException e)
        {
            // The certificate of the peer does not match the expected hostname.
        }
        catch (WebSocketException e)
        {
            // Failed to establish a WebSocket connection.
        } catch (Exception e) {
            e.printStackTrace();
        }

        webSocket.disconnect();
        return "success";
    }
    public String DownloadDatagramByName(String s) {
        datagram = null;
        webSocketFactory = new WebSocketFactory();

//        WebSocket webSocket;
        try{
            webSocket=webSocketFactory.createSocket(ws_uri);

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

                    if (message != null)
                    {
//                        NamedBox namedBox = new NamedBox(message);
//                        namedboxpool.add(namedBox);
                        update_datagram(message);

                    }
                    else
                        System.out.println("in onTextMessage namedbox is null");

                }
            });
            webSocket.addListener(new WebSocketAdapter(){
                @Override
                public void onBinaryMessage(WebSocket webSocket, byte[] bytes) throws Exception{
                    System.out.println("in binary message listener received bytes of size " + bytes.length);
                }

            });
//            webSocket.sendText("String websocket");
            get_datagram_by_name(s);
            while(datagram == null){
                Thread.sleep(200);
            }

//            File Directory = Environment.getExternalStorageDirectory();
//            File project_dir = new File(Directory, "FaceRecogApp");
            File project_dir = new File(project_path);
            if (!project_dir.exists()){
                project_dir.mkdir();
            }
            File datagram_dir = new File(datagram_path);
            if (!datagram_dir.exists())
                datagram_dir.mkdir();
            File datagram_file = new File(project_dir, s+".json");
            if (!datagram_file.exists()){
                datagram_file.createNewFile();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(datagram_file);
            fileOutputStream.write(datagram.getBytes());
            fileOutputStream.close();

        }
        catch (IOException ioe)
        {
            System.out.println(ioe.toString());
        }
        catch (OpeningHandshakeException e)
        {
            // A violation against the WebSocket protocol was detected
            // during the opening handshake.
        }
        catch (HostnameUnverifiedException e)
        {
            // The certificate of the peer does not match the expected hostname.
        }
        catch (WebSocketException e)
        {
            // Failed to establish a WebSocket connection.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        webSocket.disconnect();
        return "success";
    }

    public String DownloadDatagramByName1(String s) {


        String server_addr = settingContent.getServer_addr();
        String url = "http://"+server_addr+":8080/api/datagram" +
                "/download_datagram";
//        String url = "http://publicobject.com/helloworld.txt";
        System.out.println("in ala url is " + url);
        String res = httpDatagramDownload(url, "u", "p", s);

        return "success";

    }


    public String httpDatagramDownload(String url, String u, String p, String n) {

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("username", u)
//                .add("password", p)
                .add("datagram_name", n)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        String res = null;
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    InputStream in = responseBody.byteStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String result, line = reader.readLine();
                    result = line;

                    while((line = reader.readLine()) != null) {
                        result += line;
                    }
                    System.out.println(result);
                    response.body().close();


                    try {
                        File project_dir = new File(project_path);
                        if (!project_dir.exists()){
                            project_dir.mkdir();
                        }
                        File datagram_dir = new File(datagram_path);
                        if (!datagram_dir.exists())
                            datagram_dir.mkdir();
                        File datagram_file = new File(datagram_dir, n);
                        if (!datagram_file.exists()){
                            datagram_file.createNewFile();
                        }

                        FileOutputStream fileOutputStream = new FileOutputStream(datagram_file);
                        fileOutputStream.write(result.getBytes());
                        fileOutputStream.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        });

        return null;
    }

    public String getAvailableDatagrams(String username){
        String server_addr = settingContent.getServer_addr();
        String url = "http://"+server_addr+":8080/api/datagram/get_available_datagram";
//        String url = "http://publicobject.com/helloworld.txt";
        System.out.println("in ala url is " + url);
        jsonAvailableDatagram = null;
        httPGetAvailableDatagram(url, username);

        int try_count = 0;
        while(jsonAvailableDatagram == null){
            try {
                System.out.println("waiting jsonAvailableDatagram");
                Thread.sleep(200);
                try_count += 1;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (try_count > 5)
            {
                System.out.println("stop waiting jsonAvailableDatagram after 1s");
                break;
            }
        }
        return jsonAvailableDatagram;
    }

    private String jsonAvailableDatagram;
    private String httPGetAvailableDatagram(String url, String username) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder reqBuild = new Request.Builder();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url)
                .newBuilder();
        urlBuilder.addQueryParameter("username", username);
        reqBuild.url(urlBuilder.build());
        Request request = reqBuild.build();

        String res = null;
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String res = responseBody.string();
                    jsonAvailableDatagram = res;
                    System.out.println(res);
                    response.body().close();
                }
            }
        });

        return null;
    }


    private static final String IMGUR_CLIENT_ID = "...";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private static final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
    private static final MediaType MEDIA_TYPE_MP4 = MediaType.parse("video/mp4");
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("text/plain");
    public String UploadVideoByName(String user, String pass, String filename){
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", user)
                .addFormDataPart("date", "2021-03-16")
                .addFormDataPart("file", filename,
                        RequestBody.create(MEDIA_TYPE_MP4, new File(video_path, filename)))
                .build();

        String server_addr = settingContent.getServer_addr();
        String url = "http://"+server_addr+":8080/api/photos/upload_video";
        String token = GetToken();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
//                .header("Content-Type", "multipart/form-data")
//                .header("accept", "application/json")
                .post(requestBody)
                .build();
        System.out.println("request " + request);


        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    System.out.println("successsssssssss");
                }
            }
        });

        return "success";
    }

    public String deleteCrewByName(String name){
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("sailorname", name)
                .build();

        String server_addr = settingContent.getServer_addr();
        String url = "http://"+server_addr+":8080/api/sailors";
        String token = GetToken();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
//                .header("Content-Type", "multipart/form-data")
//                .header("accept", "application/json")
                .delete(requestBody)
                .build();
        System.out.println("request " + request);


        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    System.out.println("successsssssssss");
                }
            }
        });

        return "success";
    }


    public void resetPassword(String oldp, String newp){
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("old_password ", oldp)
                .add("new_password ", newp)
                .build();

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + GetToken())
                .url("http://10.138.100.154:8080/api/account/reset-password")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    System.out.println("successsssssssss");
                }
            }
        });
    }

    public String UploadNewCrewInfo(String chinese_name, String english_name, String position, String phone_number, File photo){
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chinese_name", chinese_name)
                .addFormDataPart("english_name", english_name)
                .addFormDataPart("position", position)
                .addFormDataPart("phone_number", phone_number)
                .addFormDataPart("file", photo.getName(),
                        RequestBody.create(MEDIA_TYPE_JPG, photo))
                .build();

        String server_addr = settingContent.getServer_addr();
        String url = "http://"+server_addr+":8080/api/sailors";
        String token = GetToken();
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
//                .header("Content-Type", "multipart/form-data")
//                .header("accept", "application/json")
                .post(requestBody)
                .build();
        System.out.println("request " + request);


        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    System.out.println("successsssssssss");
                }
            }
        });

        return "success";
    }
    public void SetLocalJson(String str) throws IOException {
//        File Directory = Environment.getExternalStorageDirectory();
//        File project_dir = new File(Directory, "FaceRecogAppConfig");
        File project_dir = new File(config_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
        }
        File jsonfile = new File(project_dir, "config.json");
        if (!jsonfile.exists()){
            try{
                jsonfile.createNewFile();
            }catch (IOException exception){
                exception.printStackTrace();
            }
        }
        try{

            FileOutputStream fileOutputStream = new FileOutputStream(jsonfile);
            fileOutputStream.write(str.getBytes());
            fileOutputStream.close();

        }catch (FileNotFoundException fileNotFoundException){
            fileNotFoundException.printStackTrace();
        }
    }
    public SettingContent GetSettingContent(){
        String json = GetLocalJson().toString();
        Gson gson = new Gson();
        return gson.fromJson(json, SettingContent.class);
    }

    public void SetSettingContent(SettingContent settingContent) throws IOException {
        Gson gson = new Gson();
        SetLocalJson(gson.toJson(settingContent));
    }

    public void SetToken(String token){
        SettingContent settingContent = GetSettingContent();
        settingContent.setToken(token);
        try {
            SetSettingContent(settingContent);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
    public String GetToken(){
//        SettingContent settingContent = GetSettingContent();
        return settingContent.getToken();
    }



    public JSONObject GetLocalJson(){
        File project_directory = new File(project_path);
        if (!project_directory.exists())
            project_directory.mkdir();
        File project_dir = new File(config_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
        }
        File jsonfile = new File(project_dir, "config.json");
        if (!jsonfile.exists()){
            try{
                jsonfile.createNewFile();
                return new JSONObject("");
            }catch (IOException | JSONException exception){
                exception.printStackTrace();
            }

        }
        try{
            FileInputStream fileInputStream = new FileInputStream(jsonfile);
            int length = fileInputStream.available();
            byte bytes[] = new byte[length];
            fileInputStream.read(bytes);
            fileInputStream.close();
            String str =new String(bytes, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(str);
            System.out.println("in get local json str is "+str);
            return jsonObject;
        }catch (FileNotFoundException fileNotFoundException){
            fileNotFoundException.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
        return null;

    }
    public String[] GetLocalVideos(){
        File project_dir  = new File(video_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
            return new String[0];
        }
        String[] filenames = project_dir.list();
        return filenames;
    }
    public String[] GetLocalDatagrams(){
//        File Directory = Environment.getExternalStorageDirectory();
//        File project_dir = new File(Directory, "FaceRecogApp");
        File project_dir  = new File(datagram_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
            return new String[0];
        }
//        File[] files = project_dir.listFiles();
        String[] filenames = project_dir.list();
        return filenames;
    }
    public File[] GetLocalDatagramFiles(){
//        File Directory = Environment.getExternalStorageDirectory();
//        File project_dir = new File(Directory, "FaceRecogApp");
        File project_dir  = new File(datagram_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
            return new File[0];
        }
        File[] files = project_dir.listFiles();
        return files;
    }

    public String get_embedding_from_file(File f) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(f);
        int length = fileInputStream.available();
        byte bytes[] = new byte[length];
        fileInputStream.read(bytes);
        fileInputStream.close();
        String str =new String(bytes, StandardCharsets.UTF_8);
        return str;
    }
    public String get_embeddings_from_files(){
//        File Directory = getFilesDir();
        File[] files = GetLocalDatagramFiles();
        String embedding_str = "";
        if(files.length == 0)
            return "";
        for (File f: files){
            System.out.println("in fda reading file " + f.getName());
            try{
                String str = get_embedding_from_file(f);
                embedding_str += str;
                System.out.println("str len is "+str.length()+" and total embedding len is "+ embedding_str.length());

            }catch (FileNotFoundException fileNotFoundException){
                fileNotFoundException.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return embedding_str;
    }

    public boolean save_embedding_local_file(Utils.NamedEmbedding embedding){

        String json_str = "{\"embedding\": [";
        for (float f: embedding.embedding){
            json_str += Float.toString(f) + ", ";
        }
        json_str = json_str.substring(0, json_str.length()-2);
        json_str += "], \"name\": \"" + embedding.id + "\"}";
        json_str += "$$$$$$$$$$";
        System.out.println(json_str);


        File project_dir = new File(datagram_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
        }
        File jsonfile = new File(project_dir, "local_temp_datagram.json");
        if (!jsonfile.exists()){
            try{
                jsonfile.createNewFile();
            }catch (IOException exception){
                exception.printStackTrace();
            }
        }
        try{

            FileOutputStream fileOutputStream = new FileOutputStream(jsonfile, true);
            fileOutputStream.write(json_str.getBytes());
            fileOutputStream.close();
            return true;

        }catch (FileNotFoundException fileNotFoundException){
            fileNotFoundException.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    private void upload_video_by_name(String s){
        webSocket.sendText("{\"message\": \"upload video by name\", \"name\": \""+s+"\"}");
    }
    private void get_datagram_by_name(String s) {
        webSocket.sendText("{\"message\": \"get datagram by name\", \"name\": \""+s+"\"}");
//        webSocket.sendText("get datagram by name:"+s);
    }
    private void send_username_pass(String s){
        webSocket.sendText("");
    }

    public void delete_datagram(String name) {
        File file = new File(datagram_path, name);
        if (file.exists()){
            file.delete();
        }
    }

    private static class NamedEmbedding{
        public float[] embedding;
        public String id;

        NamedEmbedding(String jsonString){
            try{
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
            }


        }

        NamedEmbedding(){
            embedding = new float[512];
            id = null;
        }
    }
    private String datagram = null;
    private void get_embeddings(){
        webSocket.sendText("get embeddings");
    }
    private void get_datagrams(){
        webSocket.sendText("get datagrams");
    }

    public void update_datagrams(String ad)
    {
        this.available_datagrams = ad;
    }
    public void update_datagram(String d){
        this.datagram = d;
    }
    private String login_status;
    public void update_login_status(String s){
        this.login_status = s;
    }
    public void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public String GetAvailableDatagrams(String id){
        available_datagrams = null;
        webSocketFactory = new WebSocketFactory();

//        WebSocket webSocket;
        try{
            webSocket=webSocketFactory.createSocket(ws_uri);

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

                    if (message != null)
                    {
//                        NamedBox namedBox = new NamedBox(message);
//                        namedboxpool.add(namedBox);
                        update_datagrams(message);
                    }
                    else
                        System.out.println("in onTextMessage namedbox is null");

                }
            });
            webSocket.addListener(new WebSocketAdapter(){
                @Override
                public void onBinaryMessage(WebSocket webSocket, byte[] bytes) throws Exception{
                    System.out.println("in binary message listener received bytes of size " + bytes.length);
                }

            });
//            webSocket.sendText("String websocket");
            get_datagrams();
            while(available_datagrams == null){
                Thread.sleep(200);
            }
        }catch (IOException ioe)
        {
            System.out.println(ioe.toString());
        }
        catch (OpeningHandshakeException e)
        {
            // A violation against the WebSocket protocol was detected
            // during the opening handshake.
        }
        catch (HostnameUnverifiedException e)
        {
            // The certificate of the peer does not match the expected hostname.
        }
        catch (WebSocketException e)
        {
            // Failed to establish a WebSocket connection.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (RuntimeException re){
            re.printStackTrace();
            return null;

        }

        webSocket.disconnect();
        return available_datagrams;

    }

    public String getRTMPURL(){
//        return "rtmp://"+server_uri+"/" + getRandomAlphaString(3) + '/' + getRandomAlphaDigitString(5);
        return "rtmp://" + rtmp_uri + "/live/livestream?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMCIsImV4cCI6MTYxNTQ1NDA3OCwic2NvcGVzIjpbImFwcHVzZXIiXX0.YaJewk07x36ThTgGlPh9M832CQtzJYDvq6HkJ7ynmTM";
    }

    public String getWebsocket_TEMPLATE(){
        return "ws://"+ws_uri+":8080/ws?rtmp={RTMP}&token="+settingContent.getToken();
    }

    public String getRandomAlphaString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public String getRandomAlphaDigitString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }



    public class Crew{
        String crew_id;
        File crew_file;

        public String getCrew_id() {
            return crew_id;
        }

        public File getCrew_file() {
            return crew_file;
        }
//        Crew(String i, File f){
//            this.crew_file = f;
//            this.crew_id = i;
//        }

        Crew(String jsonString, File file){
            if (jsonString.length() < 10)
                return;
            try{
                jsonString = jsonString.replace("\\","");
                if (jsonString.startsWith("\""))
                    jsonString = jsonString.substring(jsonString.indexOf('{'));
                if (jsonString.endsWith("\""))
                    jsonString = jsonString.substring(0, jsonString.indexOf('}')+1);
                System.out.println("In NamedEmbedding json str is " + jsonString);
                JSONObject jsonObject = new JSONObject(jsonString);
                this.crew_id = jsonObject.getString("name");
                JSONArray jsonArray = jsonObject.getJSONArray("embedding");
                this.crew_file = file;
            }catch (JSONException jsonException)
            {
                jsonException.printStackTrace();
                System.out.println("in catch json str is " + jsonString);
                this.crew_file = null;
                this.crew_id = null;
            }

        }
    }
    public void write_file_override(String str, File file){
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(str.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    public void delete_by_name(String name, String filename){
        File datagram_file  = new File(datagram_path, filename);
        if (datagram_file.exists()){
            String str = null;
            // 读出文件
            try {
                str = get_embedding_from_file(datagram_file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            int name_pos = str.lastIndexOf(name);
            String str1 = str.substring(0, name_pos);
            String str2 = str.substring(name_pos);
            int str1_end = str1.lastIndexOf("$");
            int str2_start = str2.indexOf("$");
            System.out.println("name in str is "+ str.indexOf(name));
            str = str1.substring(0, str1_end).concat(str2.substring(str2_start));
            System.out.println("name in str is "+ str.indexOf(name));
            // 写入文件
            write_file_override(str, datagram_file);
        }

    }

    public ArrayList<Crew> get_all_crews(){

        ArrayList<Crew> crewArrayList = new ArrayList<>();
        File[] files = GetLocalDatagramFiles();
        String embedding_str = "";
        if(files.length == 0)
            return null;
        for (File f: files){
            String str = null;
            try {
                str = get_embedding_from_file(f);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            String[] strings = str.split(deliminator);
            for (String s : strings){
                if(s.length() > 100){
                    crewArrayList.add(new Crew(s, f));
                }
            }
        }

        System.out.println("CrewList.size() "+crewArrayList.size());
        return crewArrayList;
    }
}
