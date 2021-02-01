package org.pytorch.demo.vision;

import android.content.Intent;
import android.media.FaceDetector;
import android.os.Bundle;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.FaceDetectionActivity;
import org.pytorch.demo.GlassLocalActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;
import org.pytorch.demo.RemoteFaceDetectActivity;

public class VisionListActivity extends AbstractListActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //TODO: make a new activity for face detection
    findViewById(R.id.vision_card_face_recognition_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(VisionListActivity.this, FaceDetectionActivity.class);
      intent.putExtra(FaceDetectionActivity.INTENT_MODULE_ASSET_NAME, "mobile_model2.pt");
//      intent.putExtra(ImageClassificationActivity.INTENT_INFO_VIEW_TYPE,
//              InfoViewFactory.INFO_VIEW_TYPE_IMAGE_CLASSIFICATION_RESNET);
      startActivity(intent);
    });
    //TODO: make a new activity for face detection
    findViewById(R.id.vision_card_face_recognition_remote_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(VisionListActivity.this, GlassLocalActivity.class);
      startActivity(intent);
    });
  }

  @Override
  protected int getListContentLayoutRes() {
    return R.layout.vision_list_content;
  }
}
