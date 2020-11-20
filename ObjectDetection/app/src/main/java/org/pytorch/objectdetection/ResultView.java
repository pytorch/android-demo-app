// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.objectdetection;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import org.w3c.dom.Text;


public class ResultView extends View {

    private Paint mPaintRectangle;
    private Paint mPaintText;
    private int m_y = 10;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.YELLOW);
        mPaintText = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);
        canvas.drawRect(10, m_y, 500, 300, mPaintRectangle);

        Path mPath = new Path();
        RectF mRectF = new RectF(10, m_y, 260, m_y +50);
        mPath.addRect(mRectF, Path.Direction.CW);
        mPaintText.setColor(Color.MAGENTA);
        canvas.drawPath(mPath, mPaintText);

        mPaintText.setColor(Color.WHITE);
        mPaintText.setStrokeWidth(0);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setTextSize(32);
        canvas.drawText("person 0.84", 40, 35+m_y, mPaintText);
    }

    public void setResults(int y) {
        m_y = y;
    }
}
