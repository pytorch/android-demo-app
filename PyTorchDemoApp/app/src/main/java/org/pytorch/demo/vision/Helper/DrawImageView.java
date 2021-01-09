package org.pytorch.demo.vision.Helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

public class DrawImageView extends androidx.appcompat.widget.AppCompatImageView{

    private RectF rectF;
    public DrawImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // TODO Auto-generated constructor stub
    }

    Paint paint = new Paint();
    {
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.5f);//设置线宽
        paint.setAlpha(100);
    };
    public void setRectF(Rect rect)
    {
        this.rectF = new RectF(rect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        System.out.println("in on draw of drawimageview");
        canvas.drawRect(new Rect(100, 200, 400, 500), paint);//绘制矩形
        if (rectF != null)
        {
            System.out.println("rect to draw");
            canvas.drawRect(rectF, paint);//绘制矩形

        }

    }






}