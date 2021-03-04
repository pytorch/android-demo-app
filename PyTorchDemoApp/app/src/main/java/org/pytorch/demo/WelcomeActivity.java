
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
    private Button AccountLogin;
    private EditText mEtUser;
    private EditText mEtPassword;
    private Toast mtoast;

    private Button face_loginbtn;
    //    private Button offline_loginbtn;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //找到控件
        AccountLogin = findViewById(R.id.btn_AccountLogin);
        mEtUser = findViewById(R.id.et_1);
        mEtUser.setText("admin");
        mEtPassword = findViewById(R.id.et_2);
        mEtPassword.setText("123456");
//        AccountLogin.setOnClickListener(this);
//        saveUser();
//        AccountLogin.setOnClickListener(v -> {
//            startActivity(new Intent(String.valueOf(AccountLoginActivity.class)));
//        });
        face_loginbtn = findViewById(R.id.btn_OnlineLogin);
        //重写点击事件的处理方法onClick()
        face_loginbtn.setOnClickListener(v -> {
            //显示Toast信息
            Toast.makeText(getApplicationContext(), "开始识别", Toast.LENGTH_SHORT).show();
        });




    }


    public void onClick(View v) {
        //获取输入的用户名和密码
//        String username = mEtUser.getText().toString();
//        String password = mEtPassword.getText().toString();
//        Intent intent = null;
//        if (username.equals("") || password.equals("")) {
//            //账户密码不能为空
//            showToast("账号或密码不能为空");
//        }
////        String savedUsername = sp.getString("username", "");
////        String savedPassword = sp.getString("password", "");
//        String savedUsername = "admin";
//        String savedPassword = "123456";
//        if (username.equals(savedUsername) && password.equals(savedPassword)) {
////             if new Util().sss === success
//            intent = new Intent(WelcomeActivity.this, AccountLoginActivity.class);
////            String res = sp.getString("username","none");
//            String res = "admin";
//            intent.putExtra("info", res);
//            showToast("你好！"+res);
//            startActivity(intent);

//            intent = new Intent(WelcomeActivity.this, SelectVideos.class);
//            startActivity(intent);
//            startActivity(new Intent(String.valueOf(AccountLoginActivity.class)));
//        } else {
//            showToast("用户名或密码错误，请重新登录");
//
//        }

    }
    //进入设置页面
    public void AccountLogin(View view) {
        startActivity(new Intent(WelcomeActivity.this,AccountLoginActivity.class));
    }



    private void showToast(String msg) {
        mtoast = Toast.makeText(WelcomeActivity.this, msg, Toast.LENGTH_SHORT);
        mtoast.show();
    }

//    private void saveUser() {
//        // TODO Auto-generated method stub
//        sp = getSharedPreferences("info", Activity.MODE_PRIVATE);
//        SharedPreferences.Editor edit = sp.edit();
//        edit.putString("username", "admin");
//        edit.putString("password", "123456");
//        edit.commit();
//    }

}
