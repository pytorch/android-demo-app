package org.pytorch.demo;

import android.app.Activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.pytorch.demo.util.Util;

import java.io.IOException;
import java.lang.reflect.Array;

public class SetupMenuActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    AppCompatEditText rtmp_uri;
    AppCompatEditText server_uri;
    CheckBox checkBox;
    RadioGroup encode;
    Spinner spinner_resolution;
    Spinner spinner_fps;
    int fps_selected;
    String resolution_selected;
    AppCompatEditText et_token;

    @Override
    public void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setupmenu);

        Util util = new Util();
//        R.layout.activity_face_detection2
        rtmp_uri = findViewById(R.id.et_rtmp_addr);
        rtmp_uri.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        server_uri = findViewById(R.id.et_server_addr);
        server_uri.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et_token = findViewById(R.id.et_token);
        et_token.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et_token.setEnabled(false);

        checkBox = findViewById(R.id.save_video);
        encode = findViewById(R.id.encode_method);

        spinner_fps = findViewById(R.id.spinner_fps);
        ArrayAdapter<CharSequence> adapter_fps = ArrayAdapter.createFromResource(this,
                R.array.fps_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter_fps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_fps.setAdapter(adapter_fps);

        spinner_resolution = findViewById(R.id.spinner_resolution);
        ArrayAdapter<CharSequence> adapter_resolutions = ArrayAdapter.createFromResource(this,
                R.array.resolutions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter_resolutions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner_resolution.setAdapter(adapter_resolutions);

        load_setting();


//        System.out.println("in setup menu "+util.ws+util.server_uri);
        Button save = findViewById(R.id.Save);
        save.setOnClickListener(v -> {
            try {
                save_setting();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            MDToast.makeText(SetupMenuActivity.this, "保存成功", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS).show();
        });

        findViewById(R.id.Return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetupMenuActivity.this.finish();
            }
        });
    }

    public void save_setting() throws IOException {
        String method = null;
        int checked_id = encode.getCheckedRadioButtonId();
        if (checked_id == R.id.method_software){
            method = "soft";
        }else if (checked_id == R.id.method_hardware){
            method = "hard";
        }

        SettingContent settingContent = new SettingContent(
                rtmp_uri.getText().toString(),
                server_uri.getText().toString(),
                checkBox.isChecked(),
                method,
                resolution_selected,
                fps_selected
        );

        Gson gson = new Gson();
        String setting = gson.toJson(settingContent);
        System.out.println(setting);

        new Util().SetLocalJson(setting);

    }
    public void load_setting(){



        try {
            String setting = new Util().GetLocalJson().toString();
            Gson gson = new Gson();
            SettingContent settingContent = gson.fromJson(setting, SettingContent.class);
            rtmp_uri.setText(settingContent.getRtmp_addr());
            server_uri.setText(settingContent.getServer_addr());
            et_token.setText(settingContent.getToken());
            checkBox.setChecked(settingContent.save_file);

            RadioButton radioButton;
            if (settingContent.decode_method.equals("soft")) {
                radioButton = findViewById(R.id.method_software);
            } else {
                radioButton = findViewById(R.id.method_hardware);
            }
            radioButton.setChecked(true);

            Resources r = getResources();
            String[] resolution_array = r.getStringArray(R.array.resolutions_array);
            int res_selection = 0;
            for (; res_selection < resolution_array.length; res_selection++) {
                if (resolution_array[res_selection].equals(settingContent.getResolution())) {
                    spinner_resolution.setSelection(res_selection);
                    break;
                }
            }

            int[] fps_array = r.getIntArray(R.array.fps_array);
            int fps_selection = 0;
            for (; fps_selection < fps_array.length; fps_selection++) {
                if (fps_array[fps_selection] == settingContent.getFps()) {
                    spinner_fps.setSelection(fps_selection);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(view.getId() == R.id.spinner_fps){
            Resources r = getResources();
            int[] fps_array = r.getIntArray(R.array.fps_array);
            fps_selected = fps_array[position];
        }else if (view.getId() == R.id.spinner_resolution){
            Resources r = getResources();
            String[] resolution_array = r.getStringArray(R.array.resolutions_array);
            resolution_selected = resolution_array[position];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
            fps_selected = 30;
            resolution_selected = "800x600";
    }
}
