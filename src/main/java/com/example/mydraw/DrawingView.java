package com.example.mydraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class DrawingView extends View {
    private Paint paint;
    private Path path;
    private ArrayList<DrawPath> paths;
    private Bitmap backgroundBitmap;
    private int currentColor = Color.BLACK;
    private float currentStrokeWidth = 10f;

    public enum DrawMode {
        FREE_DRAW, LINE, CIRCLE, OVAL, RECTANGLE, TRIANGLE
    }

    private DrawMode currentMode = DrawMode.FREE_DRAW;
    private float startX, startY, endX, endY;
    private boolean isDrawing = false;

    private static class DrawPath {
        Path path;
        Paint paint;
        DrawMode mode;
        float startX, startY, endX, endY;

        DrawPath(Path path, Paint paint) {
            this.path = new Path(path);
            this.paint = new Paint(paint);
            this.mode = DrawMode.FREE_DRAW;
        }

        DrawPath(DrawMode mode, float startX, float startY, float endX, float endY, Paint paint) {
            this.mode = mode;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = new Paint(paint);
        }
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(currentStrokeWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        path = new Path();
        paths = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
        for (DrawPath dp : paths) {
            if (dp.mode == DrawMode.FREE_DRAW) {
                canvas.drawPath(dp.path, dp.paint);
            } else {
                drawShape(canvas, dp.mode, dp.startX, dp.startY, dp.endX, dp.endY, dp.paint);
            }
        }
        if (isDrawing) {
            if (currentMode == DrawMode.FREE_DRAW) {
                canvas.drawPath(path, paint);
            } else {
                drawShape(canvas, currentMode, startX, startY, endX, endY, paint);
            }
        }
    }

    private void drawShape(Canvas canvas, DrawMode mode, float x1, float y1, float x2, float y2, Paint p) {
        switch (mode) {
            case LINE:
                canvas.drawLine(x1, y1, x2, y2, p);
                break;
            case CIRCLE:
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                canvas.drawCircle(x1, y1, radius, p);
                break;
            case OVAL:
                RectF ovalRect = new RectF(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
                canvas.drawOval(ovalRect, p);
                break;
            case RECTANGLE:
                RectF rect = new RectF(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
                canvas.drawRect(rect, p);
                break;
            case TRIANGLE:
                Path trianglePath = new Path();
                trianglePath.moveTo(x1, y1);
                trianglePath.lineTo(x2, y2);
                trianglePath.lineTo(x1 - (x2 - x1), y2);
                trianglePath.close();
                canvas.drawPath(trianglePath, p);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawing = true;
                startX = x;
                startY = y;
                if (currentMode == DrawMode.FREE_DRAW) {
                    path.moveTo(x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                endX = x;
                endY = y;
                if (currentMode == DrawMode.FREE_DRAW) {
                    path.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                isDrawing = false;
                endX = x;
                endY = y;
                if (currentMode == DrawMode.FREE_DRAW) {
                    path.lineTo(x, y);
                    paths.add(new DrawPath(path, paint));
                    path.reset();
                } else {
                    paths.add(new DrawPath(currentMode, startX, startY, endX, endY, paint));
                }
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void setDrawMode(DrawMode mode) { this.currentMode = mode; }
    public DrawMode getDrawMode() { return currentMode; }
    public void setColor(int color) { currentColor = color; paint.setColor(color); }
    public void setStrokeWidth(float width) { currentStrokeWidth = width; paint.setStrokeWidth(width); }
    public void clear() { paths.clear(); path.reset(); backgroundBitmap = null; invalidate(); }
    public void setBackgroundImage(Bitmap bitmap) {
        if (getWidth() > 0 && getHeight() > 0) {
            backgroundBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
            invalidate();
        }
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
        for (DrawPath dp : paths) {
            if (dp.mode == DrawMode.FREE_DRAW) {
                canvas.drawPath(dp.path, dp.paint);
            } else {
                drawShape(canvas, dp.mode, dp.startX, dp.startY, dp.endX, dp.endY, dp.paint);
            }
        }
        return bitmap;
    }

    public void loadBitmap(Bitmap bitmap) {
        clear();
        if (bitmap != null) {
            setBackgroundImage(bitmap);
        }
    }
}