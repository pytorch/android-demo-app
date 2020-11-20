package org.pytorch.objectdetection;

import android.graphics.Rect;

import java.util.ArrayList;

class Result {
    int classIndex;
    Float score;
    Rect rect;

    public Result(int cls, Float output, Rect rect) {
        this.classIndex = cls;
        this.score = output;
        this.rect = rect;
    }
};

public class PrePostProcessor {
    private static int inputWidth = 640;
    private static int inputHeight = 640;

    // model output is of size 25200*85
    private static int outputRow = 25200; // as decided by the YOLOv5 model for input image of size 640*640
    private static int outputColumn = 85; // left, top, right, bottom, score and 80 class probability
    private static float threshold = 0.35f; // score above which a detection is generated
    private static int nmsLimit = 15;


    static ArrayList<Result> outputsToNMSPredictions(Float[] outputs, float imgScaleX, float imgScaleY, float ivScaleX, float ivScaleY, float startX, float startY) {
        ArrayList<Result> results = new ArrayList<>();
        for (int i=0; i<outputRow; i++) {
            if (outputs[i*outputColumn+4] > threshold) {
                float x = outputs[i*outputColumn];
                float y = outputs[i*outputColumn+1];
                float w = outputs[i*outputColumn+2];
                float h = outputs[i*outputColumn+3];

                float left = imgScaleX * (x - w/2);
                float top = imgScaleY * (y - h/2);
                float right = imgScaleX * (x + w/2);
                float bottom = imgScaleY * (y + h/2);

                float max = outputs[i*outputColumn+5];
                int cls = 0;
                for (int j=0; j < outputColumn-5; j++) {
                    if (outputs[i*outputColumn+5+j] > max) {
                        max = outputs[i*outputColumn+5+j];
                        cls = j;
                    }
                }

                Rect rect = new Rect((int)(startX+ivScaleX*left), (int)(startY+top*ivScaleY), (int)(ivScaleX*right), (int)(ivScaleY*bottom));

                Result result = new Result(cls, outputs[i*85+4], rect);
                results.add(result);
            }
        }
        return results;
        //return nonMaxSuppression(boxes: predictions, limit: nmsLimit, threshold: threshold)
    }


}
