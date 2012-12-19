package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;

public class GraphLine implements IGraphElement
{
	public Point Start = null;
	public Point End = null;
	protected Paint mPaint = new Paint();
	
	public GraphLine()
	{
		this(null, null);
	}
	
	public GraphLine(Point start, Point end)
	{
		Start = start;
		End = end;
		if(Start == null)
			Start = new Point(0, 0);
		if(End == null)
			End = new Point(0, 0);
		SetColor(Color.WHITE);
	}
	
	@Override
	public String GetName()
	{
		return String.format("Line");
	}
	
	public GraphRectangle GetExtent(Point viewSize)
	{
		int left = Start.x;
		int right = End.x;
		int top = Start.y;
		int bottom = End.y;
		
		if(left > right)
		{
			int swapper = left;
			left = right;
			right = swapper;
		}
		
		if(top > bottom)
		{
			int swapper = top;
			top = bottom;
			bottom = swapper;
		}
		
		
		return new GraphRectangle(left, top, right, bottom);
	}
	
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		mPaint.setStyle(Style.STROKE);
		canvas.drawLine(Start.x, Start.y, End.x, End.y, mPaint);
	}
	
	public int GetColor()
	{
		return mPaint.getColor();
	}
	
	public void SetColor(int color)
	{
		mPaint.setColor(color);
	}
}