package org.pytorch.demo;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

//import androidx.fragment.app.Fragment;

public class ContentfileActivity extends Fragment {

    //创建视图
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        

        return inflater.inflate( R.layout.content_file, container, false );  //要加载的layout文件
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        activity.findViewById(R.id.local_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfileActivity.this.getActivity(), SelectFaceDatagram.class));
            }
        });
        activity.findViewById(R.id.local_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfileActivity.this.getActivity(), SelectVideos.class));
            }
        });
        activity.findViewById(R.id.checkandupdate_person).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfileActivity.this.getActivity(), SelectCrew.class));
            }
        });

    }

}
