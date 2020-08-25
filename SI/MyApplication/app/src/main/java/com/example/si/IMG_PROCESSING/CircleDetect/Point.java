package com.example.si.IMG_PROCESSING.CircleDetect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

public class Point {
    int x;
    int y;
    int accu_value;
    float radius;
    public Point(){
        x = 0;
        y = 0;
        accu_value = 0;
        radius = 0.0f;
    }
    public static Bitmap DrawCircle(ArrayList<Point> circles,Bitmap ori_img){
        System.out.println("Starting drawing circles.....");
        Bitmap bitmap_new = ori_img.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(bitmap_new);
        Paint p = new Paint();
        p.setColor(Color.RED);//设置画笔颜色
        p.setAntiAlias(false);//设置画笔为无锯齿
        p.setStrokeWidth((float) 5.0);//线宽
        p.setStyle(Paint.Style.STROKE);//空心效果
        for(int i=0;i<circles.size();i++){
            Point po_temp = circles.get(i);
            canvas.drawCircle(po_temp.y,po_temp.x,po_temp.radius,p);//绘制圆
            canvas.drawPoint(po_temp.y,po_temp.x,p);//绘制圆心点
        }
        /*
        p.setColor(Color.BLUE);//设置画笔颜色
        for(int j=0;j<line.size();j++){
            Point po_temp = circles.get(j);
            canvas.drawPoint(po_temp.y,po_temp.x,p);//绘制圆心点
        }
         */
        return bitmap_new;
    }
}
