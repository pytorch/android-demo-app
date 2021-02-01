
package org.pytorch.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {

    //声明控件
    private SharedPreferences sp;//声明SharedPreferences
    private Button offLogin;
    private EditText mEtUser;
    private EditText mEtPassword;
    private Toast mtoast;

    private Button online_loginbtn;
//    private Button offline_loginbtn;

//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//    }
//
//    private static class PageData {
//        private int titleTextResId;
//        private int imageResId;
//        private int descriptionTextResId;
//
//        public PageData(int titleTextResId, int imageResId, int descriptionTextResId) {
//            this.titleTextResId = titleTextResId;
//            this.imageResId = imageResId;
//            this.descriptionTextResId = descriptionTextResId;
//        }
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //找到控件
        offLogin = findViewById(R.id.btn_offlinelogin);
        mEtUser = findViewById(R.id.et_1);
        mEtUser.setText("admin");
        mEtPassword = findViewById(R.id.et_2);
        mEtPassword.setText("123456");
        offLogin.setOnClickListener(this);
        saveUser();

        online_loginbtn = findViewById(R.id.btn_onlinelogin);
        //重写点击事件的处理方法onClick()
        online_loginbtn.setOnClickListener(v -> {
            //显示Toast信息
            Toast.makeText(getApplicationContext(), "你点击了按钮", Toast.LENGTH_SHORT).show();
        });


//        findViewById(R.id.skip_button).setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, MainActivity.class)));
//
//        mViewPager = findViewById(R.id.welcome_view_pager);
//        mViewPagerAdapter = new WelcomeViewPagerAdapter();
//        mViewPager.setAdapter(mViewPagerAdapter);
//
//        mTabLayout = findViewById(R.id.welcome_tab_layout);
//        mTabLayout.setupWithViewPager(mViewPager);
    }


    public void onClick(View v) {
        //获取输入的用户名和密码
        String username = mEtUser.getText().toString();
        String password = mEtPassword.getText().toString();
        Intent intent = null;
        if (username.equals("") || password.equals("")) {
            //账户密码不能为空
            showToast("账号或密码不能为空");
        }
        String savedUsername = sp.getString("username", "");
        String savedPassword = sp.getString("password", "");
        if (username.equals(savedUsername) && password.equals(savedPassword)) {
//             if new Util().sss === success
            intent = new Intent(WelcomeActivity.this, SideBarActivity.class);
            String res = sp.getString("username","none");
            intent.putExtra("info", res);
            showToast("你好！"+res);
            startActivity(intent);

//            intent = new Intent(WelcomeActivity.this, SelectVideos.class);
//            startActivity(intent);
        } else {
            showToast("用户名或密码错误，请重新登录");

        }

    }



    private void showToast(String msg) {
        mtoast = Toast.makeText(WelcomeActivity.this, msg, Toast.LENGTH_SHORT);
        mtoast.show();
    }

    private void saveUser() {
        // TODO Auto-generated method stub
        sp = getSharedPreferences("info", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("username", "admin");
        edit.putString("password", "123456");
        edit.commit();
    }

}
