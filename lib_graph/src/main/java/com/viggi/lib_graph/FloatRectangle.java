package com.viggi.lib_graph;

/**
 * Created by Dave on 2/22/2015.
 */
public class FloatRectangle
{
    public float Left = 0;
    public float Right = 0;
    public float Top = 0;
    public float Bottom = 0;

    public FloatRectangle() {}

    public FloatRectangle(float left, float top, float right, float bottom)
    {
        Left = left;
        Top = top;
        Right = right;
        Bottom = bottom;
    }

    public float GetWidth()
    {
        return Right - Left;
    }

    public float GetHeight()
    {
        return Math.abs(Bottom - Top);
    }

    public void ExpandToEnclose(FloatRectangle child)
    {
        if(child == null)
            return;
        if(Left > child.Left)
            Left = child.Left;
        if(Right < child.Right)
            Right = child.Right;
        if(Top < child.Top)
            Top = child.Top;
        if(Bottom > child.Bottom)
            Bottom = child.Bottom;
    }

    public void ShrinkToAvoid(FloatRectangle neighbor)
    {
        if(neighbor == null)
            return;
        if(Left < neighbor.Right)
            Left = neighbor.Right;
        if(Right > neighbor.Left)
            Right = neighbor.Left;
        if(Top > neighbor.Bottom)
            Top = neighbor.Bottom;
        if(Bottom < neighbor.Top)
            Bottom = neighbor.Top;
    }

    public void ShrinkToFit(FloatRectangle parent)
    {
        if(parent == null)
            return;
        if(Left < parent.Left)
            Left = parent.Left;
        if(Right > parent.Right)
            Right = parent.Right;
        if(Top > parent.Top)
            Top = parent.Top;
        if(Bottom < parent.Bottom)
            Bottom = parent.Bottom;
    }
}
