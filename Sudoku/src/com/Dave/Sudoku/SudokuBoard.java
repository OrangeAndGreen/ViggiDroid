package com.Dave.Sudoku;

import android.graphics.Point;
import android.util.Log;

public class SudokuBoard
{
	public static final int BoardSize = 9;
	public static final int SquareSize = (int)Math.sqrt(BoardSize);
	
	private byte[][] mInitialBoard = null;
	private byte[][] mPlayer1Board = null;
	private byte[][] mPlayer2Board = null;
	
	private byte[][] mPendingMove = null;
	private int mPendingPlayerTurn = 0;
	
	/*
	public static final byte[][] DefaultBoard = {{5, 3, 0, 0, 7, 0, 0, 0, 0},
											    {6, 0, 0, 1, 9, 5, 0, 0, 0},
											    {0, 9, 8, 0, 0, 0, 0, 6, 0},
											    {8, 0, 0, 0, 6, 0, 0, 0, 3},
											    {4, 0, 0, 8, 0, 3, 0, 0, 1},
											    {7, 0, 0, 0, 2, 0, 0, 0, 6},
											    {0, 6, 0, 0, 0, 0, 2, 8, 0},
											    {0, 0, 0, 4, 1, 9, 0, 0, 5},
											    {0, 0, 0, 0, 8, 0, 0, 7, 9}
											   };

	public static final byte[][] NearlyCompleteBoard =  {{5, 3, 4, 6, 7, 8, 9, 1, 2},
													    {6, 7, 2, 1, 9, 5, 3, 4, 8},
													    {0, 9, 8, 3, 4, 2, 5, 6, 7},
													    {8, 5, 9, 7, 6, 1, 4, 2, 3},
													    {4, 2, 6, 8, 5, 3, 7, 9, 1},
													    {7, 1, 3, 9, 2, 4, 8, 5, 6},
													    {9, 6, 1, 5, 3, 7, 2, 8, 4},
													    {2, 8, 7, 4, 1, 9, 6, 3, 5},
													    {3, 4, 5, 2, 8, 6, 1, 7, 9}
													   };
	 */
	
	public SudokuBoard()
	{
		mInitialBoard = CreateBoard();
		mPlayer1Board = CreateBoard();
		mPlayer2Board = CreateBoard();
	}
	
	private static byte[][] CreateBoard()
	{
		byte[][] output = new byte[BoardSize][];
        for(int i=0; i<BoardSize; i++)
        	output[i] = new byte[BoardSize];
        
        return output;
	}
	
	public static SudokuBoard Create(int numberToFill, boolean makeFairForTwoPlayer)
	{
		SudokuBoard ret = new SudokuBoard();
        
        int numPlayers = 1;
        byte[] player1Choices = null;
        if(makeFairForTwoPlayer)
        {
        	numPlayers = 2;
        	player1Choices = new byte[numberToFill];
        	ret.mInitialBoard[4][4] = (byte)(Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0) + 1);
        }
        for(int player=0; player < numPlayers; player++)
	        for(int fillIndex=0; fillIndex<numberToFill; fillIndex++)
	        {
	        	//Randomly set some cells without breaking the rules
	        	//First: Choose the value to fill in
	        	
	        	//Generate a value randomly
	        	byte value = (byte)(Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0) + 1);
	        	if(makeFairForTwoPlayer)
	        	{
	        		if(player == 0) //save the choice
	        			player1Choices[fillIndex] = value;
	        		else	//do what player1 did
	        			value = player1Choices[fillIndex];
	        	}
	        	
