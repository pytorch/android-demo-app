package org.pytorch.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private PagerAdapter mViewPagerAdapter;
    private TabLayout mTabLayout;

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

    private static final PageData[] PAGES = new PageData[] {
            new PageData(
                    R.string.welcome_page_title,
                    R.drawable.ic_logo_pytorch,
                    R.string.welcome_page_description),
            new PageData(

                    R.string.welcome_page_image_classification_title,
                    R.drawable.ic_image_classification_l,
                    R.string.welcome_page_image_classification_description),
            new PageData(
                    R.string.welcome_page_nlp_title,
                    R.drawable.ic_text_classification_l,
                    R.string.welcome_page_nlp_description)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.skip_button).setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, MainActivity.class)));
//        findViewById(R.id.skip_button).setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, SelectFaceDatagram.class)));

        mViewPager = findViewById(R.id.welcome_view_pager);
        mViewPagerAdapter = new WelcomeViewPagerAdapter();
        mViewPager.setAdapter(mViewPagerAdapter);

        mTabLayout = findViewById(R.id.welcome_tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[] {Manifest.permission.INTERNET},
//                    2077);
//        }
    }

//    @Override
//    public void onRequestPermissionsResult(
//            int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == 2077) {
//            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't use image classification example without granting INTERNET permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//            else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't store video without granting Write external storage permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//            else if (grantResults[2] == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(
//                        this,
//                        "You can't record video without granting camera permission",
//                        Toast.LENGTH_LONG)
//                        .show();
//                finish();
//            }
//        }
//
//    }

    private class WelcomeViewPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return PAGES.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return object == view;
        }

        @NonNull
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            final LayoutInflater inflater = LayoutInflater.from(WelcomeActivity.this);
            final View pageView = inflater.inflate(R.layout.welcome_pager_page, container, false);
            final TextView titleTextView = pageView.findViewById(R.id.welcome_pager_page_title);
            final TextView descriptionTextView = pageView.findViewById(R.id.welcome_pager_page_description);
            final ImageView imageView = pageView.findViewById(R.id.welcome_pager_page_image);

            final PageData pageData = PAGES[position];
            titleTextView.setText(pageData.titleTextResId);
            descriptionTextView.setText(pageData.descriptionTextResId);
            imageView.setImageResource(pageData.imageResId);
            container.addView(pageView);
            return pageView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
