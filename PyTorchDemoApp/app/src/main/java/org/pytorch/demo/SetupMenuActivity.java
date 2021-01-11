package org.pytorch.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import org.pytorch.demo.util.Util;

public class SetupMenuActivity extends AppCompatActivity {
    private String ws;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setupmenu);
        ws = Util.ws;
        AppCompatEditText view = findViewById(R.id.Input1);
        view.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        view.setText(ws);

    }
    public void selectfaceDatagram(View view){
        startActivity(new Intent(SetupMenuActivity.this, SelectFaceDatagram.class));
    }
}
