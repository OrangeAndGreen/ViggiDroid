package com.Dave.Sudoku;

public class MyRandom
{
	public static int Get(int min, int max)
	{
		int range = max - min + 1;
		
		int random = (int)Math.round((Math.random() * range - 0.5) + min);
		
		if(random < min)
			random = min;
		if(random > max)
			random = max;
		
		return random;
	}
}
