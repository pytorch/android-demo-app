package org.pytorch.demo.util;

import android.content.Context;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.Context.*;


public class Util {
    public static final String ws = "ws://10.138.94.7:8000/ws/chat/lobby/";//websocket测试地址

    private static WebSocketFactory webSocketFactory;
    private static WebSocket webSocket;
    private static String available_datagrams;

    private static String deliminator = "\\$\\$\\$\\$\\$\\$\\$\\$\\$\\$";

    public static String DownloadDatagramByName(String s) {
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

            File Directory = Environment.getExternalStorageDirectory();
            File datagram_file = new File(Directory, s+".json");
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

    private static void get_datagram_by_name(String s) {
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
    private static String datagram = null;
    private static void get_embeddings(){
        webSocket.sendText("get embeddings");
    }
    private static void get_datagrams(){
        webSocket.sendText("get datagrams");
    }

    public static void update_datagrams(String ad)
    {
        Util.available_datagrams = ad;
    }
    public static void update_datagram(String d){
        Util.datagram = d;
    }
    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }



    public static String GetAvailableDatagrams(String id){
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
