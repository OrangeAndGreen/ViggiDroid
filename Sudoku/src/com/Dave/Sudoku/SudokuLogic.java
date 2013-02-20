package com.Dave.Sudoku;

import android.graphics.Point;
import android.util.Log;

public class SudokuLogic
{
	public static final int BoardSize = 9;
	
	public static final int[][] BlankBoard =   {{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0},
												{0, 0, 0, 0, 0, 0, 0, 0, 0}
											   };

	/*
	public static final int[][] DefaultBoard = {{5, 3, 0, 0, 7, 0, 0, 0, 0},
											    {6, 0, 0, 1, 9, 5, 0, 0, 0},
											    {0, 9, 8, 0, 0, 0, 0, 6, 0},
											    {8, 0, 0, 0, 6, 0, 0, 0, 3},
											    {4, 0, 0, 8, 0, 3, 0, 0, 1},
											    {7, 0, 0, 0, 2, 0, 0, 0, 6},
											    {0, 6, 0, 0, 0, 0, 2, 8, 0},
											    {0, 0, 0, 4, 1, 9, 0, 0, 5},
											    {0, 0, 0, 0, 8, 0, 0, 7, 9}
											   };

	public static final int[][] NearlyCompleteBoard =  {{5, 3, 4, 6, 7, 8, 9, 1, 2},
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
	
	public static int[][] CreateBoard(int numberToFill)
	{
		return CreateBoard(numberToFill, false);
	}
	
	public static int[][] CreateBoard(int numberToFill, boolean makeFairForTwoPlayer)
	{
		int[][] output = new int[BoardSize][];
        for(int i=0; i<BoardSize; i++)
        	output[i] = new int[BoardSize];
        
        int numPlayers = 1;
        int[] player1Choices = null;
        if(makeFairForTwoPlayer)
        {
        	numPlayers = 2;
        	player1Choices = new int[numberToFill];
        	output[4][4] = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0) + 1;
        }
        for(int player=0; player < numPlayers; player++)
	        for(int fillIndex=0; fillIndex<numberToFill; fillIndex++)
	        {
	        	//Randomly set some cells without breaking the rules
	        	//First: Choose the value to fill in
	        	
	        	//Generate a value randomly
	        	int value = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0) + 1;
	        	if(makeFairForTwoPlayer)
	        	{
	        		if(player == 0) //save the choice
	        			player1Choices[fillIndex] = value;
	        		else	//do what player1 did
	        			value = player1Choices[fillIndex];
	        	}
	        	
	        	while(true)
	        	{
	        		//Randomly pick a cell
	        		int x = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0);
	        		int y = Math.max(Math.min((int)Math.round(Math.random() * BoardSize - .5), BoardSize - 1), 0);
	        		Point cell = new Point(x, y);
	        		
	        		//Make sure the cell is empty
	        		//For two-player, make sure we are in the right territory
	        		//Make sure the cell is legal based on other already filled values
	        		if(output[x][y] > 0
	        				|| (makeFairForTwoPlayer && GetPlayerTerritory(cell) != player)
	        				|| !GetOptions(output, cell)[value])
	        		{
	        			continue;
	        		}
	        		
	        		//Set the cell in the map
	        		output[x][y] = value;
	        		break;
	        	}
	        }
        
        return output;
	}
	
	public static int[][] CreateBoard(int[][] boardToClone)
	{
		int[][] output = CreateBoard(0);
		
		for(int x = 0; x < BoardSize; x++)
			for(int y = 0; y < BoardSize; y++)
				output[x][y] = boardToClone[x][y];
		
		return output;
	}
	
	public static int GetPlayerTerritory(Point point)
	{
		int numSquares = (int)Math.sqrt(SudokuLogic.BoardSize);
		int boardX = point.x / numSquares;
		int boardY = point.y / numSquares;
		
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
    public static int[][] TransposeBoard(int[][] board)
    {
    	int[][] output = CreateBoard(0);
        
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
    	
    	int squareSize = (int)Math.sqrt(BoardSize);
    	
    	square.x = cell.x / squareSize;
    	square.y = cell.y / squareSize;
    	
    	return square;
    }
    
    public static int[][] GetFullBoard(int[][] initialBoard, int[][] player1Board, int[][] player2Board)
    {
    	int[][] fullBoard = CreateBoard(0);
    	
    	for(int x = 0; x < BoardSize; x++)
    		for(int y = 0; y < BoardSize; y++)
    			if(initialBoard != null && initialBoard[x][y] != 0)
    				fullBoard[x][y] = initialBoard[x][y];
    			else if(player1Board != null && player1Board[x][y] != 0)
    				fullBoard[x][y] = player1Board[x][y];
    			else if(player2Board != null && player2Board[x][y] != 0)
    				fullBoard[x][y] = player2Board[x][y];
    	
    	return fullBoard;
    }
    
    public static boolean[] GetOptions(int[][] fullBoard, Point point)
    {
    	boolean[] options = new boolean[BoardSize + 1];
    	for(int i=1; i<BoardSize + 1; i++)
    		options[i] = true;
    	
    	for(int x = 0; x<BoardSize; x++)
    		if(x != point.x && fullBoard[x][point.y] > 0)
    			options[fullBoard[x][point.y]] = false;
    	
    	for(int y = 0; y<BoardSize; y++)
    		if(y != point.y && fullBoard[point.x][y] > 0)
    			options[fullBoard[point.x][y]] = false;
    	
    	
    	int numSquares = (int)Math.sqrt(BoardSize);
    	int squareX = point.x / numSquares;
    	int squareY = point.y / numSquares;
    	for(int x = 0; x<numSquares; x++)
    		for(int y=0; y<numSquares; y++)
    		{
    			int xValue = squareX * numSquares + x;
    			int yValue = squareY * numSquares + y;
    			int curValue = fullBoard[xValue][yValue];
    			if(point.x != xValue && point.y != yValue && curValue > 0)
    				options[curValue] = false;
    		}
    	
    	return options;
    }
    
    public static boolean IsSquareValid(int[][] fullBoard, Point point)
    {
    	boolean[] options = GetOptions(fullBoard, point);
    	
    	for(int i=0; i<options.length; i++)
    		if(options[i])
    			return true;
    	
    	return false;
    }
    
    public static boolean CheckBoard(int[][] initialBoard, int[][] player1Board, int[][] player2Board, boolean requireCorrect)
    {
    	int[][] fullBoard = GetFullBoard(initialBoard, player1Board, player2Board);
    	
    	for(int x = 0; x < BoardSize; x++)
    		for(int y = 0; y < BoardSize; y++)
    			if(fullBoard[x][y] == 0)
    				return false;

    	//Now make sure the puzzle is logically correct if required
    	if(requireCorrect)
    	{
	    	for(int i=0; i<BoardSize; i++)
	    		if(!CheckRow(fullBoard, i))
	    		{
	    			Log.i("SudokuLogic", String.format("Row %d not done yet", i));
	    			return false;
	    		}
	    	
	    	for(int i=0; i<BoardSize; i++)
	    		if(!CheckColumn(fullBoard, i))
	    		{
	    			Log.i("SudokuLogic", String.format("Column %d not done yet", i));
	    			return false;
	    		}
	    	
	    	int numSquares = (int)Math.sqrt(BoardSize);
	    	for(int x = 0; x < numSquares; x++)
	    		for(int y = 0; y < numSquares; y++)
	    			if(!CheckSquare(fullBoard, x, y))
	    			{
	    				Log.i("SudokuLogic", String.format("Square (%d, %d) not done yet", x, y));
	    				return false;
	    			}
    	}
    	return true;
    }
    
    public static boolean CheckRow(int[][] fullBoard, int rowId)
    {
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
    
    public static boolean CheckColumn(int[][] fullBoard, int columnId)
    {
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
    
    public static boolean CheckSquare(int[][] fullBoard, int squareX, int squareY)
    {
    	boolean[] values = new boolean[BoardSize];
    	boolean foundInvalid = false;
    	
    	int numSquares = (int)Math.sqrt(BoardSize);
    	
    	for(int x = 0; x<numSquares; x++)
    		for(int y=0; y<numSquares; y++)
    		{
    			int curValue = fullBoard[squareX * numSquares + x][squareY * numSquares + y];
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
}
