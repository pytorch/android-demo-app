package org.pytorch.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.view.View;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

public class SelectCrew extends AppCompatActivity {


    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    CrewSelectAdapter crewSelectAdapter;
    private static final int REQUEST_CODE_FILE_PERMISSION = 204;
    private static final String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_crew);
        crewSelectAdapter = new CrewSelectAdapter();
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(crewSelectAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if(position == 1)
                    {
                        tab.setText("本地添加人员数据");
                    }
                    else
                        tab.setText("云端下载人员数据");
                }
        ).attach();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_FILE_PERMISSION);
        }


    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FILE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't use check datagrams without granting INTERNET permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                        this,
                        "You can't store datagrams without granting Write external storage permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            else if (grantResults[2] == PackageManager.PERMISSION_DENIED)
            {
                Toast.makeText(
                        this,
                        "You can't check datagrams without granting read external storage permission",
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }

        }
    }
}