	        	for(int attempt = 0; attempt < 1000; attempt++)
	        	{
	        		//Randomly pick a cell
	        		int x = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0);
	        		int y = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0);
	        		Point cell = new Point(x, y);
	        		
	        		//Log.i("SudokuBoard", String.format("Trying to set cell (%d, %d) to %d", x, y, value));
	        		
	        		//Make sure the cell is empty
	        		//For two-player, make sure we are in the right territory
	        		//Make sure the cell is legal based on other already filled values
	        		if(ret.mInitialBoard[x][y] > 0
	        				|| (makeFairForTwoPlayer && GetPlayerTerritory(cell) != player)
	        				|| !ret.GetCellOptions(cell, true)[value])
	        		{
	        			continue;
	        		}
	        		
	        		//Set the cell in the map
	        		ret.mInitialBoard[x][y] = value;
	        		
	        		//Make sure all remaining blank cells are still valid
	        		boolean keepLooking = false;
	        		for(int xCheck=0; xCheck<BoardSize; xCheck++)
	        			for(int yCheck=0; yCheck<BoardSize; yCheck++)
	        			{
	        				Point checkCell = new Point(xCheck, yCheck);
	        				if(ret.GetCell(checkCell, true) == 0 && !ret.IsCellValid(checkCell))
	        				{
	        					ret.mInitialBoard[x][y] = 0;
	        					keepLooking = true;
	        				}
	        			}
	        		if(keepLooking)
	        			continue;
	        		
	        		//Make sure every square can still be completed
	        		
	        		
	        		break;
	        	}
	        }
        
        return ret;
	}
	
	public static SudokuBoard Clone(SudokuBoard boardToClone)
	{
		SudokuBoard output = Create(0, false);
		
		for(int x = 0; x < BoardSize; x++)
			for(int y = 0; y < BoardSize; y++)
			{
				output.mInitialBoard[x][y] = boardToClone.mInitialBoard[x][y];
				output.mPlayer1Board[x][y] = boardToClone.mPlayer1Board[x][y];
				output.mPlayer2Board[x][y] = boardToClone.mPlayer2Board[x][y];
			}
		
		return output;
	}
	
	public static Point[] GetPlayerSquares(int player)
	{
		Point[] ret = new Point[4];
		
		if(player == 0)
		{
			ret[0] = new Point(0, 0);
    		ret[1] = new Point(1, 0);
    		ret[2] = new Point(2, 1);
    		ret[3] = new Point(0, 2);
		}
		else
		{
			ret[0] = new Point(2, 0);
			ret[1] = new Point(0, 1);
    		ret[2] = new Point(1, 2);
    		ret[3] = new Point(2, 2);
		}
		
		return ret;
	}
	
	public static int GetPlayerTerritory(Point point)
	{
		if(point == null)
			return -1;
		
		int boardX = point.x / SquareSize;
		int boardY = point.y / SquareSize;
		
		if(boardX == 1 && boardY == 1)
			return -1;
		
		if((boardX == 0 && boardY == 0) ||
				(boardX == 1 && boardY == 0) ||
				(boardX == 2 && boardY == 1) ||
				(boardX == 0 && boardY == 2))
			return 0;
		
		return 1;
	}
	
	/*
     * Helper method so boards can be defined intuitively and indexed intuitively
     */
    public static byte[][] TransposeBoard(byte[][] board)
    {
    	byte[][] output = CreateBoard();
        
        for(int x = 0; x<BoardSize; x++)
        	for(int y=0; y<BoardSize; y++)
        	{
        		output[x][y] = board[y][x];
        	}
        
        return output;
    }
    
    public static Point GetSquare(Point cell)
    {
    	Point square = new Point();
    	
    	square.x = cell.x / SquareSize;
    	square.y = cell.y / SquareSize;
    	
    	return square;
    }
    
    public byte[][] GetSubBoard(int player)
    {
    	if(player == 0)
    		return mPlayer1Board;
    	if(player == 1)
    		return mPlayer2Board;
    	else
    		return mInitialBoard;
    }
    
    public byte[][] GetPendingMoves()
    {
    	return mPendingMove;
    }
    
    public byte GetCell(Point cell, boolean includePending)
    {
    	byte[][] fullBoard = GetFullBoard(includePending);
    	return fullBoard[cell.x][cell.y];
    }
    
    public void SetCell(Point cell, byte value, int playerTurn, boolean commitNow)
    {
    	mPendingMove = CreateBoard();
    	
    	if(cell != null)
    	{
	    	mPendingMove[cell.x][cell.y] = value;
	    	
	    	//Set invalid cells
	    	for(int x=0; x<BoardSize; x++)
	    		for(int y=0; y<BoardSize; y++)
	    			if(GetFullBoard(true)[x][y] == 0 && !IsCellValid(new Point(x, y)))
	    				{
	    					DebugLog.Write(String.format("Cetting cell (%d, %d) invalid", x, y), null);
	    					Log.i("SudokuBoard", String.format("Setting cell (%d, %d) invalid", x, y));
	    					mPendingPlayerTurn = playerTurn;
	    					mPendingMove[x][y] = -1;
	    				}
    	}
    	
    	if(commitNow)
    		Commit();
    }
    
    public void ClearPending()
    {
    	mPendingMove = null;
    }
    
    public void Commit()
    {
    	if(mPendingMove == null)
    		return;
    	
    	for(int x=0; x<BoardSize; x++)
    		for(int y=0; y<BoardSize; y++)
    			if(mPendingMove[x][y] != 0)
    			{
    				int playerTerritory = GetPlayerTerritory(new Point(x, y));
    				if(playerTerritory < 0)
    					playerTerritory = mPendingPlayerTurn;
    				if(playerTerritory == 0)
    					mPlayer1Board[x][y] = mPendingMove[x][y];
    				else
    					mPlayer2Board[x][y] = mPendingMove[x][y];
    			}
    	
    	mPendingMove = null;
    }
    
    public byte[][] GetFullBoard(boolean includePending)
    {
    	byte[][] fullBoard = CreateBoard();
    	
    	for(int x = 0; x < BoardSize; x++)
    		for(int y = 0; y < BoardSize; y++)
    			if(mInitialBoard != null && mInitialBoard[x][y] != 0)
    				fullBoard[x][y] = mInitialBoard[x][y];
    			else if(mPlayer1Board != null && mPlayer1Board[x][y] != 0)
    				fullBoard[x][y] = mPlayer1Board[x][y];
    			else if(mPlayer2Board != null && mPlayer2Board[x][y] != 0)
    				fullBoard[x][y] = mPlayer2Board[x][y];
    			else if(includePending && mPendingMove != null && mPendingMove[x][y] != 0)
    				fullBoard[x][y] = mPendingMove[x][y];
    	
    	return fullBoard;
    }
    
    public boolean[] GetCellOptions(Point cell, boolean includePending)
    {
    	byte[][] fullBoard = GetFullBoard(includePending);
    	
    	//Start by assuming all options are true
    	boolean[] options = new boolean[BoardSize + 1];
    	for(int i=1; i<BoardSize + 1; i++)
    		options[i] = true;
    	
    	//Search the row for existing values
    	for(int x = 0; x<BoardSize; x++)
    		if(x != cell.x && fullBoard[x][cell.y] > 0)
    			options[fullBoard[x][cell.y]] = false;
    	
    	//Search the column for existing values
    	for(int y = 0; y<BoardSize; y++)
    		if(y != cell.y && fullBoard[cell.x][y] > 0)
    			options[fullBoard[cell.x][y]] = false;
    	
    	//Search the square for existing values
    	int squareX = cell.x / SquareSize;
    	int squareY = cell.y / SquareSize;
    	for(int x = 0; x<SquareSize; x++)
    		for(int y=0; y<SquareSize; y++)
    		{
    			int xValue = squareX * SquareSize + x;
    			int yValue = squareY * SquareSize + y;
    			int curValue = fullBoard[xValue][yValue];
    			if(cell.x != xValue && cell.y != yValue && curValue > 0)
    				options[curValue] = false;
    		}
    	
    	return options;
    }
    
    public boolean[] GetSquareOptions(Point square, boolean includePending)
    {
    	byte[][] fullBoard = GetFullBoard(includePending);
    	
    	//For each cell in the square, find the options
    	//Combine the options together for all squares
    	
    	boolean[] options = new boolean[BoardSize + 1];
    	
    	for(int x = 0; x<SquareSize; x++)
    		for(int y=0; y<SquareSize; y++)
    		{
    			int xValue = square.x * SquareSize + x;
    			int yValue = square.y * SquareSize + y;
    			int curValue = fullBoard[xValue][yValue];
    			if(curValue > 0)
    				options[curValue] = false;
    			else
    			{
    				boolean[] tempOptions = GetCellOptions(new Point(xValue, yValue), includePending);
    				for(int i=0; i < options.length; i++)
    					options[i] |= tempOptions[i];
    			}
    		}

    	
    	return options;
    }
    
    public boolean[] GetPendingSquareOptions(Point square)
    {
    	//For each cell in the square, find the options
    	//Combine the options together for all squares
    	
    	boolean[] options = new boolean[BoardSize + 1];
    	
    	for(int x = 0; x<SquareSize; x++)
    		for(int y=0; y<SquareSize; y++)
    		{
    			int xValue = square.x * SquareSize + x;
    			int yValue = square.y * SquareSize + y;
    			int curValue = mPendingMove[xValue][yValue];
    			if(curValue > 0)
    				options[curValue] = false;
    			else
    			{
    				boolean[] tempOptions = GetCellOptions(new Point(xValue, yValue), true);
    				for(int i=0; i < options.length; i++)
    					options[i] |= tempOptions[i];
    			}
    		}

    	
    	return options;
    }
    
    public boolean IsCellValid(Point cell)
    {
    	boolean[] options = GetCellOptions(cell, true);
    	
    	for(int i=1; i<options.length; i++)
    		if(options[i])
    		{
    			//Log.i("SudokuBoard", String.format("Cell (%d, %d) has %d as an option", cell.x, cell.y, i));
    			return true;
    		}
    	
    	return false;
    }
    
    public boolean IsSquareValid(Point square)
    {
    	
    	return true;
    }
    
    public boolean CheckBoard(boolean requireCorrect)
    {
    	byte[][] fullBoard = GetFullBoard(true);
    	
    	for(int x = 0; x < BoardSize; x++)
    		for(int y = 0; y < BoardSize; y++)
    			if(fullBoard[x][y] == 0)
    				return false;

    	//Now make sure the puzzle is logically correct if required
    	if(requireCorrect)
    	{
	    	for(int i=0; i<BoardSize; i++)
	    		if(!CheckRow(i))
	    		{
	    			Log.i("SudokuLogic", String.format("Row %d not done yet", i));
	    			return false;
	    		}
	    	
	    	for(int i=0; i<BoardSize; i++)
	    		if(!CheckColumn(i))
	    		{
	    			Log.i("SudokuLogic", String.format("Column %d not done yet", i));
	    			return false;
	    		}
	    	
	    	for(int x = 0; x < SquareSize; x++)
	    		for(int y = 0; y < SquareSize; y++)
	    			if(!CheckSquare(new Point(x, y)))
	    			{
	    				Log.i("SudokuLogic", String.format("Square (%d, %d) not done yet", x, y));
	    				return false;
	    			}
    	}
    	return true;
    }
    
    public boolean CheckRow(int rowId)
    {
    	byte[][] fullBoard = GetFullBoard(true);
    	
    	boolean[] values = new boolean[BoardSize];
    	
    	for(int i=0; i<BoardSize; i++)
    	{
    		int curValue = fullBoard[i][rowId]; 
    		if(curValue == 0)
    			return false;
    		if(curValue > 0)
    			values[curValue - 1] = true;
    	}
    	
    	for(int i=0; i<BoardSize; i++)
    		if(!values[i])
    			return false;
    	
    	return true;
    }
    
    public boolean CheckColumn(int columnId)
    {
    	byte[][] fullBoard = GetFullBoard(true);
    	boolean[] values = new boolean[BoardSize];
    	
    	for(int i=0; i<BoardSize; i++)
    	{
    		int curValue = fullBoard[columnId][i]; 
    		if(curValue == 0)
    			return false;
    		if(curValue > 0)
    			values[curValue - 1] = true;
    	}
    	
    	for(int i=0; i<BoardSize; i++)
    		if(!values[i])
    			return false;
    	
    	return true;
    }
    
    public boolean CheckSquare(Point square)
    {
    	byte[][] fullBoard = GetFullBoard(true);
    	boolean[] values = new boolean[BoardSize];
    	boolean foundInvalid = false;
    	
    	for(int x = 0; x<SquareSize; x++)
    		for(int y=0; y<SquareSize; y++)
    		{
    			int curValue = fullBoard[square.x * SquareSize + x][square.y * SquareSize + y];
    			if(curValue < 0)
    				foundInvalid = true;
    			else if(curValue > 0)
    				values[curValue - 1] = true;
    			else
    				return false; //found a blank cell
    		}
    	
    	//Invalid cells only happen in two-player mode
    	//If there are no blank cells and we found an invalid, call the square good
    	if(foundInvalid)
    		return true;
    	
    	//Make sure we saw all the digits 1-9
    	for(int i=0; i<BoardSize; i++)
    		if(!values[i])
    			return false;
    	
    	return true;
    }
    
    @Override
    public String toString()
    {
    	String ret = "";
    	byte[][] fullBoard = GetFullBoard(true);
    	
    	for(int y = 0; y < BoardSize; y++)
    	{
    		if(y > 0)
    			ret += "\n";
    		for(int x= 0; x < BoardSize; x++)
    		{
    			if (x > 0)
    				ret +=",";
    			ret += fullBoard[x][y];
    		}
    	}
    	
    	return ret;
    }
}
