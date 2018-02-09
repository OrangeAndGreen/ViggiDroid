package com.viggi.lib_graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * Created by Dave on 2/22/2015.
 */
public class GraphRectangle implements IGraphElement
{
    public int Left = 0;
    public int Right = 0;
    public int Top = 0;
    public int Bottom = 0;
    public boolean Fill = false;
    public boolean Border = true;
    public int FillColor = Color.DKGRAY;
    public int BorderColor = Color.WHITE;
    private Paint mPaint = new Paint();

    public GraphRectangle()
    {
        this(0, 0, 0, 0);
    }

    public GraphRectangle(int left, int top, int right, int bottom)
    {
        Left = left;
        Top = top;
        Right = right;
        Bottom = bottom;
        SetColor(Color.WHITE);
    }

    @Override
    public String GetName()
    {
        return String.format("Rectangle");
    }

    public GraphRectangle GetExtent(Point viewSize)
    {
        return this;
    }

    public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
    {
        if(Fill)
        {
            mPaint.setColor(FillColor);
            canvas.drawRect(Left, Top, Right, Bottom, mPaint);
        }
        if(Border)
        {
            mPaint.setColor(BorderColor);
            canvas.drawLine(Left, Top, Left, Bottom, mPaint);
            canvas.drawLine(Left, Top, Right, Top, mPaint);
            canvas.drawLine(Right, Bottom, Left, Bottom, mPaint);
            canvas.drawLine(Right, Bottom, Right, Top, mPaint);
        }

    }

    public int GetColor()
    {
        if(Fill)
            return FillColor;
        else
            return BorderColor;
    }

    public void SetColor(int color)
    {
        if(Fill)
            FillColor = color;
        else
            BorderColor = color;
    }

    public int GetWidth()
    {
        return Right - Left;
    }

    public int GetHeight()
    {
        return Bottom - Top;
    }

    public void ExpandToEnclose(GraphRectangle child)
    {
        if(child == null)
            return;
        if(Left > child.Left)
            Left = child.Left;
        if(Right < child.Right)
            Right = child.Right;
        if(Top > child.Top)
            Top = child.Top;
        if(Bottom < child.Bottom)
            Bottom = child.Bottom;
    }

    public void ShrinkToAvoid(GraphRectangle neighbor)
    {
        if(neighbor == null)
            return;
        if(Left < neighbor.Right)
            Left = neighbor.Right;
        if(Right > neighbor.Left)
            Right = neighbor.Left;
        if(Top < neighbor.Bottom)
            Top = neighbor.Bottom;
        if(Bottom > neighbor.Top)
            Bottom = neighbor.Top;
    }

    public void ShrinkToFit(GraphRectangle parent)
    {
        if(parent == null)
            return;
        if(Left < parent.Left)
            Left = parent.Left;
        if(Right > parent.Right)
            Right = parent.Right;
        if(Top < parent.Top)
            Top = parent.Top;
        if(Bottom > parent.Bottom)
            Bottom = parent.Bottom;
    }
}
