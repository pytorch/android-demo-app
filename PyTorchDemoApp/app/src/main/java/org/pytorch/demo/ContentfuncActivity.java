package org.pytorch.demo;


import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.pytorch.demo.util.Util;


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

        RadioButton local_recog = getActivity().findViewById(R.id.local_recog);
        RadioButton remote_recog = getActivity().findViewById(R.id.remote_recog);
        setBounds(R.mipmap.phonemode,local_recog);
        setBounds(R.mipmap.phonemode,remote_recog);
        RadioButton glass_remote_recog = getActivity().findViewById(R.id.glass_remote_recog);
        RadioButton glass_local_recog = getActivity().findViewById(R.id.glass_local_recog);
        setBounds(R.mipmap.glassmode,glass_remote_recog);
        setBounds(R.mipmap.glassmode,glass_local_recog);
        RadioButton shipname_detect = getActivity().findViewById(R.id.shipname_detect);
        RadioButton shipclass_detect = getActivity().findViewById(R.id.shipclass_detect);
        setBounds(R.mipmap.ship,shipname_detect);
        setBounds(R.mipmap.ship,shipclass_detect);
        RadioButton add_crew  = getActivity().findViewById(R.id.btn_add_crew);
        RadioButton other_func = getActivity().findViewById(R.id.btn_other);
        setBounds(R.mipmap.account,add_crew);
        setBounds(R.mipmap.account,other_func);
        Activity baseActivity = getActivity();
        Button local_button = baseActivity.findViewById(R.id.local_recog);
        Button remote_button = baseActivity.findViewById(R.id.remote_recog);
        local_button.setOnClickListener(v -> startActivity(new Intent(ContentfuncActivity.this.getActivity(),FaceDetectionActivity2.class)));
        remote_button.setOnClickListener(v -> startActivity(new Intent(ContentfuncActivity.this.getActivity(),RemoteFaceDetectActivity.class)));
        baseActivity.findViewById(R.id.glass_local_recog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfuncActivity.this.getActivity(), GlassLocalActivity.class));
            }
        });
        baseActivity.findViewById(R.id.glass_remote_recog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfuncActivity.this.getActivity(), GlassRemoteActivity.class));
            }
        });
        add_crew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContentfuncActivity.this.getActivity(), AddNewCrew.class));
            }
        });
        shipname_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                new Util().resetPassword("001", "00x");
                new Util().deleteCrewByName("name");
            }
        });

    }

    /**
     *
     * @param drawableId  drawableLeft  drawableTop drawableBottom 所用的选择器 通过R.drawable.xx 获得
     * @param radioButton  需要限定图片大小的radioButton
     */
    private void setBounds(int drawableId, RadioButton radioButton) {
        //定义底部标签图片大小和位置
        Drawable drawable_news = getResources().getDrawable(drawableId);
        //当这个图片被绘制时，给他绑定一个矩形 ltrb规定这个矩形  (这里的长和宽写死了 自己可以可以修改成 形参传入)
        drawable_news.setBounds(0, 0, 95, 95);
        //设置图片在文字的哪个方向
        radioButton.setCompoundDrawables(null,drawable_news,null, null);
    }



}
