package org.pytorch.demo.vision.Helper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import org.json.*;

public class RectOverlay extends GraphicOverlay.Graphic {
    private int mRectColor = Color.RED;
    private float mStrokeWidth = 4.0f;
    private Paint mRectPaint;
    private Paint mBackPaint;
    private Paint mTextPaint;
    private GraphicOverlay graphicOverlay;
    private final int text_size = 50;
    private Rect rect;
    private String info;
    public RectOverlay(GraphicOverlay graphicOverlay, Rect rect, String info, int color) {
        super(graphicOverlay);
        mRectPaint = new Paint();
        mRectPaint.setColor(color);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(mStrokeWidth);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(text_size);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTypeface(Typeface.MONOSPACE);

        mBackPaint = new Paint();
        mBackPaint.setColor(Color.WHITE);
        mBackPaint.setStyle(Paint.Style.FILL);

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
            int height = text_size;
            int width = name.length() * text_size;
            canvas.drawRect(rectF.left, rectF.top - height, rectF.left + width, rectF.top + 0.2f * height, mBackPaint);
            canvas.drawText(name,rectF.left,rectF.top, mTextPaint);
        }


        canvas.drawRect(rectF,mRectPaint);

    }
}
