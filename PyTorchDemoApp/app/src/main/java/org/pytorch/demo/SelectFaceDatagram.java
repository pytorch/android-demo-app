package org.pytorch.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

public class SelectFaceDatagram extends AppCompatActivity {


    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    DatagramSelectAdapter datagramSelectAdapter;
    private static final int REQUEST_CODE_FILE_PERMISSION = 204;
    private static final String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_face_datagram);
        datagramSelectAdapter = new DatagramSelectAdapter();
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(datagramSelectAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText("OBJECT " + (position + 1))
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

