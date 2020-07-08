package org.pytorch.demo.vision;

import android.content.Intent;
import android.os.Bundle;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;

public class VisionListActivity extends AbstractListActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    findViewById(R.id.vision_card_qmobilenet_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(VisionListActivity.this, ImageClassificationActivity.class);
      intent.putExtra(ImageClassificationActivity.INTENT_MODULE_ASSET_NAME,
          "mobilenet_v2.pt");
      intent.putExtra(ImageClassificationActivity.INTENT_INFO_VIEW_TYPE,
          InfoViewFactory.INFO_VIEW_TYPE_IMAGE_CLASSIFICATION_QMOBILENET);
      startActivity(intent);
    });
    findViewById(R.id.vision_card_resnet_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(VisionListActivity.this, ImageClassificationActivity.class);
      intent.putExtra(ImageClassificationActivity.INTENT_MODULE_ASSET_NAME, "resnet18.pt");
      intent.putExtra(ImageClassificationActivity.INTENT_INFO_VIEW_TYPE,
          InfoViewFactory.INFO_VIEW_TYPE_IMAGE_CLASSIFICATION_RESNET);
      startActivity(intent);
    });
  }

  @Override
  protected int getListContentLayoutRes() {
    return R.layout.vision_list_content;
  }
}
