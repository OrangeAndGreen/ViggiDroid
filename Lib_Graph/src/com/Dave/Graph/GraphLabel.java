package com.Dave.Graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Align;

public class GraphLabel implements IGraphElement
{
	public Point Location = null;
	public String Text = null;
	public Align HAlign = Align.LEFT;
	public VerticalAlign VAlign = VerticalAlign.BOTTOM;
	private Paint mPaint = new Paint();
	private int mHeight = 0;
	private boolean mTitle = false;
	
	public GraphLabel()
	{
		this(null);
	}
	
	public GraphLabel(boolean title)
	{
		this(null);
		mTitle = title;
	}
	
	public GraphLabel(Point location)
	{
		Location = location;
		//if(Location == null)
		//	Location = new Point(0, 0);
		SetColor(Color.WHITE);
		Text = "";
	}
	
	public GraphLabel(Point location, String text)
	{
		this(location);
		Text = text;
	}
	
	public GraphLabel(Point location, String text, Align hAlign, VerticalAlign vAlign)
	{
		this(location, text);
		HAlign = hAlign;
		VAlign = vAlign;
	}
	
	@Override
	public String GetName()
	{
		return String.format("Label-%s", Text);
	}
	
	public GraphRectangle GetExtent(Point viewSize)
	{
		if(mTitle && viewSize != null)
		{
			HAlign = Align.CENTER;
			VAlign = VerticalAlign.TOP;
			
			if(Location == null)
				Location = new Point(0,0);
			
			Location.x = viewSize.x / 2;
			Location.y = 0;
		}
		
		String[] lines = { "" };
		if(Text != null)
			lines = Text.split("\n");
		
		Rect bounds = new Rect();
		mHeight = 0;
		int width = 0;
		for(int i=0; i<lines.length; i++)
		{
			mPaint.getTextBounds(lines[i], 0, lines[i].length(), bounds);
			if(bounds.right > width)
				width = bounds.right;
			mHeight += bounds.bottom - bounds.top + 1;
		}
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		switch(HAlign)
		{
		case LEFT:
			left = Location.x;
			break;
		case CENTER:
			left = Location.x - Math.round((float)width/2);
			break;
		case RIGHT:
			left = Location.x - width;
			break;
		default:
			break;
		}
		right = left + width;

		switch(VAlign)
		{
		case TOP:
			top = Location.y;
			break;
		case MIDDLE:
			top = Location.y - Math.round((float)mHeight/2);
			break;
		case BOTTOM:
			top = Location.y - mHeight;
			break;
		default:
			break;
		}
		bottom = top + mHeight;
		
		return new GraphRectangle(left, top, right, bottom);
	}
	
	public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
	{
		//Log.i("GraphLabel", String.format("Drawing label %s at (%d, %d)", Text, Location.x, Location.y));
		GetExtent(null);
		switch(HAlign)
		{
		case LEFT:
			mPaint.setTextAlign(Align.LEFT);
			break;
		case CENTER:
			mPaint.setTextAlign(Align.CENTER);
			break;
		case RIGHT:
			mPaint.setTextAlign(Align.RIGHT);
			break;
		default:
			break;
		}

		int heightOffset = 0;
		switch(VAlign)
		{
		case TOP:
			heightOffset = mHeight;
			break;
		case MIDDLE:
			heightOffset = Math.round((float)mHeight/2);
			break;
		default:
			break;
		}
		
		String[] lines = { "" };
		if(Text != null)
			lines = Text.split("\n");
		int lineHeight = mHeight / lines.length;
		for(int i=0; i<lines.length; i++)
			canvas.drawText(lines[i], Location.x, Location.y + heightOffset - ((lines.length - 1 - i) * lineHeight), mPaint);
	}
	
	public int GetColor()
	{
		return mPaint.getColor();
	}
	
	public void SetColor(int color)
	{
		mPaint.setColor(color);
	}
	
	public enum VerticalAlign
	{
		TOP,
		MIDDLE,
		BOTTOM
	}
	
	
}
