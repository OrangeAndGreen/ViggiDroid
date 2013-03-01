package com.Dave.Sudoku;

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
	
	private boolean mShowSquareOptions = false;
	
	private boolean mTwoPlayer = false;
	private Integer mPlayer1Color = null;
	private Integer mPlayer2Color = null;

	//These are just for transferring the data to onDraw(), not for permanent storage
	private SudokuBoard mBoard = null;
	
	public SudokuView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public void InitializeBoard(SudokuBoard board, Integer player1Color, Integer player2Color)
	{
		mBoard = board;
		mPlayer1Color = player1Color;
		mPlayer2Color = player2Color;
		mTwoPlayer = player2Color != null;
		this.invalidate();
	}
	
	public void UpdateBoard()
	{
		mShowSquareOptions = false;
		this.invalidate();
	}

	public void ShowSquareOptions()
	{
		mShowSquareOptions = true;
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
		//Update View size
		mTotalHeight = getHeight();
		mTotalWidth = getWidth();
		if(mTotalHeight < mTotalWidth)
			mTotalWidth = mTotalHeight;
		else
			mTotalHeight = mTotalWidth;
			
		if(mShowSquareOptions)
			DrawSquareOptions(canvas);
		else
			DrawGame(canvas);
	}
	
	private void DrawSquareOptions(Canvas canvas)
	{
		int usableWidth = mTotalWidth - (2 * mMargin);
    	int usableHeight = mTotalHeight - (2 * mMargin);
    	
    	int textSize = usableHeight / SudokuBoard.BoardSize / 2;
    	
    	Paint numberPaint = new Paint();
    	numberPaint.setTextSize(textSize);
    	numberPaint.setTextAlign(Align.CENTER);
    	numberPaint.setColor(Color.BLACK);
    	
    	Paint oldExPaint = new Paint();
    	oldExPaint.setTextSize(textSize);
    	oldExPaint.setTextAlign(Align.CENTER);
    	oldExPaint.setColor(Color.RED);
    	
    	Paint newExPaint = new Paint();
    	newExPaint.setTextSize(textSize);
    	newExPaint.setTextAlign(Align.CENTER);
    	newExPaint.setColor(Color.GREEN);
    	
		int squareWidth = usableWidth / SudokuBoard.SquareSize;
    	
		DrawTwoPlayerBackground(canvas);
		DrawMajorGridLines(canvas);
		
		for(int x = 0; x < SudokuBoard.SquareSize; x++)
			for(int y=0; y<SudokuBoard.SquareSize; y++)
			{
				
				Point square = new Point(x, y);
				//Find the available options
				boolean[] oldOptions = mBoard.GetSquareOptions(square, false);
				boolean[] newOptions = mBoard.GetSquareOptions(square, true);
				
				/*
				String oldOptionString = "";
				for(int i=0; i < oldOptions.length; i++)
					oldOptionString += String.format("%s,", oldOptions[i]);
				Log.i("SudokuView", String.format("Square [%d,%d] old options: %s", x, y, oldOptionString));
				String newOptionString = "";
				for(int i=0; i < newOptions.length; i++)
					newOptionString += String.format("%s,", newOptions[i]);
				Log.i("SudokuView", String.format("Square [%d,%d] new options: %s", x, y, newOptionString));
				*/
				
				//Draw the options in a circle within the square
				float xCenter = ((float)square.x + 0.5f) * squareWidth + mMargin;
				float yCenter = ((float)square.y + 0.5f) * squareWidth + mMargin + textSize / 2;
				int radius = squareWidth / 3;
				
				for(int i=1; i<newOptions.length; i++)
				{
					float theta = (float)(i - 1) * 360 / SudokuBoard.BoardSize - 90;
					
					int xPixel = (int)(xCenter + radius * Math.cos(theta / 180 * Math.PI));
					int yPixel = (int)(yCenter + radius * Math.sin(theta / 180 * Math.PI));
					
					canvas.drawText(String.format("%d", i), xPixel, yPixel, numberPaint);
					if(!newOptions[i])
					{
						Paint curPaint = null;
						if(!oldOptions[i])
							curPaint = oldExPaint;
						else
							curPaint = newExPaint;
						canvas.drawText("X", xPixel, yPixel, curPaint);
					}
				}
			}
	}
	
	private void DrawGame(Canvas canvas)
	{
		DrawBoard(canvas);
		
		int initialColor = Color.WHITE;
		int player1Color = Color.GREEN;
		if(mTwoPlayer)
		{
			initialColor = Color.BLACK;
			player1Color = Color.WHITE;
		}
		DrawValues(canvas, mBoard.GetSubBoard(-1), initialColor);
		DrawValues(canvas, mBoard.GetSubBoard(0), player1Color);
		if(mTwoPlayer)
		{
			DrawValues(canvas, mBoard.GetSubBoard(1), Color.WHITE);
			DrawValues(canvas, mBoard.GetPendingMoves(), Color.GREEN);
		}
	}
	
	private void DrawBoard(Canvas canvas)
	{
    	if(mTwoPlayer)
    	{
    		DrawTwoPlayerBackground(canvas);
    	}
    	
    	DrawMinorGridLines(canvas);
    	
    	DrawMajorGridLines(canvas);
	}
	
	private void DrawTwoPlayerBackground(Canvas canvas)
	{
		Paint paint = new Paint();
    	
    	int usableWidth = mTotalWidth - (2 * mMargin);
    	int usableHeight = mTotalHeight - (2 * mMargin);
		
		//Shade the squares for each player
		paint.setStyle(Style.FILL);
		
		paint.setColor(Color.rgb(166, 166, 166));
		
		Point centerSquare = new Point(1, 1);
		//(1,1)
		int xStartOffset = (int)((float)centerSquare.x * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
		int xEndOffset = (int)((float)(centerSquare.x + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
		int yStartOffset = (int)((float)centerSquare.y * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
		int yEndOffset = (int)((float)(centerSquare.y + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
		canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);

		Point[] player1Squares = SudokuBoard.GetPlayerSquares(0);
		
		paint.setColor(mPlayer1Color);
		
		for(int i=0; i<player1Squares.length; i++)
		{
			xStartOffset = (int)((float)player1Squares[i].x * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
			xEndOffset = (int)((float)(player1Squares[i].x + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
			yStartOffset = (int)((float)player1Squares[i].y * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
			yEndOffset = (int)((float)(player1Squares[i].y + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
			canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
		}
		
		Point[] player2Squares = SudokuBoard.GetPlayerSquares(1);
		
		paint.setColor(mPlayer2Color);
		
		for(int i=0; i<player2Squares.length; i++)
		{
			xStartOffset = (int)((float)player2Squares[i].x * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
			xEndOffset = (int)((float)(player2Squares[i].x + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableWidth + mMargin);
			yStartOffset = (int)((float)player2Squares[i].y * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
			yEndOffset = (int)((float)(player2Squares[i].y + 1) * SudokuBoard.SquareSize / SudokuBoard.BoardSize * usableHeight + mMargin);
			canvas.drawRect(xStartOffset, yStartOffset, xEndOffset, yEndOffset, paint);
		}
	}
	
	private void DrawMajorGridLines(Canvas canvas)
	{
		Paint paint = new Paint();
    	
    	int usableHeight = mTotalHeight - (2 * mMargin);
    	int usableWidth = mTotalWidth - (2 * mMargin);
    	
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(5);
    	
    	//Draw the major vertical lines
    	for(int x = 1; x < SudokuBoard.SquareSize; x++)
    	{
    		int offset = (int)((float)x / SudokuBoard.SquareSize * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the major horizontal lines
    	for(int y = 1; y < SudokuBoard.SquareSize; y++)
    	{
    		int offset = (int)((float)y / SudokuBoard.SquareSize * usableHeight + mMargin);
    		Point start = new Point (mMargin, offset);
    		Point end = new Point (mTotalWidth - mMargin, offset);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
	}
	
	private void DrawMinorGridLines(Canvas canvas)
	{
		Paint paint = new Paint();
    	
    	int usableHeight = mTotalHeight - (2 * mMargin);
    	int usableWidth = mTotalWidth - (2 * mMargin);
    	
		paint.setColor(Color.WHITE);
    	paint.setStyle(Style.STROKE);
    	paint.setStrokeWidth(1);
    	
    	//Draw the minor vertical lines
    	for(int x =0; x<SudokuBoard.BoardSize + 1; x++)
    	{
    		int offset = (int)((float)x / SudokuBoard.BoardSize * usableWidth + mMargin);
    		Point start = new Point (offset, mMargin);
    		Point end = new Point (offset, mTotalHeight - mMargin);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
    	
    	//Draw the minor horizontal lines
    	for(int y =0; y<SudokuBoard.BoardSize + 1; y++)
    	{
    		int offset = (int)((float)y / SudokuBoard.BoardSize * usableHeight + mMargin);
    		Point start = new Point (mMargin, offset);
    		Point end = new Point (mTotalWidth - mMargin, offset);
    		canvas.drawLine(start.x, start.y, end.x, end.y, paint);
    	}
	}
	
	private void DrawValues(Canvas canvas, byte[][] values, int color)
	{
		if(values == null)
			return;
		
		int usableHeight = mTotalHeight - (2 * mMargin);
    	int usableWidth = mTotalWidth - (2 * mMargin);
		
    	int boxSize = usableHeight / SudokuBoard.BoardSize;
    	
    	Paint paint = new Paint();
		paint.setTextSize(boxSize);
		paint.setTextAlign(Align.CENTER);
    	
		for(int x = 0; x < SudokuBoard.BoardSize; x++)
			for(int y=0; y < SudokuBoard.BoardSize; y++)
			{
				paint.setColor(color);
				
				int xOffset = (int)((float)x / SudokuBoard.BoardSize * usableWidth + mMargin + boxSize / 2);
				int yOffset = (int)((float)y / SudokuBoard.BoardSize * usableHeight + boxSize);
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
		int width = measureWidth(widthMeasureSpec);
        setMeasuredDimension(width, measureHeight(heightMeasureSpec, width));
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
    private int measureHeight(int measureSpec, int width)
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
            result = width;
        }
        return result;
    }
}
