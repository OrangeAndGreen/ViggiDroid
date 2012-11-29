package com.Dave.Files;

import java.util.Calendar;

import com.Dave.DateStrings.DateStrings;

import android.location.Location;

public class GPSCoordinate
{
	public static String METRIC = "Metric";
	public static String IMPERIAL = "Imperial";
	
	public static String LARGE = "Large";
	public static String SMALL = "Small";
	
	public static String METERS = "meters";
	public static String FEET = "feet";
	public static String MILES = "miles";
	public static String KILOMETERS = "kilometers";
	
	public static String SECONDS = "seconds";
	public static String MINUTES = "minutes";
	public static String HOURS = "hours";
	
	private static GPSUnits mFileUnits = null;
	
	private Location mLocation = null;
	
	public GPSCoordinate()
	{
		this(new Location(""));
	}
	
	public GPSCoordinate(Location location)
	{
		if(mFileUnits == null)
		{
			mFileUnits = new GPSUnits();
			
			mFileUnits.AccuracyUnits = GPSUnits.METERS.toString();
			mFileUnits.SpeedDistanceUnits = GPSUnits.METERS.toString();
			mFileUnits.SpeedTimeUnits = GPSUnits.SECONDS.toString();
			mFileUnits.AltitudeUnits = GPSUnits.METERS.toString();
			mFileUnits.DistanceUnits = GPSUnits.METERS.toString();
		}
		
		mLocation = new Location(location);
	}
	
	public static GPSCoordinate FromString(String input)
	{
		return null;
	}
	
	public String ToString()
	{
		Calendar date = Calendar.getInstance();
        date.setTimeInMillis(mLocation.getTime());
        String dateStr = DateStrings.GetDateTimeString(date);
        
        return String.format("%s,%d,%f,%f,%f,%f,%f,%f\n", dateStr, mLocation.getTime(), mLocation.getLongitude(),
        												  mLocation.getLatitude(), mLocation.getAltitude(),
        												  mLocation.getAccuracy(), mLocation.getBearing(), mLocation.getSpeed());
	}
	
	
	/* Data Accessors */
	public long GetTime()
	{
		return mLocation.getTime();
	}
	
	public double GetLongitude()
	{
		return mLocation.getLongitude();
	}
	
	public double GetLatitude()
	{
		return mLocation.getLatitude();
	}
	
	public double GetAltitude(GPSUnits units)
	{
		double altitude =  mLocation.getAltitude();
		return GPSUnits.ConvertDistance(mFileUnits.AltitudeUnits, altitude, units.AltitudeUnits);
	}
	
	public float GetAccuracy(GPSUnits units)
	{
		float accuracy =  mLocation.getAccuracy();
		return GPSUnits.ConvertDistance(mFileUnits.AccuracyUnits, accuracy, units.AccuracyUnits);
	}
	
	public float GetBearing()
	{
		return mLocation.getBearing();
	}
	
	public float GetSpeed(GPSUnits units)
	{
		float speed = mLocation.getSpeed();
		return GPSUnits.ConvertSpeed(mFileUnits.SpeedDistanceUnits, mFileUnits.SpeedTimeUnits, speed,
									 units.SpeedDistanceUnits, units.SpeedTimeUnits);
	}

	
	
	
	public static float GetDistance(GPSCoordinate first, GPSCoordinate second, GPSUnits units)
	{
		float[] results = new float[1];
		Location.distanceBetween(first.GetLatitude(), first.GetLongitude(), second.GetLatitude(), second.GetLongitude(), results);

		return GPSUnits.ConvertDistance(mFileUnits.DistanceUnits, results[0], units.DistanceUnits);

	}
	
	
	
	
}
