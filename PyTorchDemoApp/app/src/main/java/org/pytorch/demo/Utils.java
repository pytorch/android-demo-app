package org.pytorch.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.valdesekamdem.library.mdtoast.MDToast;

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

  public static String token = null;
  public static int TOP_K = 3;
  public static float distance_threshold = 0.7f;

  public static void convertDis2Prob(String[] names, float[] dists){
    float total = 0;
    for (int i = 0; i < dists.length; i++){
      if (dists[i] > distance_threshold){
        names[i] = "UNKNOWN";
        dists[i] = 0;
      }
      else{
        dists[i] = distance_threshold - dists[i];
        total += dists[i];
      }

    }
    for (int i = 0; i < dists.length; i++){
      dists[i]/=total;
    }
  }



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

  public static double distance2middle(float[] box)
  {
    float midx = (box[0] + box[2]) / 2;
    float midy = (box[1] + box[3]) / 2;

    double dist = Math.pow((midx - 0.5), 2) + Math.pow((midy - 0.5), 2);
    return dist;
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

    NamedBox copy(){
      NamedBox newNamedBox = new NamedBox();
      newNamedBox.id = this.id;
      newNamedBox.id_k = this.id_k;
      newNamedBox.prob_k = this.prob_k;
      newNamedBox.rect = this.rect;
      newNamedBox.info = this.info;
      return newNamedBox;
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


  public static Bitmap cropBitmap(Bitmap bitmap, float[] rect) {
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
  public static int Type_info = MDToast.TYPE_INFO;
  public static int Type_error = MDToast.TYPE_ERROR;
  public static int Type_success = MDToast.TYPE_SUCCESS;
  public static int Type_warning = MDToast.TYPE_WARNING;
  public static int dura_long = MDToast.LENGTH_LONG;
  public static int dura_short = MDToast.LENGTH_SHORT;
  public static void ShowMDToast(Context context, String message, int duration, int type){
    MDToast.makeText(context, message, duration, type).show();
  }


  /**

     * 根据文件后缀名获得对应的MIME类型。

     * @param file

     */

  public static String getMIMEType(File file) {
  String type="*/*";

  String fName = file.getName();

  //获取后缀名前的分隔符"."在fName中的位置。

  int dotIndex = fName.lastIndexOf(".");

  if(dotIndex < 0){
  return type;

  }

  /* 获取文件的后缀名*/

  String end=fName.substring(dotIndex,fName.length()).toLowerCase();

  if(end=="")return type;

  //在MIME和文件类型的匹配表中找到对应的MIME类型。

  for(int i=0;i<MIME_MapTable.length;i++){
    if(end.equals(MIME_MapTable[i][0]))
      type = MIME_MapTable[i][1];
  }

  return type;

  }

  public static final String[][] MIME_MapTable={
          //{后缀名，MIME类型}

                  {".3gp", "video/3gpp"},

                  {".apk", "application/vnd.android.package-archive"},

                  {".asf", "video/x-ms-asf"},

                  {".avi", "video/x-msvideo"},

                  {".bin", "application/octet-stream"},

                  {".bmp", "image/bmp"},

                  {".c", "text/plain"},

                  {".class", "application/octet-stream"},

                  {".conf", "text/plain"},

                  {".cpp", "text/plain"},

                  {".doc", "application/msword"},

                  {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},

                  {".xls", "application/vnd.ms-excel"},

                  {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},

                  {".exe", "application/octet-stream"},

                  {".gif", "image/gif"},

                  {".gtar", "application/x-gtar"},

                  {".gz", "application/x-gzip"},

                  {".h", "text/plain"},

                  {".htm", "text/html"},

                  {".html", "text/html"},

                  {".jar", "application/java-archive"},

                  {".java", "text/plain"},

                  {".jpeg", "image/jpeg"},

                  {".jpg", "image/jpeg"},

                  {".js", "application/x-javascript"},

                  {".log", "text/plain"},

                  {".json", "text/plain"},

                  {".m3u", "audio/x-mpegurl"},

                  {".m4a", "audio/mp4a-latm"},

                  {".m4b", "audio/mp4a-latm"},

                  {".m4p", "audio/mp4a-latm"},

                  {".m4u", "video/vnd.mpegurl"},

                  {".m4v", "video/x-m4v"},

                  {".mov", "video/quicktime"},

                  {".mp2", "audio/x-mpeg"},

                  {".mp3", "audio/x-mpeg"},

                  {".mp4", "video/mp4"},

                  {".mpc", "application/vnd.mpohun.certificate"},

                  {".mpe", "video/mpeg"},

                  {".mpeg", "video/mpeg"},

                  {".mpg", "video/mpeg"},

                  {".mpg4", "video/mp4"},

                  {".mpga", "audio/mpeg"},

                  {".msg", "application/vnd.ms-outlook"},

                  {".ogg", "audio/ogg"},

                  {".pdf", "application/pdf"},

                  {".png", "image/png"},

                  {".pps", "application/vnd.ms-powerpoint"},

                  {".ppt", "application/vnd.ms-powerpoint"},

                  {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},

                  {".prop", "text/plain"},

                  {".rc", "text/plain"},

                  {".rmvb", "audio/x-pn-realaudio"},

                  {".rtf", "application/rtf"},

                  {".sh", "text/plain"},

                  {".tar", "application/x-tar"},

                  {".tgz", "application/x-compressed"},

                  {".txt", "text/plain"},

                  {".wav", "audio/x-wav"},

                  {".wma", "audio/x-ms-wma"},

                  {".wmv", "audio/x-ms-wmv"},

                  {".wps", "application/vnd.ms-works"},

                  {".xml", "text/plain"},

                  {".z", "application/x-compress"},

                  {".zip", "application/x-zip-compressed"},

                  {"", "*/*"}

                  };


          


}
