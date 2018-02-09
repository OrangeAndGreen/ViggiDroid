package com.viggi.lib_graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.viggi.lib_math.ArrayMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
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
        float yMin = ArrayMath.GetMin(Data);
        yMin = (float) Math.floor(yMin);
        float xMax = Math.round((Data.length - 1) * XInterval);
        float xMin = 0;

        if(XValues != null)
        {
            xMin = ArrayMath.GetMin(Data);
            xMax = ArrayMath.GetMax(XValues);
        }

        //Log.i("GraphPlot.GetDataRange", String.format("Raw X data range: %f, %f", xMin, xMax));
        //Log.i("GraphPlot.GetDataRange", String.format("Raw Y data range: %f, %f", yMin, yMax));

        //Log.i("GraphPlot.GetDataRange", String.format("size: (%.02f, %d)", yMax, (int)xMax));
        return new FloatRectangle(xMin, yMax, xMax, yMin);
    }

    public GraphRectangle GetExtent(Point viewSize)
    {
        return null;
    }

    public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
    {
        //Log.e("Graphing", "Drawing graph plot");
        //Log.e("Graphing", String.format("Drawing graph plot. Bound: {(%d,%d)(%d,%d)}  Data Bound: {(%.02f,%.02f)(%.02f,%.02f)}", bounds.Left, bounds.Right, bounds.Top, bounds.Bottom, dataBounds.Left, dataBounds.Right, dataBounds.Top, dataBounds.Bottom));
        int graphHeight = bounds.Bottom - bounds.Top;
        int graphWidth = bounds.Right - bounds.Left;
        if(dataBounds.Top % 1 != 0)
            dataBounds.Top = dataBounds.Top + 1 - (dataBounds.Top % 1);
        float dataYMultiplier = graphHeight / (dataBounds.Top - dataBounds.Bottom);
        //float dataXMultiplier = 0;
        float xMax = dataBounds.Right;

        //if(XValues != null)
        //	xMax = ArrayMath.GetMax(XValues);
        //Log.i("GraphPlot", String.format("Data max: %f, bounds.right: %f", xMax, dataBounds.Right));

        //if(Data != null && Data.length > 1)
        //	dataXMultiplier = (float)graphWidth / (xMax) * XInterval;

        DataPoints.clear();
        Point lastPoint = null;
        for(int i=0; i<Data.length; i++)
        {
            float xValue = i * XInterval;

            if(XValues != null)
                xValue = XValues[i];

            int xPixel = bounds.Left + Math.round(xValue / xMax * graphWidth);
            int yPixel = bounds.Bottom - (int)((Data[i] - dataBounds.Bottom) * dataYMultiplier);
            Point newPoint = new Point(xPixel, yPixel);
            if((DrawLine || DrawLinePairs) && lastPoint != null)
                canvas.drawLine(lastPoint.x, lastPoint.y, newPoint.x, newPoint.y, mPaint);
            if(DrawPoints)
            {
                GraphPoint point = new GraphPoint(newPoint, xValue, Data[i], 1, GraphPoint.PointType.CROSS);
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
