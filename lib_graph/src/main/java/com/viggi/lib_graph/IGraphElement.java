package com.viggi.lib_graph;

import android.graphics.Canvas;
import android.graphics.Point;

/**
 * Created by Dave on 2/22/2015.
 */
public interface IGraphElement
{
    GraphRectangle GetExtent(Point viewSize);

    void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds);

    int GetColor();

    void SetColor(int color);

    String GetName();
}
