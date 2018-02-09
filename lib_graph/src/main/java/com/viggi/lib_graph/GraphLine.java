package com.viggi.lib_graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * Created by Dave on 2/22/2015.
 */
public class GraphLine implements IGraphElement
{
    public Point Start = null;
    public Point End = null;
    protected Paint mPaint = new Paint();

    public GraphLine()
    {
        this(null, null, 1);
    }

    public GraphLine(Point start, Point end, int width)
    {
        Start = start;
        End = end;
        //if(Start == null)
        //	Start = new Point(0, 0);
        //if(End == null)
        //	End = new Point(0, 0);

        mPaint.setStrokeWidth(width);
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
        mPaint.setStyle(Paint.Style.STROKE);
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
