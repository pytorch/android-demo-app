package org.pytorch.demo;

import android.annotation.SuppressLint;

import android.content.Intent;

import android.content.res.ColorStateList;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.pytorch.demo.nlp.NLPListActivity;
import org.pytorch.demo.vision.VisionListActivity;

public class SideBarActivity extends AppCompatActivity {


//    private  SharedPreferences sp = getSharedPreferences("info", Context.MODE_PRIVATE);

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Intent i = this.getIntent();
        String info = i.getStringExtra("info");
        super.onCreate(savedInstanceState);
        View view = this.getLayoutInflater().inflate(R.layout.activity_sidemenu,null);
        setContentView(view);
        init_function_button();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        NavigationView navigationview = (NavigationView) findViewById(R.id.navigation_view);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        setSupportActionBar(toolbar);//将toolbar与ActionBar关联
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.setDrawerListener(toggle);//初始化状态
        toggle.syncState();


        /*---------------------------添加头布局和尾布局-----------------------------*/
        //获取xml头布局view
        View headerView = navigationview.getHeaderView(0);
        //添加头布局的另外一种方式
        //View headview=navigationview.inflateHeaderView(R.layout.navigationview_header);


        navigationview.setNavigationItemSelectedListener(menuItem -> false);
        ColorStateList csl = getResources().getColorStateList(R.color.nav_menu_text_color);
        //设置item的条目颜色
        navigationview.setItemTextColor(csl);
        //去掉默认颜色显示原来颜色  设置为null显示本来图片的颜色
        navigationview.setItemIconTintList(csl);

        //登录组件传值
        TextView msg = navigationview.findViewById(R.id.cname);
        msg.setText(info);

        //寻找头部里面的控件
        androidx.appcompat.widget.AppCompatImageView imageView = view.findViewById(R.id.iv_head);
        imageView.setOnClickListener(v -> Toast.makeText(getApplicationContext(), msg.getText(), Toast.LENGTH_LONG).show());

        //设置条目点击监听
        navigationview.setNavigationItemSelectedListener(menuItem -> {
            //安卓
            Toast.makeText(getApplicationContext(), menuItem.getTitle(), Toast.LENGTH_LONG).show();
            //设置哪个按钮被选中
            menuItem.setChecked(true);
            //关闭侧边栏
            drawer.closeDrawers();
            return false;
        });

//        findViewById(R.id.main_vision_click_view).setOnClickListener(v -> startActivity(new Intent(SideBarActivity.this, VisionListActivity.class)));
//        findViewById(R.id.main_nlp_click_view).setOnClickListener(v -> startActivity(new Intent(SideBarActivity.this, NLPListActivity.class)));
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_func);
    }
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @SuppressLint({"ResourceType", "NonConstantResourceId"})
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentManager fragmentManager = getSupportFragmentManager();  //使用fragmentmanager和transaction来实现切换效果
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            switch (item.getItemId()) {
                case R.id.navigation_func:
                    transaction.replace(R.id.content,new ContentfuncActivity());  //对应的java class
                    transaction.commit();  //一定不要忘记commit，否则不会显示
                    return true;
                case R.id.navigation_file:
                    transaction.replace(R.id.content,new ContentfileActivity());  //对应的java class
                    transaction.commit();  //一定不要忘记commit，否则不会显示
                    return true;

                case R.id.navigation_sett:
                    transaction.replace(R.id.content,new SetupMenuActivity());
                    transaction.commit();
//                    transaction.replace(R.id.content,new ContentfileActivity());
//                    transaction.commit();
                    return true;

            }
            return false;
        }
    };
    public void init_function_button(){
        findViewById(R.id.button_datagram).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SideBarActivity.this, SelectFaceDatagram.class));
            }
        });

        findViewById(R.id.button_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SideBarActivity.this, SelectVideos.class));
            }
        });
    }
    //进入设置页面
    public void SetupMenu(View view) {
        startActivity(new Intent(SideBarActivity.this,SetupMenuActivity.class));
    }

    //退出
    public void exit(View view){
        startActivity(new Intent(SideBarActivity.this,WelcomeActivity.class));

    }
}

