package com.example.mydraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class DrawingView extends View {
    private Paint brush;
    private ArrayList<ShapeData> shapeList;
    private int selectedColor = Color.BLACK;
    private DrawMode selectedMode = DrawMode.LINE;
    
    private float firstX, firstY, secondX, secondY;
    private boolean pointSelected = false; // 첫 점 클릭 여부

    public enum DrawMode { LINE, CIRCLE, OVAL }

    private static class ShapeData {
        DrawMode shapeType;
        float startX, startY, endX, endY;
        Paint drawPaint;

        ShapeData(DrawMode type, float x1, float y1, float x2, float y2, Paint p) {
            this.shapeType = type;
            this.startX = x1;
            this.startY = y1;
            this.endX = x2;
            this.endY = y2;
            this.drawPaint = new Paint(p);
        }
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        brush = new Paint();
        brush.setColor(selectedColor);
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeWidth(5f);
        brush.setAntiAlias(true);
        shapeList = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        
        // 저장된 도형 렌더링
        for (ShapeData shape : shapeList) {
            renderShape(canvas, shape.shapeType, shape.startX, shape.startY, 
                       shape.endX, shape.endY, shape.drawPaint);
        }
        
        // 첫 점 클릭 후 미리보기
        if (pointSelected) {
            renderShape(canvas, selectedMode, firstX, firstY, secondX, secondY, brush);
        }
    }

    private void renderShape(Canvas c, DrawMode type, float x1, float y1, float x2, float y2, Paint p) {
        switch (type) {
            case LINE:
                c.drawLine(x1, y1, x2, y2, p);
                break;
            case CIRCLE:
                float rad = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                c.drawCircle(x1, y1, rad, p);
                break;
            case OVAL:
                RectF bounds = new RectF(Math.min(x1, x2), Math.min(y1, y2),
                                        Math.max(x1, x2), Math.max(y1, y2));
                c.drawOval(bounds, p);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            
            if (!pointSelected) {
                // 시작점 설정
                firstX = touchX;
                firstY = touchY;
                secondX = touchX;
                secondY = touchY;
                pointSelected = true;
            } else {
                // 끝점 설정 및 완성
                secondX = touchX;
                secondY = touchY;
                shapeList.add(new ShapeData(selectedMode, firstX, firstY, secondX, secondY, brush));
                pointSelected = false;
            }
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && pointSelected) {
            // 드래그 시 미리보기 업데이트
            secondX = event.getX();
            secondY = event.getY();
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setDrawMode(DrawMode mode) {
        this.selectedMode = mode;
        pointSelected = false; // 모드 변경 시 초기화
    }

    public void setColor(int color) {
        selectedColor = color;
        brush.setColor(color);
    }

    public void clear() {
        shapeList.clear();
        pointSelected = false;
        invalidate();
    }

    public Bitmap getBitmap() {
        Bitmap result = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(result);
        tempCanvas.drawColor(Color.WHITE);
        for (ShapeData shape : shapeList) {
            renderShape(tempCanvas, shape.shapeType, shape.startX, shape.startY, 
                       shape.endX, shape.endY, shape.drawPaint);
        }
        return result;
    }

    public void loadBitmap(Bitmap bitmap) {
        clear();
    }
}
