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

import java.util.Objects;

public class ContentfuncActivity extends Fragment {

    //创建视图
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate( R.layout.content_func, container, false );  //要加载的layout文件
    }


    /**
     * 在这里才能操作fragment使用当前父Activity的控件。
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity baseActivity = getActivity();
        Button local_button = baseActivity.findViewById(R.id.local_recog);
        Button remote_button = baseActivity.findViewById(R.id.remote_recog);
        local_button.setOnClickListener(v -> startActivity(new Intent(ContentfuncActivity.this.getActivity(),FaceDetectionActivity.class)));
        remote_button.setOnClickListener(v -> startActivity(new Intent(ContentfuncActivity.this.getActivity(),RemoteFaceDetectActivity.class)));
    }




}
