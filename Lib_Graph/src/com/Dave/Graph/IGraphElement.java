package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Point;

/*
 * Common functionality for all graph display elements
 * 
 * Includes:
 * 		-Drawing
 * 		-Getting size
 * 		-Getting/setting common properties like color
 * 
 */

public interface IGraphElement
{
	
	GraphRectangle GetExtent(Point viewSize);
	
	void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds);
	
	int GetColor();
	
	void SetColor(int color);
	
	String GetName();

}
