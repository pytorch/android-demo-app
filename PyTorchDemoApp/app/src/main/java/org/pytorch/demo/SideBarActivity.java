package org.pytorch.demo;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.pytorch.demo.nlp.NLPListActivity;
import org.pytorch.demo.vision.VisionListActivity;

public class SideBarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sidemenu);

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

        //寻找头部里面的控件
        ImageView imageView = headerView.findViewById(R.id.iv_head);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "点击了头像", Toast.LENGTH_LONG).show();
            }
        });
        navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        });
        ColorStateList csl = (ColorStateList) getResources().getColorStateList(R.color.nav_menu_text_color);
        //设置item的条目颜色
        navigationview.setItemTextColor(csl);
        //去掉默认颜色显示原来颜色  设置为null显示本来图片的颜色
        navigationview.setItemIconTintList(csl);



        //设置条目点击监听
        navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                //安卓
                Toast.makeText(getApplicationContext(), menuItem.getTitle(), Toast.LENGTH_LONG).show();
                //设置哪个按钮被选中
                menuItem.setChecked(true);
                //关闭侧边栏
                drawer.closeDrawers();
                return false;
            }
        });

        findViewById(R.id.main_vision_click_view).setOnClickListener(v -> startActivity(new Intent(SideBarActivity.this, VisionListActivity.class)));
        findViewById(R.id.main_nlp_click_view).setOnClickListener(v -> startActivity(new Intent(SideBarActivity.this, NLPListActivity.class)));
    }

    //进入设置页面
    public void SetupMenu(View view) {
        startActivity(new Intent(SideBarActivity.this,SetupMenuActivity.class));
    }
}





//设置单个消息数量
//        LinearLayout llAndroid = (LinearLayout) navigationview.getMenu().findItem(R.id.single_1).getActionView();
//        TextView msg= (TextView) navigationview.getMenu().findItem(R.id.msg_bg);
//        msg.setText("李龙");