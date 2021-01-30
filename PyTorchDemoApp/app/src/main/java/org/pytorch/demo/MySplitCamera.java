package org.pytorch.demo;


import android.content.Context;
import android.os.Environment;

import com.madgaze.smartglass.otg.BaseCamera;

import org.pytorch.demo.util.Util;

import java.io.File;

public class MySplitCamera extends BaseCamera {
    private static MySplitCamera INSTANCE = null;

    public static MySplitCamera getInstance(Context context) {
        if (context != mContext) {
            if (INSTANCE != null) {
                INSTANCE.destroyInstance();
                INSTANCE = null;
            }

            INSTANCE = createInstance(context);
        }

        return INSTANCE;
    }

    private static MySplitCamera createInstance(Context context) {
        MySplitCamera instance = new MySplitCamera(context);
        return instance;
    }

    public MySplitCamera(Context context) {
        super(context);
    }


    @Override
    public void startRecording() {
        File video_dir = new File(new Util().video_path);
        if (!video_dir.exists()){
            video_dir.mkdir();
        }
        this.startRecording(video_dir.getAbsolutePath(), 0, true);
    }

}
