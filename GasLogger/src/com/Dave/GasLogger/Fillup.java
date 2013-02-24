package com.Dave.GasLogger;

import android.database.Cursor;

public class Fillup
{
	public long RowID = -1;

	public String Date = null;
	
	public int Mileage = -1;
	public int Octane = -1;
	public float Gallons = 0;
	public float CostPerGallon = 0;
	public String Indicator = null;
	public String Station = null;
	public String Car = null;
	public String State = null;
	public String Location = null;
	public int Mte = 0;
	public int Receipt = 0;	
	
	public int LowMileage = -1;
	public float LowDays = -1;

	public static Fillup FromCursor(Cursor cursor)
	{
		Fillup ret = new Fillup();
		
		ret.RowID = cursor.getLong(0);
		ret.Date = cursor.getString(1);
		ret.Mileage = cursor.getInt(2);
		ret.Octane = cursor.getInt(3);
		ret.Gallons = cursor.getFloat(4);
		ret.CostPerGallon = cursor.getFloat(5);
		ret.Indicator = cursor.getString(6);
		ret.Station = cursor.getString(7);
		ret.Car = cursor.getString(8);
		ret.State = cursor.getString(9);
		ret.Location = cursor.getString(10);
		ret.Mte = cursor.getInt(11);
		ret.Receipt = cursor.getInt(12);
		ret.LowMileage = cursor.getInt(13);
		ret.LowDays = cursor.getFloat(14);

		return ret;
	}
	
	public static Fillup FromString(String string)
	{
		Fillup ret = new Fillup();
		
		String[] parts = string.split("\t");
		
		if(parts.length < 10)
			return null;
		
		ret.RowID = DBAdapter.IDFromDate(parts[0]);
		ret.Date = parts[0];
		ret.Mileage = Integer.parseInt(parts[1]);
		ret.Octane = Integer.parseInt(parts[2]);
		ret.Gallons = Float.parseFloat(parts[3]);
		ret.CostPerGallon = Float.parseFloat(parts[4]);
		ret.Indicator = parts[5];
		ret.Station = parts[6];
		ret.Location = parts[7].trim();
		ret.LowMileage = Integer.parseInt(parts[8]);
		ret.LowDays = Float.parseFloat(parts[9]);

		return ret;
	}
	
	public String toString()
	{
		return String.format("%s\t%d\t%d\t#.03f\t%.03f\t%s\t%s\t%s\t%d\t%d\n", Date, Mileage, Octane, Gallons,
																			CostPerGallon, Indicator, Station, Location,
																			LowMileage, LowDays);
	}
}
