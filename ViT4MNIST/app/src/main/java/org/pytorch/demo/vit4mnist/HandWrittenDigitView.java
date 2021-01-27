// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.


package org.pytorch.demo.vit4mnist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class HandWrittenDigitView extends View {
    private Path mPath;
    private Paint mPaint, mCanvasPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private MainActivity mActivity;
    private List<List<Pair<Float, Float>>> mAllPoints = new ArrayList<>();
    private List<Pair<Float, Float>> mConsecutivePoints = new ArrayList<>();

    public HandWrittenDigitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (MainActivity) context;

        setPathPaint();
    }

    private void setPathPaint() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(0xFF000000);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(18);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mCanvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mCanvasPaint);
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mConsecutivePoints.clear();
                mConsecutivePoints.add(new Pair(x, y));
                mPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mConsecutivePoints.add(new Pair(x, y));
                mPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                mConsecutivePoints.add(new Pair(x, y));
                mAllPoints.add(new ArrayList<>(mConsecutivePoints));
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public List<List<Pair<Float, Float>>> getAllPoints() {
        return mAllPoints;
    }

    public void clearAllPointsAndRedraw() {
        mBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvasPaint = new Paint(Paint.DITHER_FLAG);
        mCanvas.drawBitmap(mBitmap, 0, 0, mCanvasPaint);

        setPathPaint();
        invalidate();
        mAllPoints.clear();
    }

    public void clearAllPoints() {
        mAllPoints.clear();
    }
}