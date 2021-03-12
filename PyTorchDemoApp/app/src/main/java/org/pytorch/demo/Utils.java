package org.pytorch.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RadioButton;

import com.valdesekamdem.library.mdtoast.MDToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.vision.Helper.GraphicOverlay;
import org.pytorch.demo.vision.Helper.RectOverlay;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class Utils {

  public static String token = null;
  public static int TOP_K = 3;
  public static float distance_threshold = 0.7f;
  public static final String FORMAT_MS = "%dms";
  public static final String FORMAT_AVG_MS = "avg:%.0fms";
  public static final String FILE_PROVIDER_AUTHORITY = "authority";
  public static final String FORMAT_FPS = "%.1fFPS";
  public static final String SCORES_FORMAT = "%.2f";


  /*
  * @params rtmp_uri: uri with format like rtmp://ip[:port]/rtmp1/rtmp2
  * @return rtmp2
  *
  * */
  public static String extract_rtmp_string(String rtmp_uri){
    int i = rtmp_uri.lastIndexOf("/");
    if (i >= 0)
      return rtmp_uri.substring(i+1);
    else
      return "";
  }
  public static Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
            matrix, true);
  }
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


  public static ArrayList<Float> possible_score(Tensor scores, Tensor boxes, float threshold)
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

  public static float IoU(float[] rec1, float[] rec2)
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

  public static ArrayList<float[]> nms(Tensor boxes, Tensor scores, ArrayList<Float> possible_indexes, float nms_threshold)
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

  public static  ArrayList<float[]> expand_box(ArrayList<float[]> boxes, float ratio)
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

  /*
   * 使用@param nms_box中的框截取@param bitmap，对于获得的人脸，计算embedding。
   *
   * */
  public static void get_unrecognized_face_embedding(Bitmap bitmap, ArrayList<float[]> nms_boxes, Module encoder, ArrayList<NamedEmbedding> namedEmbeddings,  ArrayList<Utils.NamedBox> namedboxpool){
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
        NamedEmbedding unnamed = encode_face(bitmap_c, encoder);
        find_topk_distance(unnamed, namedEmbeddings, box_c, TOP_K, namedboxpool);
      }
    }

  }

  /*
   * 使用@param unnamed 中的embedding和全局变量embeddings计算512维空间的欧式距离
   * 找到距离最近的@param topk个结果，将最近的结果打包成NamedBox放到全局变量namedboxpool中去用于画框。
   */
  public static void find_topk_distance(NamedEmbedding unnamed, ArrayList<NamedEmbedding> namedEmbeddings, float[] box_c, int topk, ArrayList<NamedBox> namedboxpool){
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
  public static float calc_dist(float[] x, float[] y){
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
  public static int[] get_min_topk(float[] array, int k) {
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



  public static NamedEmbedding encode_face(Bitmap bitmap_c, Module encoder){
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

  public static NamedBox drawFaceResults_nbp(int width, int height, GraphicOverlay graphicOverlay, ArrayList<NamedBox> namedboxpool){
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

  public static NamedBox drawFaceResults(ArrayList<float[]> nms_boxes, int width, int height, GraphicOverlay graphicOverlay, ArrayList<NamedBox> namedboxpool) {
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
//  public static class NamedEmbedding{
//    public float[] embedding;
//    public String id;
//
//    NamedEmbedding(String jsonString){
//      if (jsonString.length() < 10)
//        return;
//      try{
//        jsonString = jsonString.replace("\\","");
//        System.out.println("In NamedEmbedding json str is " + jsonString);
//        JSONObject jsonObject = new JSONObject(jsonString);
//        this.id = jsonObject.getString("name");
//        JSONArray jsonArray = jsonObject.getJSONArray("embedding");
//        this.embedding = new float[512];
//        for(int i = 0; i < 512; i++)
//        {
//          this.embedding[i] = (float) jsonArray.getDouble(i);
//        }
//      }catch (JSONException jsonException)
//      {
//        jsonException.printStackTrace();
//        this.embedding = null;
//        this.id = null;
//      }
//
//
//    }
//
//    NamedEmbedding(){
//      embedding = new float[512];
//      id = null;
//    }
//  }
public static class NamedEmbedding{
  public float[] embedding;
  public String id;

  NamedEmbedding(String jsonString){
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
      System.out.println("in catch json str is " + jsonString);
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
