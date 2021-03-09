package org.pytorch.demo;


import android.annotation.SuppressLint;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.demo.util.Util;

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
                            String res = new Util().DownloadDatagramByName(arg0[0]);
                            return res;
                        }
                        protected void onPostExecute(String result) {
                            if (result != null) {
                                Toast.makeText(parent.getContext(), "下载完成", Toast.LENGTH_SHORT).show();
                                System.out.println(result);
                            }else{
                                Toast.makeText(parent.getContext(), "下载失败，检查网络",Toast.LENGTH_SHORT).show();
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
                refresh_lv1();
            }

    }

    private void refresh_lv1() {
      String[] filenames = new Util().GetLocalDatagrams();
      updateListView1(filenames);
    }

    @SuppressLint("StaticFieldLeak")
    private void refresh_lv0() {
        String login_id = Utils.token;
        new AsyncTask<String, Integer, String>() {


            @Override
            protected String doInBackground(String... arg0) {
                String res = new Util().GetAvailableDatagrams(arg0[0]);
                return res;
            }

            protected void onPostExecute(String result) {
                if (result != null) {
                    Toast.makeText(parent.getContext(), "刷新完成", Toast.LENGTH_SHORT).show();
                    updateListView0(result);
                } else {
                    Toast.makeText(parent.getContext(), "刷新失败，检查网络", Toast.LENGTH_SHORT).show();
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

    public DatagramSelectAdapter() {
        super();
    }

    public class ViewPagerViewHolder extends RecyclerView.ViewHolder{
        public ViewPagerViewHolder(View itemView){
            super(itemView);
        }
    }

}
