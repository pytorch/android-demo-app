package org.pytorch.demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Utils {
  public static String assetFilePath(Context context, String assetName) {
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
      Log.e(Constants.TAG, "Error process asset " + assetName + " to file path");
    }
    return null;
  }

  public static int[] topK(float[] a, final int topk) {
    float values[] = new float[topk];
    Arrays.fill(values, -Float.MAX_VALUE);
    int ixs[] = new int[topk];
    Arrays.fill(ixs, -1);

    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < topk; j++) {
        if (a[i] > values[j]) {
          for (int k = topk - 1; k >= j + 1; k--) {
            values[k] = values[k - 1];
            ixs[k] = ixs[k - 1];
          }
          values[j] = a[i];
          ixs[j] = i;
          break;
        }
      }
    }
    return ixs;
  }

  public static class NamedBox{

    private static final int TOP_K = 3;
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

  public static class NamedEmbedding{
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

}
