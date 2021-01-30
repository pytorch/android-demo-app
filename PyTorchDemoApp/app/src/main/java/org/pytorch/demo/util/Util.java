package org.pytorch.demo.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.ImageView;
import android.widget.Toast;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.demo.BitmapToVideoEncoder;
import org.pytorch.demo.FaceDetectionActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;



public class Util {
    public String project_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FaceRecogApp";
    public String datagram_path = project_path + "/datagrams";
    public String video_path = project_path + "/videos";
    public String default_ws = "ws://10.138.118.224.7:8000/ws/chat/lobby/";//websocket测试地址
    public String default_server = "120.27.241.217";
    private WebSocketFactory webSocketFactory;
    private WebSocket webSocket;
    private String available_datagrams;
    private SharedPreferences sharedPreferences;
    public String ws = null;
    public String server_uri = null;
    public Util(Activity activity)  {
        File project_dir = new File(project_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
        }
//        Util();
//        sharedPreferences = activity.getSharedPreferences("info",  Activity.MODE_PRIVATE);
//        ws = sharedPreferences.getString("rtmp_uri", default_ws);
//        server_uri = sharedPreferences.getString("server_uri", default_server);
//        default_server = server_uri;
//        default_ws = ws;
        JSONObject jsonObject = GetLocalJson();
        try{
            ws = jsonObject.getString("rtmp_uri");
            server_uri = jsonObject.getString("server_uri");
        }catch (JSONException | NullPointerException jsonException){
            jsonException.printStackTrace();
        }
        if (ws == null){
            ws = default_ws;
        }
        if (server_uri == null)
        {
            server_uri = default_server;
        }
        System.out.println("server_uri "+server_uri);
        System.out.println("rtmp uri " + ws);

    }
    public Util(){
        File project_dir = new File(project_path);
        if (!project_dir.exists()){
            project_dir.mkdir();
        }
        JSONObject jsonObject = GetLocalJson();
        try{
            ws = jsonObject.getString("rtmp_uri");
            server_uri = jsonObject.getString("server_uri");
        }catch (JSONException | NullPointerException jsonException){
            jsonException.printStackTrace();
        }
        if (ws == null){
            ws = default_ws;
        }
        if (server_uri == null)
        {
            server_uri = default_server;
        }
        System.out.println("server_uri "+server_uri);
        System.out.println("rtmp uri " + ws);
    }

    public String deliminator = "\\${10,}";
    public String DownloadDatagramByName(String s) {
        datagram = null;
        webSocketFactory = new WebSocketFactory();

//        WebSocket webSocket;
        try{
            webSocket=webSocketFactory.createSocket(ws);

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

    public void SetLocalJson(String str) throws IOException {
        File Directory = Environment.getExternalStorageDirectory();
        File project_dir = new File(Directory, "FaceRecogAppConfig");
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


    public JSONObject GetLocalJson(){
        File Directory = Environment.getExternalStorageDirectory();
        File project_dir = new File(Directory, "FaceRecogAppConfig");
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

    public String[] GetLocalDatagrams(){
        File Directory = Environment.getExternalStorageDirectory();
        File project_dir = new File(Directory, "FaceRecogApp");
        if (!project_dir.exists()){
            project_dir.mkdir();
            return new String[0];
        }
//        File[] files = project_dir.listFiles();
        String[] filenames = project_dir.list();
        return filenames;
    }
    public File[] GetLocalDatagramFiles(){
        File Directory = Environment.getExternalStorageDirectory();
        File project_dir = new File(Directory, "FaceRecogApp");
        if (!project_dir.exists()){
            project_dir.mkdir();
            return new File[0];
        }
        File[] files = project_dir.listFiles();
        return files;
    }

    private void get_datagram_by_name(String s) {
        webSocket.sendText("{\"message\": \"get datagram by name\", \"name\": \""+s+"\"}");
//        webSocket.sendText("get datagram by name:"+s);
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
    public void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }



    public String GetAvailableDatagrams(String id){
        available_datagrams = null;
        webSocketFactory = new WebSocketFactory();

//        WebSocket webSocket;
        try{
            webSocket=webSocketFactory.createSocket(ws);

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

        webSocket.disconnect();
        return available_datagrams;

    }
}
