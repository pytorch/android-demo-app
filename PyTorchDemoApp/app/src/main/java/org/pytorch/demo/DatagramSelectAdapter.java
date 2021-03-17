package org.pytorch.demo;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.valdesekamdem.library.mdtoast.MDToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.demo.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DatagramSelectAdapter extends RecyclerView.Adapter<DatagramSelectAdapter.ViewPagerViewHolder> {
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
    SwipeRefreshLayout swipeRefreshLayout;
    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {

        if(position == 0){
            swipeRefreshLayout = holder.itemView.findViewById(R.id.swiperFresh);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refresh_lv0();
//                    new Util().DownloadDatagramByName1("abccc");
//                    System.out.println("download abccc");
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
            System.out.println("in onbindviewholder position is "+position);
            listView = holder.itemView.findViewById(R.id.list);
            refresh_lv0();
//            Button button = holder.itemView.findViewById(R.id.button);

//            button.setOnClickListener(new View.OnClickListener() {
//                @SuppressLint("StaticFieldLeak")
//                @Override
//                public void onClick(View v) {
//                    String login_id = "1";
//                    new AsyncTask<String, Integer, String>() {
//
//
//                        @Override
//                        protected String doInBackground(String... arg0) {
//                            String res = new Util().GetAvailableDatagrams("id");
//                            return res;
//                        }
//
//                        protected void onPostExecute(String result) {
//                            if (result != null) {
//                                Toast.makeText(parent.getContext(), "刷新完成", Toast.LENGTH_SHORT).show();
//                                updateListView0(result);
//                            } else {
//                                Toast.makeText(parent.getContext(), "刷新失败，检查网络", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }.execute("1");
//                }
//            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                @SuppressLint("StaticFieldLeak")
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    System.out.println("position "+position);
                    String name = ((HashMap<String, String>)(listView.getItemAtPosition(position))).get("name");
                    System.out.println(name);
                    new AsyncTask<String, Integer, String>(){
                        @Override
                        protected String doInBackground(String... arg0){
                            String res = new Util().DownloadDatagramByName1(arg0[0]);
                            return res;
                        }
                        protected void onPostExecute(String result) {
                            if (result != null) {
                                MDToast.makeText(parent.getContext(), "下载完成", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                                System.out.println(result);
                            }else{
                                MDToast.makeText(parent.getContext(), "下载失败，网络或远程服务器故障", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                            }
                        }
                    }.execute(name);
                    }
                });
            }
            else {
                swipeRefreshLayout = holder.itemView.findViewById(R.id.swiperFresh);
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refresh_lv1();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
                listView1 = holder.itemView.findViewById(R.id.list);
                System.out.println("in onbindviewholder position is " + position);
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
                    dialog.setTitle("选择操作");
                    System.out.println("before setting listener");
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            MDToast.makeText(parent.getContext(), "上传成功", Utils.dura_short, Utils.Type_success).show();
//                            Toast.makeText(parent.getContext(), "上传成功", Toast.LENGTH_SHORT).show();
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
                                MDToast.makeText(parent.getContext(), "上传完成", Utils.dura_short, Utils.Type_success).show();
//                                Toast.makeText(parent.getContext(), "上传完成",Toast.LENGTH_SHORT).show();
                                System.out.println(result);
                            }else{
//                                Toast.makeText(parent.getContext(), "上传失败，检查网络或服务器",Toast.LENGTH_SHORT).show();
                                MDToast.makeText(parent.getContext(), "上传失败，检查网络或服务器", Utils.dura_short, Utils.Type_warning).show();

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

                    final String[] choices = new String[]{"预览", "删除"};

                    alertDialog = new AlertDialog.Builder(parent.getContext())
                            .setTitle("操作"+name)
                            .setIcon(R.drawable.ic_logo_pytorch)
                            .setItems(choices, new DialogInterface.OnClickListener() {//添加列表
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(i==0)//预览
                                    {
                                        //TODO call media here
                                        File file = new File(new Util().datagram_path , name);
                                        System.out.println("in onclick video path is " + file.getAbsolutePath());
                                        Intent intent = new Intent(Intent.ACTION_VIEW);


                                        if (file.exists()){
                                            Uri contentUri = FileProvider.getUriForFile(parent.getContext(), "authority", file);
//                                            Uri uri = Uri.fromFile(file);
                                            parent.getContext().grantUriPermission(parent.getContext().getPackageName(), contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                            String type = Utils.getMIMEType(file);
                                            System.out.println("type is " + type);
                                            System.out.println("uri is " + contentUri.getPath());
                                            intent.setDataAndType(contentUri, type);
                                            parent.getContext().startActivity(intent);
                                            MDToast.makeText(parent.getContext(), "成功打开", Utils.dura_short, Utils.Type_success).show();
                                        }else{
                                            MDToast.makeText(parent.getContext(), "文件不存在，文件路径错误", Utils.dura_short, Utils.Type_error).show();
                                        }
                                    }
                                    else if (i == 1)//删除
                                    {
                                        new AlertDialog.Builder(parent.getContext())
                                                .setTitle("删除 " + name)
                                                .setMessage("是否确定删除 "+ name)

                                                // Specifying a listener allows you to take an action before dismissing the dialog.
                                                // The dialog is automatically dismissed when a dialog button is clicked.
                                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // Continue with delete operation
                                                        new Util().delete_datagram(name);
                                                        MDToast.makeText(parent.getContext(), "文件已删除", Utils.dura_short, Utils.Type_success).show();
                                                    }
                                                })

                                                // A null listener allows the button to dismiss the dialog and take no further action.
                                                .setNegativeButton(android.R.string.no, null)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .show();

                                    }
                                }
                            })
                            .create();

                    alertDialog.show();
                }
            });

            refresh_lv1();

            }

    }

    private void refresh_lv1() {
        String[] filenames = new Util().GetLocalDatagrams();
        MDToast.makeText(parent.getContext(), "刷新完成", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
        updateListView1(filenames);
    }

    @SuppressLint("StaticFieldLeak")
    private void refresh_lv0() {
        String login_id = "admin";
        new AsyncTask<String, Integer, String>() {


            @Override
            protected String doInBackground(String... arg0) {
                String res = new Util().getAvailableDatagrams(arg0[0]);
                return res;
            }

            protected void onPostExecute(String result) {
                if (result != null) {
                    MDToast.makeText(parent.getContext(), "刷新完成", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
                    updateListView0(result);
                } else {
                    MDToast.makeText(parent.getContext(), "刷新失败，网络或远程服务器故障", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR).show();
                }
            }
        }.execute(login_id);
    }

    public void updateListView1(String[] filenames){
        if (filenames.length == 0){
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
//            int count = jsonObject.getInt("datagram_len");
//            int count = jsonObject.length();

            JSONArray jsonArrayn = jsonObject.getJSONArray("datagram_name");
            JSONArray jsonArraym = jsonObject.getJSONArray("datagram_describe");
            int count = jsonArrayn.length();
//            JSONArray jsonArrays = jsonObject.getJSONArray("datagram site");

            for (int i = 0; i < count; i++){
                Map<String, String> map= new HashMap<>();
                map.put("name", jsonArrayn.getString(i));
                map.put("describe", jsonArraym.getString(i));
//                map.put("site", jsonArrays.getString(i));
                list.add(map);
            }
        }catch (JSONException jsonException){
            jsonException.printStackTrace();
        }

        SimpleAdapter adapter = new SimpleAdapter(parent.getContext(),
                list,
                R.layout.datagram_list_item,
                new String[]{"name", "describe"},
                new int[]{R.id.text1, R.id.text2});

        listView.setAdapter(adapter);

        


    }
    @Override
    public int getItemCount() {
        return 2;
    }

    public DatagramSelectAdapter() {
        super();
    }

    public class ViewPagerViewHolder extends RecyclerView.ViewHolder{
        public ViewPagerViewHolder(View itemView){
            super(itemView);
        }
    }

}
