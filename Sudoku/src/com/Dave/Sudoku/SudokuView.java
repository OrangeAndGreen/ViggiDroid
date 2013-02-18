package com.Dave.Sudoku;

import com.Dave.Files.ErrorFile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class SudokuView extends View
{
	private static final int mMargin = 5;
	private int mTotalWidth = 0;
	private int mTotalHeight = 0;
	
	private boolean mTwoPlayer = false;

	private int[][] mOriginalValues = null;
	private int[][] mValues;
	private int[][] mValues2;
	
	public SudokuView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public void InitializeBoard(int[][] values, boolean twoPlayer)
	{
		mOriginalValues = values;
		mTwoPlayer = twoPlayer;
		this.invalidate();
	}
	
	public void UpdateBoard(int[][] values)
	{
		mValues = values;
		this.invalidate();
	}
	
	public void UpdateBoard(int[][] values, int[][] values2)
	{
		mValues = values;
		mValues2 = values2;
		this.invalidate();
	}

	public Point GetCell(float x, float y)
	{
		Point result = new Point();
		
		int usableSize = mTotalHeight - (2 * mMargin);
		
    	int boxSize = usableSize / 9;
		
		result.x = (int)((x - mMargin) / boxSize);
		result.y = (int)((y - mMargin) / boxSize);
		
		return result;
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		try
		{
			//Update View size
			mTotalHeight = getHeight();
			mTotalWidth = getWidth();
			if(mTotalHeight < mTotalWidth)
				mTotalWidth = mTotalHeight;
			else
				mTotalHeight = mTotalWidth;
			
			DrawBoard(canvas);
			int initialColor = Color.WHITE;
			int player1Color = Color.GREEN;
			if(mTwoPlayer)
			{
				initialColor = Color.BLACK;
				player1Color = Color.WHITE;
			}
			DrawValues(canvas, mOriginalValues, initialColor);
			DrawValues(canvas, mValues, player1Color);
			if(mTwoPlayer)
				DrawValues(canvas, mValues2, Color.WHITE);

		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	private void DrawBoard(Canvas canvas)
	{
		Paint paint = new Paint();
    	
    	int usableHeight = mTotalHeight - (2 * mMargin);
    	int usableWidth = mTotalWidth - (2 * mMargin);
    	
    	if(mTwoPlayer)
    	{
    		//Shade the squares for each player
    		paint.setStyle(Style.FILL);
    		
    		paint.setColor(Color.rgb(166, 166, 166));
    		
    		//(1,1)
    		int xStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		int xEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		int yStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		int yEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		paint.setColor(Color.rgb(79, 129, 189));
    		
    		//(0,0)
    		xStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(1,0)
    		xStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(2,1)
    		xStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(0,2)
    		xStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		paint.setColor(Color.rgb(149, 55, 53));
    		
    		//(2,0)
    		xStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(0,1)
    		xStartOffset = (int)((float)0 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(1,2)
    		xStartOffset = (int)((float)3 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		
    		//(2,2)
    		xStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		xEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		yStartOffset = (int)((float)6 / 9 * usableWidth + mMargin);
    		yEndOffset = (int)((float)9 / 9 * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    	}
    	
    	paint.setColor(Color.WHITE);
    	paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(1);
    	
    	//Draw the minor vertical lines
    	for(int x =0; x<10; x++)
    	{
    		int offset = (int)((float)x / 9 * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the minor horizontal lines
    	for(int y =0; y<10; y++)
    	{
    		int offset = (int)((float)y / 9 * usableHeight + mMargin);
    		Point start = new Point (mMargin, offset);
    		Point end = new Point (mTotalWidth - mMargin, offset);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	
    	paint.setColor(Color.WHITE);
    	paint.setStrokeWidth(5);
    	
    	//Draw the major vertical lines
    	for(int x = 1; x < 3; x++)
    	{
    		int offset = (int)((float)x / 3 * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the major horizontal lines
    	for(int y = 1; y < 3; y++)
    	{
    		int offset = (int)((float)y / 3 * usableHeight + mMargin);
    		Point start = new Point (mMargin, offset);
    		Point end = new Point (mTotalWidth - mMargin, offset);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
	}
	
	private void DrawValues(Canvas canvas, int[][] values, int color)
	{
		if(values == null)
			return;
		
		int usableHeight = mTotalHeight - (2 * mMargin);
    	int usableWidth = mTotalWidth - (2 * mMargin);
		
    	int boxSize = usableHeight / 9;
    	
    	Paint paint = new Paint();
		paint.setColor(color);
		paint.setTextSize(boxSize);
		paint.setTextAlign(Align.CENTER);
    	
		for(int x = 0; x < 9; x++)
			for(int y=0; y < 9; y++)
			{
				int xOffset = (int)((float)x / 9 * usableWidth + mMargin + boxSize / 2);
				int yOffset = (int)((float)y / 9 * usableHeight + boxSize);
				int value = values[x][y]; 
				if(value > 0)
				{
					canvas.drawText(String.format("%d", value), xOffset, yOffset, paint);
				}
				else if(value < 0)
				{
					canvas.drawText("X", xOffset, yOffset, paint);
				}
			}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}
	
	/**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
            // Set the size
            result = 200;
        }
        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
        	//Set size
            result = 200;
        }
        return result;
    }
}
