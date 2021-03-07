package org.pytorch.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MinePageActivity extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        return inflater.inflate( R.layout.content_sett, container, false );  //要加载的layout文件
    }
    /**
     *fragment的控件操作通过父Activity操作，且在onActivityCreated中。
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //和以前一样，在这里操作点击我的账户，使用帮助，修改密码等功能进行控件跳转。

        View set = getActivity().findViewById(R.id.setting);
        set.setOnClickListener(v -> {
            startActivity(new Intent(MinePageActivity.this.getActivity(),SetupMenuActivity.class));
        });

    }
}
