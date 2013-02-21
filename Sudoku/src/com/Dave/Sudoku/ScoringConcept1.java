package com.Dave.Sudoku;

import android.graphics.Point;
import android.util.Log;

/*
 * This scoring system awards points when a number is made invalid in an opponent's square
 */
public class ScoringConcept1 implements IScoring
{
	public int ScoreMove(int[][] fullBoard, Point point, int number, int multiplier)
	{
		if(number <= 0)
			return 0;
		
		int score = number * multiplier;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Add that number to the score if so
		
		int playerTurn = SudokuLogic.GetPlayerTerritory(point);
		
		Point[] enemySquares = SudokuLogic.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.length; i++)
		{
			boolean[] initialSquareOptions = SudokuLogic.GetSquareOptions(fullBoard, enemySquares[i]);
			
			fullBoard[point.x][point.y] = number;
			
			boolean[] finalSquareOptions = SudokuLogic.GetSquareOptions(fullBoard, enemySquares[i]);
			
			for(int n = 1; n < initialSquareOptions.length; n++)
				if(initialSquareOptions[n] && !finalSquareOptions[n])
				{
					Log.i("ScoringConcept1", String.format("Adding %d to score", n));
					score += n;
				}
			
			fullBoard[point.x][point.y]= 0; 
		}
		
		return score;
	}

	public int GetNextMultiplier(int[][] fullBoard, Point point, int number)
	{
		return 0;
	}
}
