package org.pytorch.demo;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.demo.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class VideoSelectAdapter extends RecyclerView.Adapter<VideoSelectAdapter.ViewPagerViewHolder> {
    ViewGroup parent;
    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_select_face_datagram, parent, false);
        this.parent = parent;
        return new ViewPagerViewHolder(view);
    }

    ListView listView;
    ListView listView1;

    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {
//        TextView textView = holder.itemView.findViewById(R.id.textView);
//        textView.setText(String.format("set to %d", position));
//
//        listView = holder.itemView.findViewById(R.id.listview);
//        Button button = holder.itemView.findViewById(R.id.button);
//
//        button.setOnClickListener(new View.OnClickListener() {
//                                      @SuppressLint("StaticFieldLeak")
//                                      @Override
//                                      public void onClick(View v) {
//                                          String login_id = "1";
//                                          new AsyncTask<String, Integer, String>(){
//
//
//                                              @Override
//                                              protected String doInBackground(String... arg0){
//                                                  String res = new Util().GetAvailableDatagrams("id");
//                                                  return res;
//                                              }
//
//                                              protected void onPostExecute(String result){
//                                                  if (result != null){
//                                                      Toast.makeText(parent.getContext(), "刷新完成",Toast.LENGTH_SHORT).show();
//                                                      updateListView(result);
//                                                  }else{
//                                                      Toast.makeText(parent.getContext(), "刷新失败，检查网络",Toast.LENGTH_SHORT).show();
//                                                  }
//                                              }
//
////                                              @Override
////                                              protected onProgressUpdate(Integer... progress){
////                                                  setProgressPercent(progress[0]);
////                                              }
//                                          }.execute("1");
//                                      }
//                                  }
//        );

        if(position == 0){
            TextView textView = holder.itemView.findViewById(R.id.textView);
            textView.setText(String.format("***********************set to %d", position));

            System.out.println("in onbindviewholder position is "+position);
            listView = holder.itemView.findViewById(R.id.listview);
            Button button = holder.itemView.findViewById(R.id.button);

            button.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("StaticFieldLeak")
                @Override
                public void onClick(View v) {
                    String login_id = "1";
                    new AsyncTask<String, Integer, String>() {


                        @Override
                        protected String doInBackground(String... arg0) {
                            String res = new Util().GetAvailableDatagrams("id");
                            return res;
                        }

                        protected void onPostExecute(String result) {
                            if (result != null) {
                                Toast.makeText(parent.getContext(), "刷新完成", Toast.LENGTH_SHORT).show();
                                updateListView0(result);
                            } else {
                                Toast.makeText(parent.getContext(), "刷新失败，网络或服务器出错", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute("1");
                }
            });


//            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                @SuppressLint("StaticFieldLeak")
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    System.out.println("position "+position);
//                    String name = ((HashMap<String, String>)(listView.getItemAtPosition(position))).get("name");
//                    System.out.println(name);
//                    new AsyncTask<String, Integer, String>(){
//                        @Override
//                        protected String doInBackground(String... arg0){
//                            String res = new Util().DownloadDatagramByName(arg0[0]);
//                            return res;
//                        }
//                        protected void onPostExecute(String result) {
//                            if (result != null) {
//                                Toast.makeText(parent.getContext(), "下载完成", Toast.LENGTH_SHORT).show();
//                                System.out.println(result);
//                            }else{
//                                Toast.makeText(parent.getContext(), "下载失败，网络或服务器出错",Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }.execute(name);
//                }
//            });
        }
        else {
            TextView textView = holder.itemView.findViewById(R.id.textView);
            textView.setText(String.format("*****************set to %d", position));

            System.out.println("in onbindviewholder position is " + position);
            listView1 = holder.itemView.findViewById(R.id.listview);
            Button button = holder.itemView.findViewById(R.id.button);

            button.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              String login_id = "1";
                                              String[] filenames = new Util().GetLocalVideos();
                                              updateListView1(filenames);
                                          }
                                      }
            );

            listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                AlertDialog alertDialog;

                public void upload_dialog(String filename){
//                    alertDialog.hide();
                    alertDialog.dismiss();
                    System.out.println("before create pd");
                    ProgressDialog dialog = new ProgressDialog(parent.getContext());
                    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setTitle("上传中");
                    System.out.println("before setting listener");
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Toast.makeText(parent.getContext(), "上传成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.setMessage("正在上传，请稍等");
                    System.out.println("before show");
                    dialog.show();
//                    dialog.setMax(100);

                    Util util = new Util();
                    new AsyncTask<String, Integer, String>(){
                        @Override
                        protected String doInBackground(String... arg0){
                            String res = util.UploadVideoByName(arg0[0]);
                            return res;
                        }

                        protected void onPostExecute(String result){
                            if (result != null){
                                Toast.makeText(parent.getContext(), "上传完成",Toast.LENGTH_SHORT).show();
                                System.out.println(result);
                            }else{
                                Toast.makeText(parent.getContext(), "上传失败，检查网络或服务器",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute(filename);

                    System.out.println("before while");
                    while(util.upload_progress < 0.99){
                        System.out.println("in while up is "+ util.upload_progress);
                        dialog.setProgress((int)(100 * util.upload_progress));
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
//                    Toast.makeText(parent.getContext(),"上传成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                }



                @Override
                @SuppressLint("StaticFieldLeak")
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    System.out.println("position "+position);
                    String name = ((HashMap<String, String>)(listView1.getItemAtPosition(position))).get("name");
                    System.out.println(name);
//                Environment.getDataDirectory()

                    final String[] choices = new String[]{"预览", "上传"};

                    alertDialog = new AlertDialog.Builder(parent.getContext())
                            .setTitle("选择操作")
                            .setIcon(R.drawable.ic_logo_pytorch)
                            .setItems(choices, new DialogInterface.OnClickListener() {//添加列表
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(i==0)//预览
                                    {
                                        //TODO call media here
                                    }
                                    else if (i == 1)// 下载
                                    {
                                        upload_dialog(name);
                                    }
//                                    Toast.makeText(parent.getContext(), "点的是：" + choices[i], Toast.LENGTH_SHORT).show();
                                }
                            })
                            .create();

                    alertDialog.show();

//                    Button button_upload = popupWindow.getContentView().findViewById(R.id.button_upload);
//                    button_upload.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//
//                        }
//                    });
//                    Button button_preview = popupWindow.getContentView().findViewById(R.id.button_preview);
//                    button_preview.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            //TODO open media and play video here
//                        }
//                    });
//                    ProgressBar progressBar = popupWindow.getContentView().findViewById(R.id.progressBar);
//                    progressBar.setMax(100);

                }
            });
        }

    }

    public void updateListView1(String[] filenames){
        if (filenames.length == 0){
            Toast.makeText(parent.getContext(), new Util().video_path + "下没有文件", Toast.LENGTH_SHORT).show();
            return;
        }
        System.out.println("in updata listview first filename string is "+filenames[0]);
        ArrayList<Map<String,String>> list = new ArrayList<>();


        for (int i = 0; i < filenames.length; i++){
            Map<String, String> map= new HashMap<>();
            map.put("name", filenames[i]);
//                map.put("mission", jsonArraym.getString(i));
//                map.put("site", jsonArrays.getString(i));
            list.add(map);
        }


        SimpleAdapter adapter = new SimpleAdapter(
                parent.getContext(),
                list,
                R.layout.datagram_list_item,
                new String[]{"name"},
                new int[]{R.id.text1});


        listView1.setAdapter(adapter);
    }

    public void updateListView0(String jsonString){
        System.out.println("in updata listview json string is "+jsonString);
        ArrayList<Map<String,String>> list = new ArrayList<>();
        try{
            JSONObject jsonObject = new JSONObject(jsonString);
            int count = jsonObject.getInt("datagram len");
            JSONArray jsonArrayn = jsonObject.getJSONArray("datagram name");
            JSONArray jsonArraym = jsonObject.getJSONArray("datagram mission");
            JSONArray jsonArrays = jsonObject.getJSONArray("datagram site");

            for (int i = 0; i < count; i++){
                Map<String, String> map= new HashMap<>();
                map.put("name", jsonArrayn.getString(i));
                map.put("mission", jsonArraym.getString(i));
                map.put("site", jsonArrays.getString(i));
                list.add(map);
            }
        }catch (JSONException jsonException){
            jsonException.printStackTrace();
        }

        SimpleAdapter adapter = new SimpleAdapter(parent.getContext(),
                list,
                R.layout.datagram_list_item,
                new String[]{"name", "mission"},
                new int[]{R.id.text1, R.id.text2});

        listView.setAdapter(adapter);




    }
    @Override
    public int getItemCount() {
        return 2;
    }

    public VideoSelectAdapter() {
        super();
    }

    public class ViewPagerViewHolder extends RecyclerView.ViewHolder{
        public ViewPagerViewHolder(View itemView){
            super(itemView);
        }
    }

}
