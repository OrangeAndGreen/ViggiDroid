package com.Dave.Sudoku;

import java.util.List;

import android.graphics.Point;

public class ScoringConcept3 implements IScoring
{
	public String GetName()
	{
		return "Least square";
	}
	
	public int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier)
	{
		if(number <= 0)
			return 0;
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		List<Point> friendlySquares = SudokuBoard.GetPlayerSquares(playerTurn);
		List<Point> enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		//The score is the number of cells filled in the least-filled square
		int minFilled = 9;
		for(int i = 0; i<friendlySquares.size(); i++)
		{
			int numFilled = 0;
			for(int x=0; x<SudokuBoard.SquareSize; x++)
				for(int y=0; y<SudokuBoard.SquareSize; y++)
				{
					Point cell = new Point(friendlySquares.get(i).x * SudokuBoard.SquareSize + x, friendlySquares.get(i).y * SudokuBoard.SquareSize + y);
					if(board.GetCell(cell, true) > 0 || (number > 0 && cell.x == point.x && cell.y == point.y))
						numFilled++;
				}
			if(numFilled < minFilled)
				minFilled = numFilled;
		}
				
		
		int score = minFilled * board.GetCellMultiplier(point);
		if(multiplier > 0)
			score *= multiplier;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Add that number to the score if so
		
		for(int i=0; i<enemySquares.size(); i++)
		{
			List<Byte> initialSquareOptions = board.GetSquareOptions(enemySquares.get(i), false);
			
			board.SetCell(point, number, playerTurn, false);
			
			List<Byte> finalSquareOptions = board.GetSquareOptions(enemySquares.get(i), true);
			
			for(int n = 0; n < initialSquareOptions.size(); n++)
				if(!finalSquareOptions.contains(initialSquareOptions.get(n)))
				{
					DebugLog.Write(String.format("%d invalidated in square (%d, %d)", n, enemySquares.get(i).x, enemySquares.get(i).y), null);
					//Log.i("ScoringConcept1", String.format("Adding %d to score", n));
					//score += n;
				}
		}
		
		DebugLog.Write("Score: " + score, null);
		
		return score;
	}

	public int GetNextMultiplier(SudokuBoard board, Point point, byte number, int currentMultiplier)
	{
		int multiplier = currentMultiplier;
		
		if(number <= 0)
			return 0;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Increment the multiplier if so
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		List<Point> enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.size(); i++)
		{
			List<Byte> initialSquareOptions = board.GetSquareOptions(enemySquares.get(i), false);
			
			board.SetCell(point, number, playerTurn, false);
			
			List<Byte> finalSquareOptions = board.GetSquareOptions(enemySquares.get(i), true);
			boolean incrementMultiplier = false;
			for(int n = 0; n < initialSquareOptions.size(); n++)
				if(!finalSquareOptions.contains(initialSquareOptions.get(n)))
				{
					if(multiplier == 0)
						multiplier = 1;
					
					//Log.i("ScoringConcept1", String.format("Incrementing multiplier to %d", multiplier + 1));
					incrementMultiplier = true;
					break;
				}
			
			if(incrementMultiplier)
			{
				multiplier++;
				
				DebugLog.Write("Increasing multiplier to " + multiplier, null);
				
				break;
			}
		}
		
		return multiplier;
	}
}
