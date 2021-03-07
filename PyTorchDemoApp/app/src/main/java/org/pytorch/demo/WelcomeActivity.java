
package org.pytorch.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WelcomeActivity extends AppCompatActivity{

    //声明控件
    private SharedPreferences sp;//声明SharedPreferences
    private Button offLogin;
    private EditText mEtUser;
    private EditText mEtPassword;
    private Toast mtoast;
    private Button AccountLogin;
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

        AccountLogin = findViewById(R.id.btn_AccountLogin);
        AccountLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, AccountLoginActivity.class);
                startActivity(intent);
            }
        });
//        mEtUser = findViewById(R.id.et_1);
//        mEtUser.setText("admin");
//        mEtPassword = findViewById(R.id.et_2);
//        mEtPassword.setText("123456");
//        AccountLogin.setOnClickListener(this);

//        offLogin = findViewById(R.id.btn_OnlineLogin);
//        mEtUser = findViewById(R.id.et_1);
//        mEtUser.setText("002");
//        mEtPassword = findViewById(R.id.et_2);
//        mEtPassword.setText("002");
//        offLogin.setOnClickListener(this);

//        saveUser();


        online_loginbtn = findViewById(R.id.btn_OnlineLogin);
        //重写点击事件的处理方法onClick()
        online_loginbtn.setOnClickListener(v -> {
            //显示Toast信息
            Toast.makeText(getApplicationContext(), "你点击了按钮", Toast.LENGTH_SHORT).show();
//            check_password();
            Intent intent = new Intent(WelcomeActivity.this, SideBarActivity.class);
            startActivity(intent);
        });

        requestPermission_rfda();


//        findViewById(R.id.skip_button).setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, GlassRemoteActivity.class)));
//
//        mViewPager = findViewById(R.id.welcome_view_pager);
//        mViewPagerAdapter = new WelcomeViewPagerAdapter();
//        mViewPager.setAdapter(mViewPagerAdapter);
//
//        mTabLayout = findViewById(R.id.welcome_tab_layout);
//        mTabLayout.setupWithViewPager(mViewPager);
    }

    public final static int RC_CAMERA = 100;
    private void requestPermission_rfda() {
        //1. 检查是否已经有该权限
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {
            //2. 权限没有开启，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_CAMERA);
        }else{
            System.out.println("in request permission before init");

        }

    }
    //3. 接收申请成功或者失败回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {

                //权限被用户拒绝，做相应的事情
                finish();
            }
            for (int i = 0; i < permissions.length; i++)
            {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    Toast.makeText(this, permissions[i] + "权限没有成功获取", Toast.LENGTH_LONG);
                    finish();
                }
            }
        }
    }





//    private String sendByOKHttp() {
//        String result = null;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder().url("https://www.baidu.com").build();
//                try {
//                    Response response = client.newCall(request).execute();//发送请求
//                    result = response.body().string();
//                    Log.d("tag in send ok http ", "result: " + result);
//                    System.out.println("in run result is ");
//                    System.out.println(result);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        private void setResult(String res){
//            result = res;
//            }
//    }


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
