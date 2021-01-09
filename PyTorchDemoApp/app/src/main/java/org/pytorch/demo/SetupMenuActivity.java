package org.pytorch.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SetupMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setupmenu);
    }
    public void selectfaceDatagram(View view){
        startActivity(new Intent(SetupMenuActivity.this, SelectFaceDatagram.class));
    }
}
