package com.Dave.Graph;

import java.util.ArrayList;
import java.util.List;

import com.Dave.Graph.GraphPoint.PointType;
import com.Dave.Math.ArrayMath;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

/*
 * Contains all information for a data plot
 */

public class GraphPlot implements IGraphElement
{
	public float[] Data = null;
	public float[] XValues = null;
	public List<GraphPoint> DataPoints = new ArrayList<GraphPoint>();
	public float XInterval = 1;
	public boolean DrawLine = true;
	public boolean DrawLinePairs = false;
	public boolean DrawPoints = true;
	public int PointColor = Color.GREEN;
	protected Paint mPaint = new Paint();
	
	public GraphPlot()
	{
		this(null, 1);
	}
	
	public GraphPlot(float[] data, float xInterval)
	{
		Data = data;
		XInterval = xInterval;
		if(Data == null)
			Data = new float[0];
		SetColor(Color.WHITE);
	}
	
	public GraphPlot(float[] xValues, float[] yValues)
	{
		XValues = xValues;
		Data = yValues;
		DrawLine = true;
		if(Data == null)
			Data = new float[0];
		SetColor(Color.WHITE);
	}
	
	public GraphPlot(float[] xValues, float[] yValues, boolean drawPairs)
	{
		XValues = xValues;
		Data = yValues;
		DrawLinePairs = drawPairs;
		if(Data == null)
			Data = new float[0];
		SetColor(Color.WHITE);
	}
	
	@Override
	public String GetName()
	{
		return String.format("Plot");
	}
	
	public FloatRectangle GetDataRange()
	{
		float yMax = ArrayMath.GetCeiling(Data);
		float xMax = Math.round(Data.length * XInterval);
		
		if(XValues != null)
			xMax = ArrayMath.GetMax(XValues);
		
		//Log.i("GraphPlot.GetDataRange", String.format("size: (%.02f, %d)", yMax, (int)xMax));
		return new FloatRectangle(0, yMax, xMax, 0);
	}
	
	public GraphRectangle GetExtent(Point viewSize)
	{
		return null;
	}
	
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		//Log.e("Graphing", "Drawing graph plot");
		int graphHeight = bounds.Bottom - bounds.Top;
		int graphWidth = bounds.Right - bounds.Left;
		if(dataBounds.Top % 1 != 0)
			dataBounds.Top = dataBounds.Top + 1 - (dataBounds.Top % 1);
		float dataYMultiplier = graphHeight / dataBounds.Top;
		float dataXMultiplier = 0;
		float xMax = (Data.length - 1) * XInterval;
		
		if(XValues != null)
			xMax = ArrayMath.GetMax(XValues);
		
		if(Data != null && Data.length > 1)
			dataXMultiplier = (float)graphWidth / (xMax) * XInterval;
		
		DataPoints.clear();
		Point lastPoint = null;
		for(int i=0; i<Data.length; i++)
		{
			float xValue = i * XInterval;
			
			if(XValues != null)
				xValue = XValues[i];
			
			int xPixel = bounds.Left + Math.round(xValue / xMax * graphWidth);
			int yPixel = bounds.Bottom - (int)(Data[i]*dataYMultiplier);
			Point newPoint = new Point(xPixel, yPixel);
			if((DrawLine || DrawLinePairs) && lastPoint != null)
				canvas.drawLine(lastPoint.x, lastPoint.y, newPoint.x, newPoint.y, mPaint);
			if(DrawPoints)
			{
				GraphPoint point = new GraphPoint(newPoint, xValue, Data[i], 1, PointType.CROSS);
				point.SetColor(PointColor);
				DataPoints.add(point);
			}
			if(lastPoint == null || !DrawLinePairs)
				lastPoint = newPoint;
			else
				lastPoint = null;
		}
		for(int i=0; i<DataPoints.size(); i++)
			DataPoints.get(i).Draw(canvas, bounds, dataBounds);
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
