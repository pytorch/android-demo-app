package org.pytorch.demo.nlp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.BaseModuleActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.vision.view.ResultRowView;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

public class TextClassificationActivity extends BaseModuleActivity {

  public static final String INTENT_MODULE_ASSET_NAME = "INTENT_MODULE_ASSET_NAME";

  private static final long EDIT_TEXT_STOP_DELAY = 600l;
  private static final int TOP_K = 3;
  private static final String SCORES_FORMAT = "%.2f";

  private EditText mEditText;
  private View mResultContent;
  private ResultRowView[] mResultRowViews = new ResultRowView[3];

  private Module mModule;
  private String mModuleAssetName;

  private String mLastBgHandledText;
  private String[] mModuleClasses;

  private static class AnalysisResult {
    private final String[] topKClassNames;
    private final float[] topKScores;

    public AnalysisResult(String[] topKClassNames, float[] topKScores) {
      this.topKClassNames = topKClassNames;
      this.topKScores = topKScores;
    }
  }

  private Runnable mOnEditTextStopRunnable = () -> {
    final String text = mEditText.getText().toString();
    mBackgroundHandler.post(() -> {
      if (TextUtils.equals(text, mLastBgHandledText)) {
        return;
      }

      if (TextUtils.isEmpty(text)) {
        runOnUiThread(() -> applyUIEmptyTextState());
        mLastBgHandledText = null;
        return;
      }

      final AnalysisResult result = analyzeText(text);
      if (result != null) {
        runOnUiThread(() -> applyUIAnalysisResult(result));
        mLastBgHandledText = text;
      }
    });
  };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_text_classification);
    mEditText = findViewById(R.id.text_classification_edit_text);
    findViewById(R.id.text_classification_clear_button).setOnClickListener(v -> mEditText.setText(""));

    final ResultRowView headerRow = findViewById(R.id.text_classification_result_header_row);
    headerRow.nameTextView.setText(R.string.text_classification_topic);
    headerRow.scoreTextView.setText(R.string.text_classification_score);
    headerRow.setVisibility(View.VISIBLE);

    mResultRowViews[0] = findViewById(R.id.text_classification_top1_result_row);
    mResultRowViews[1] = findViewById(R.id.text_classification_top2_result_row);
    mResultRowViews[2] = findViewById(R.id.text_classification_top3_result_row);
    mResultContent = findViewById(R.id.text_classification_result_content);

    mEditText.addTextChangedListener(new InternalTextWatcher());
  }

  protected String getModuleAssetName() {
    if (!TextUtils.isEmpty(mModuleAssetName)) {
      return mModuleAssetName;
    }
    final String moduleAssetNameFromIntent = getIntent().getStringExtra(INTENT_MODULE_ASSET_NAME);
    mModuleAssetName = !TextUtils.isEmpty(moduleAssetNameFromIntent)
        ? moduleAssetNameFromIntent
        : "model-reddit16-f140225004_2.pt1";

    return mModuleAssetName;
  }

  @WorkerThread
  @Nullable
  private AnalysisResult analyzeText(final String text) {
    if (mModule == null) {
      final String moduleFileAbsoluteFilePath = new File(
          Utils.assetFilePath(this, getModuleAssetName())).getAbsolutePath();
      mModule = Module.load(moduleFileAbsoluteFilePath);

      final IValue getClassesOutput = mModule.runMethod("get_classes");

      final IValue[] classesListIValue = getClassesOutput.toList();
      final String[] moduleClasses = new String[classesListIValue.length];

      int i = 0;
      for (IValue iv : classesListIValue) {
        moduleClasses[i++] = iv.toStr();
      }

      mModuleClasses = moduleClasses;
    }
    byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
    final long[] shape = new long[]{1, bytes.length};
    final Tensor inputTensor = Tensor.fromBlobUnsigned(bytes, shape);

    final Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
    final float[] scores = outputTensor.getDataAsFloatArray();
    final int[] ixs = Utils.topK(scores, TOP_K);

    final String[] topKClassNames = new String[TOP_K];
    final float[] topKScores = new float[TOP_K];
    for (int i = 0; i < TOP_K; i++) {
      final int ix = ixs[i];
      topKClassNames[i] = mModuleClasses[ix];
      topKScores[i] = scores[ix];
    }

    return new AnalysisResult(topKClassNames, topKScores);
  }

  private void applyUIAnalysisResult(AnalysisResult result) {
    for (int i = 0; i < TOP_K; i++) {
      setUiResultRowView(
          mResultRowViews[i],
          result.topKClassNames[i],
          String.format(Locale.US, SCORES_FORMAT, result.topKScores[i]));
    }

    mResultContent.setVisibility(View.VISIBLE);
  }

  private void applyUIEmptyTextState() {
    mResultContent.setVisibility(View.GONE);
  }

  private void setUiResultRowView(ResultRowView resultRowView, String name, String score) {
    resultRowView.nameTextView.setText(name);
    resultRowView.scoreTextView.setText(score);
    resultRowView.setProgressState(false);
  }

  @Override
  protected int getInfoViewCode() {
    return InfoViewFactory.INFO_VIEW_TYPE_TEXT_CLASSIFICATION;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mModule != null) {
      mModule.destroy();
    }
  }

  private class InternalTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
      mUIHandler.removeCallbacks(mOnEditTextStopRunnable);
      mUIHandler.postDelayed(mOnEditTextStopRunnable, EDIT_TEXT_STOP_DELAY);
    }
  }

}
