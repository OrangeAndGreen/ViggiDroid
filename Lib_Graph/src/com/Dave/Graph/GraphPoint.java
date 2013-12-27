package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class GraphPoint implements IGraphElement
{
	public Point Location = null;
	public float XValue = -1;
	public float YValue = -1;
	public int Radius = 1;
	public PointType Type = PointType.DOT;
	private Paint mPaint = new Paint();
	
	public GraphPoint()
	{
		this(null, -1, -1);
	}
	
	public GraphPoint(Point location, float xValue, float yValue)
	{
		Location = location;
		XValue = xValue;
		YValue = yValue;
		SetColor(Color.WHITE);
	}
	
	public GraphPoint(Point location, float xValue, float yValue, int radius, PointType type)
	{
		this(location, xValue, yValue);
		Radius = radius;
		Type = type;
	}
	
	@Override
	public String GetName()
	{
		return String.format("Point");
	}
	
	public GraphRectangle GetExtent(Point viewSize)
	{
		if(Location == null)
			return new GraphRectangle(0, 0, 0, 0);
		return new GraphRectangle(Location.x - (Radius/2), Location.y - (Radius/2),
								  Location.x + (Radius/2), Location.y + (Radius/2));
	}
	
	public FloatRectangle GetDataRange()
	{
		FloatRectangle ret = new FloatRectangle();
		
		ret.Left = XValue;
		ret.Right = XValue;
		ret.Top = YValue;
		ret.Bottom = YValue;
		
		return ret;
	}
	
	public void RecalculateGraphPosition(GraphRectangle bounds, FloatRectangle dataBounds)
	{
    	int x = bounds.Left + Math.round(XValue / (dataBounds.GetWidth()) * bounds.GetWidth());
    	int y = bounds.Bottom - Math.round(YValue / dataBounds.GetHeight() * bounds.GetHeight());
    	
    	//if(XValue > 23)
		//	Log.d("GraphPoint", String.format("Scaling %.02f between %.02f and %.02f, to %d", XValue, dataBounds.Left, dataBounds.Right, x));
    	
    	Location = new Point(x, y);
	}
	
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		//Calculate the pixel location from the data value if necessary (used for histogram points)
		if(Location == null)
			RecalculateGraphPosition(bounds, dataBounds);
		
		if(Type == PointType.CROSS)
			DrawCross(canvas);
		canvas.drawPoint(Location.x, Location.y, mPaint);
	}
	
	public void DrawCross(Canvas canvas)
	{
		canvas.drawLine(Location.x - Radius, Location.y, Location.x + Radius, Location.y, mPaint);
		canvas.drawLine(Location.x, Location.y - Radius, Location.x, Location.y + Radius, mPaint);
	}
	
	public int GetColor()
	{
		return mPaint.getColor();
	}
	
	public void SetColor(int color)
	{
		mPaint.setColor(color);
	}
	
	public enum PointType
	{
		DOT,
		CIRCLE,
		SQUARE,
		CROSS,
		X,
		DASH,
		SPLAT,
		TRIANGLE
	}
}
