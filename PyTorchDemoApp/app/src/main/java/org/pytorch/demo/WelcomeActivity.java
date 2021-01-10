
package org.pytorch.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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


    //    private ViewPager mViewPager;
//    private PagerAdapter mViewPagerAdapter;
//    private TabLayout mTabLayout;
    private Button online_loginbtn;
//    private Button offline_loginbtn;

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private static class PageData {
        private int titleTextResId;
        private int imageResId;
        private int descriptionTextResId;

        public PageData(int titleTextResId, int imageResId, int descriptionTextResId) {
            this.titleTextResId = titleTextResId;
            this.imageResId = imageResId;
            this.descriptionTextResId = descriptionTextResId;
        }
    }
//
//    private static final PageData[] PAGES = new PageData[] {
//            new PageData(
//                    R.string.welcome_page_title,
//                    R.drawable.ic_logo_pytorch,
//                    R.string.welcome_page_description),
//            new PageData(
//                    R.string.welcome_page_image_classification_title,
//                    R.drawable.ic_image_classification_l,
//                    R.string.welcome_page_image_classification_description),
//            new PageData(
//                    R.string.welcome_page_nlp_title,
//                    R.drawable.ic_text_classification_l,
//                    R.string.welcome_page_nlp_description)
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //找到控件
        offLogin = findViewById(R.id.btn_offlinelogin);
        mEtUser = findViewById(R.id.et_1);
        mEtPassword = findViewById(R.id.et_2);
        offLogin.setOnClickListener(this);


        online_loginbtn = findViewById(R.id.btn_onlinelogin);
        //重写点击事件的处理方法onClick()
        online_loginbtn.setOnClickListener(v -> {
            //显示Toast信息
            Toast.makeText(getApplicationContext(), "你点击了按钮", Toast.LENGTH_SHORT).show();
        });

        saveuser();

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
            showToast("账号密码不能为空");
        }
        String savedUsername = sp.getString("username","");
        String savedPassword = sp.getString("password","");
        if (username.equals(savedUsername) && password.equals(savedPassword)) {
            showToast("登陆成功！");
            intent = new Intent(WelcomeActivity.this, SideBarActivity.class);
            startActivity(intent);
        } else {
            showToast("用户名或密码错误，请重新登录");
        }
    }


//    private class WelcomeViewPagerAdapter extends PagerAdapter {
//        @Override
//        public int getCount() {
//            return PAGES.length;
//        }
//
//        @Override
//        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
//            return object == view;
//        }

//        @NonNull
//        public Object instantiateItem(@NonNull ViewGroup container, int position) {
//            final LayoutInflater inflater = LayoutInflater.from(WelcomeActivity.this);
//            final View pageView = inflater.inflate(R.layout.welcome_pager_page, container, false);
//            final TextView titleTextView = pageView.findViewById(R.id.welcome_pager_page_title);
//            final TextView descriptionTextView = pageView.findViewById(R.id.welcome_pager_page_description);
//            final ImageView imageView = pageView.findViewById(R.id.welcome_pager_page_image);
//
//            final PageData pageData = PAGES[position];
//            titleTextView.setText(pageData.titleTextResId);
//            descriptionTextView.setText(pageData.descriptionTextResId);
//            imageView.setImageResource(pageData.imageResId);
//            container.addView(pageView);
//            return pageView;
//        }

//        @Override
//        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//            container.removeView((View) object);
//        }
//    }


    private void showToast(String msg){
        mtoast=Toast.makeText(WelcomeActivity.this,msg,Toast.LENGTH_SHORT);
        mtoast.show();
    }

    private void saveuser() {
        // TODO Auto-generated method stub
        sp =getSharedPreferences("info", Activity.MODE_PRIVATE);
        SharedPreferences.Editor edit=sp.edit();
        edit.putString("username", "admin");
        edit.putString("password", "123456");
        edit.apply();
    }

//        public void get_sidebar(View view) {
//            startActivity(new Intent(WelcomeActivity.this, SideBarActivity.class));
//        }
}
