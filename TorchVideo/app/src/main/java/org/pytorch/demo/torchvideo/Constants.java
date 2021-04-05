package org.pytorch.demo.torchvideo;

public final class Constants {

    public final static float[] MEAN_RGB = new float[] {0.45f, 0.45f, 0.45f};
    public final static float[] STD_RGB = new float[] {0.225f, 0.225f, 0.225f};
    public final static int COUNT_OF_FRAMES_PER_INFERENCE = 4;
    public final static int TARGET_VIDEO_SIZE = 160;
    public final static int MODEL_INPUT_SIZE = COUNT_OF_FRAMES_PER_INFERENCE * 3 * TARGET_VIDEO_SIZE * TARGET_VIDEO_SIZE;
    public final static int TOP_COUNT = 5;
}
