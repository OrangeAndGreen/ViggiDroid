package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;

public class GraphCircle extends GraphLine
{
	public Point Center = null;
	public int Radius = 5;
	
	public GraphCircle()
	{
		this(null, 5);
	}
	
	public GraphCircle(Point center, int radius)
	{
		Center = center;
		Radius = Math.abs(radius);
		if(Center == null)
			Center = new Point(0, 0);
		SetColor(Color.WHITE);
	}
	
	@Override
	public GraphRectangle GetExtent(Point viewSize)
	{
		int left = Center.x - Radius;
		int right = Center.x + Radius;
		int top = Center.y - Radius;
		int bottom = Center.y + Radius;
		
		
		return new GraphRectangle(left, top, right, bottom);
	}
	
	@Override
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		mPaint.setStyle(Style.STROKE);
		canvas.drawCircle(Center.x, Center.y, Radius, mPaint);
	}
	
	@Override
	public int GetColor()
	{
		return mPaint.getColor();
	}
	
	@Override
	public void SetColor(int color)
	{
		mPaint.setColor(color);
	}
}
