package org.pytorch.demo.vision.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.json.*;

public class RectOverlay extends GraphicOverlay.Graphic {
    private int mRectColor = Color.RED;
    private float mStrokeWidth = 2.0f;
    private Paint mRectPaint;
    private GraphicOverlay graphicOverlay;
    private Rect rect;
    private String info;
    public RectOverlay(GraphicOverlay graphicOverlay, Rect rect, String info, int color) {
        super(graphicOverlay);
        mRectPaint = new Paint();
        mRectPaint.setColor(color);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);
        mRectPaint.setTextSize(30);

        this.graphicOverlay = graphicOverlay;
        this.rect = rect;
        this.info = info;

        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        RectF rectF = new RectF(rect);
        System.out.println("in draw of rect");
        //TODO parse json from info and draw it above box
        if (info != null)
        {
//            try{
//
//                JSONObject jsonObject = new JSONObject(info);
//                String name = jsonObject.getString("id");
//                System.out.println("in draw of rect name is "+name);
//                canvas.drawText(name,rectF.left,rectF.top, mRectPaint);
//            }catch (JSONException jsonException)
//            {
//                jsonException.printStackTrace();
//            }
            String name = info;
            System.out.println("in draw of rect name is "+name);
            canvas.drawText(name,rectF.left,rectF.top, mRectPaint);
        }


        canvas.drawRect(rectF,mRectPaint);

    }
}
