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
	private Integer mPlayer1Color = null;
	private Integer mPlayer2Color = null;

	private int[][] mOriginalValues = null;
	private int[][] mValues;
	private int[][] mValues2;
	private Point mHighlightPoint = null;
	
	public SudokuView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public void InitializeBoard(int[][] values, Integer player1Color, Integer player2Color)
	{
		mOriginalValues = values;
		mPlayer1Color = player1Color;
		mPlayer2Color = player2Color;
		mTwoPlayer = player2Color != null;
		this.invalidate();
	}
	
	public void UpdateBoard(int[][] values)
	{
		mValues = values;
		this.invalidate();
	}
	
	public void UpdateBoard(int[][] values, int[][] values2, Point highlightPoint)
	{
		mValues = values;
		mValues2 = values2;
		mHighlightPoint = highlightPoint;
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
    	
    	int squareSize = (int)Math.sqrt(SudokuLogic.BoardSize);
    	
    	if(mTwoPlayer)
    	{
    		//Shade the squares for each player
    		paint.setStyle(Style.FILL);
    		
    		paint.setColor(Color.rgb(166, 166, 166));
    		
    		Point centerSquare = new Point(1, 1);
    		//(1,1)
    		int xStartOffset = (int)((float)centerSquare.x * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    		int xEndOffset = (int)((float)(centerSquare.x + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    		int yStartOffset = (int)((float)centerSquare.y * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    		int yEndOffset = (int)((float)(centerSquare.y + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);

    		Point[] player1Squares = SudokuLogic.GetPlayerSquares(0);
    		
    		paint.setColor(mPlayer1Color);
    		
    		for(int i=0; i<player1Squares.length; i++)
    		{
    			xStartOffset = (int)((float)player1Squares[i].x * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			xEndOffset = (int)((float)(player1Squares[i].x + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			yStartOffset = (int)((float)player1Squares[i].y * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			yEndOffset = (int)((float)(player1Squares[i].y + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		}
    		
    		Point[] player2Squares = SudokuLogic.GetPlayerSquares(1);
    		
    		paint.setColor(mPlayer2Color);
    		
    		for(int i=0; i<player2Squares.length; i++)
    		{
    			xStartOffset = (int)((float)player2Squares[i].x * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			xEndOffset = (int)((float)(player2Squares[i].x + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			yStartOffset = (int)((float)player2Squares[i].y * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			yEndOffset = (int)((float)(player2Squares[i].y + 1) * squareSize / SudokuLogic.BoardSize * usableWidth + mMargin);
    			canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
    		}
    	}
    	
    	paint.setColor(Color.WHITE);
    	paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(1);
    	
    	//Draw the minor vertical lines
    	for(int x =0; x<SudokuLogic.BoardSize + 1; x++)
    	{
    		int offset = (int)((float)x / SudokuLogic.BoardSize * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the minor horizontal lines
    	for(int y =0; y<SudokuLogic.BoardSize + 1; y++)
    	{
    		int offset = (int)((float)y / SudokuLogic.BoardSize * usableHeight + mMargin);
    		Point start = new Point (mMargin, offset);
    		Point end = new Point (mTotalWidth - mMargin, offset);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	
    	paint.setColor(Color.WHITE);
    	paint.setStrokeWidth(5);
    	
    	//Draw the major vertical lines
    	for(int x = 1; x < squareSize; x++)
    	{
    		int offset = (int)((float)x / squareSize * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the major horizontal lines
    	for(int y = 1; y < squareSize; y++)
    	{
    		int offset = (int)((float)y / squareSize * usableHeight + mMargin);
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
		
    	int boxSize = usableHeight / SudokuLogic.BoardSize;
    	
    	Paint paint = new Paint();
		paint.setTextSize(boxSize);
		paint.setTextAlign(Align.CENTER);
    	
		for(int x = 0; x < SudokuLogic.BoardSize; x++)
			for(int y=0; y < SudokuLogic.BoardSize; y++)
			{
				if(mHighlightPoint != null && mHighlightPoint.x == x && mHighlightPoint.y == y)
					paint.setColor(Color.GREEN);
				else
					paint.setColor(color);
				
				int xOffset = (int)((float)x / SudokuLogic.BoardSize * usableWidth + mMargin + boxSize / 2);
				int yOffset = (int)((float)y / SudokuLogic.BoardSize * usableHeight + boxSize);
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
