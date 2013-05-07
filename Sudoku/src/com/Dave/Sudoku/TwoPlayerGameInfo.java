package com.Dave.Sudoku;

import java.util.Calendar;

import android.util.Log;

public class TwoPlayerGameInfo
{
	public int GameId = 0;
	
	public String Player1 = null;
	public int Player1Score = 0;
	public String Player2 = null;
	public int Player2Score = 0;
	
	public Calendar StartDate = null;
	public Calendar PlayDate = null;
	
	public int Turn = 0;
	
	public static TwoPlayerGameInfo FromString(String input)
	{
		TwoPlayerGameInfo ret = new TwoPlayerGameInfo();
		
		Log.d("TwoPlayerGameInfo", input);
		
		String[] parts = input.split(",");
		
		ret.GameId = Integer.parseInt(parts[0]);
		ret.Player1 = parts[1];
		ret.Player1Score = Integer.parseInt(parts[2]);
		ret.Player2 = parts[3];
		ret.Player2Score = Integer.parseInt(parts[4]);
		ret.StartDate = ParseCalendar(parts[5]);
		ret.PlayDate = ParseCalendar(parts[6]);
		ret.Turn = Integer.parseInt(parts[7].trim());
		
		return ret;
	}
	
	private static Calendar ParseCalendar(String input)
	{
		Calendar ret = Calendar.getInstance();
		
		String[] parts = input.split(":");
		
		ret.set(Calendar.YEAR, Integer.parseInt(parts[0]));
		ret.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
		ret.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
		ret.set(Calendar.HOUR, Integer.parseInt(parts[3]));
		ret.set(Calendar.MINUTE, Integer.parseInt(parts[4]));
		ret.set(Calendar.SECOND, Integer.parseInt(parts[5]));
		
		return ret;
	}
}
