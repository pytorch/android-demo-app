package org.pytorch.demo;

import android.app.Activity;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;

import org.pytorch.demo.util.Util;

import java.io.IOException;

public class SetupMenuActivity extends AppCompatActivity {


    @Override
    public void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setupmenu);

        Util util = new Util(this);

        AppCompatEditText rtmp_uri = findViewById(R.id.Input2);
        rtmp_uri.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        rtmp_uri.setText(util.ws);
        AppCompatEditText server_uri = findViewById(R.id.Input1);
        server_uri.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        server_uri.setText(util.server_uri);

        System.out.println("in setup menu "+util.ws+util.server_uri);
        Button save = findViewById(R.id.Save);
        save.setOnClickListener(v -> {
            try {
                new Util().SetLocalJson("{\"rtmp_uri\": \""+rtmp_uri.getText().toString()+"\", \"server_uri\": \""+server_uri.getText().toString()+"\"}");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            Toast.makeText(SetupMenuActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
//                edit.putString("rtmp_uri", rtmp_uri.getText().toString());
//                edit.putString("server_uri", server_uri.getText().toString());
        });

        findViewById(R.id.Return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetupMenuActivity.this.finish();
            }
        });
    }




}
