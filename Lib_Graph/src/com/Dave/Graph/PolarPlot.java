package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.Log;

import com.Dave.Graph.GraphPoint.PointType;
import com.Dave.Math.ArrayMath;

public class PolarPlot extends GraphPlot
{
	public PolarPlot()
	{
		this(null, 1);
	}
	
	public PolarPlot(float[] data, float xInterval)
	{
		Data = data;
		XInterval = xInterval;
		if(Data == null)
			Data = new float[0];
		SetColor(Color.WHITE);
	}
	
	@Override
	public FloatRectangle GetDataRange()
	{
		//Log.e("GetRectangleRange", String.format("size: (%.02f, %d)", ArrayMath.GetMax(Data), Math.round(Data.length * XInterval)));
		return new FloatRectangle(0, ArrayMath.GetMax(Data), Math.round(Data.length * XInterval), 0);
	}
	
	@Override
	public GraphRectangle GetExtent(Point viewSize)
	{
		return null;
	}
	
	@Override
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		Log.e("Graphing", "Drawing polar plot");
		mPaint.setStyle(Style.STROKE);
		int plotWidth = bounds.Right - bounds.Left;
		int plotHeight = bounds.Bottom - bounds.Top;
		int plotRadius = Math.min(plotWidth, plotHeight) / 2;
		
		float xCenter = ((float)bounds.Left + bounds.Right) / 2;
		float yCenter = ((float)bounds.Top + bounds.Bottom) / 2;
		
		//Log.e("Graphing", String.format("Polar graph center pixel: %.02f, %.02f", xCenter, yCenter));
		
		//if(dataBounds.Top % 1 != 0)
		//	dataBounds.Top = dataBounds.Top + 1 - (dataBounds.Top % 1);
		float dataXMultiplier = 1;
		float xMax = (Data.length - 1) * XInterval;
		if(Data != null && Data.length > 1)
			dataXMultiplier = (float)plotRadius / (xMax) * XInterval;
		
		DataPoints.clear();
		Point lastPoint = null;
		float prevTheta = 0;
		for(int i=0; i<Data.length; i++)
		{
			float radius = i * dataXMultiplier;
			float theta = Data[i];
			
			if(theta == 0)
				theta = prevTheta;
			else
				prevTheta = theta;
			
			int xPixel = (int)(xCenter + radius * Math.cos((theta - 90) / 180 * Math.PI));
			int yPixel = (int)(yCenter + radius * Math.sin((theta - 90) / 180 * Math.PI));
	
			Point newPoint = new Point(xPixel, yPixel);
			if(lastPoint != null && Data[i - 1] != 0)
			{
				canvas.drawPoint(xPixel, yPixel, mPaint);
				
				//canvas.drawLine(lastPoint.x, lastPoint.y, newPoint.x, newPoint.y, mPaint);
				
				RectF rect = new RectF();
				rect.top = yCenter - radius;
				rect.bottom = yCenter + radius;
				rect.left = xCenter - radius;
				rect.right = xCenter + radius;
					
				float sweepAngle = theta - Data[i - 1];
				float startAngle = Data[i - 1] - 90;
				if(sweepAngle > 180)
					sweepAngle -= 360;
				if(sweepAngle < -180)
					sweepAngle += 360;
				canvas.drawArc(rect, startAngle, sweepAngle, false, mPaint);
			}
				
			if(DrawPoints)
			{
				GraphPoint point = new GraphPoint(newPoint, i * dataXMultiplier, Data[i], 1, PointType.CROSS);
				point.SetColor(PointColor);
				DataPoints.add(point);
			}
			lastPoint = newPoint;
		}

		/*
		float theta = 355;
		int xGuide = (int)(xCenter + plotRadius * (float)Math.cos((theta - 90) / 180 * Math.PI));
		int yGuide = (int)(yCenter + plotRadius * (float)Math.sin((theta - 90) / 180 * Math.PI));
		canvas.drawLine(xCenter, yCenter, xGuide, yGuide, mPaint);
		Log.e("Graphing", String.format("(%d, %d)", xGuide, yGuide));
		theta = 0;
		xGuide = (int)(xCenter + Data.length * dataXMultiplier * (float)Math.cos((theta - 90) / 180 * Math.PI));
		yGuide = (int)(yCenter + Data.length * dataXMultiplier * (float)Math.sin((theta - 90) / 180 * Math.PI));
		canvas.drawLine(xCenter, yCenter, xGuide, yGuide, mPaint);
		*/
		for(int i=0; i<DataPoints.size(); i++)
			DataPoints.get(i).Draw(canvas, bounds, dataBounds);
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
