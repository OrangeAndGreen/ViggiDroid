package com.Dave.Files;


/*
 * Metric uses meters and kilometers
 * Imperial uses feet and miles
 * 
 * Small values are meters and feet
 * Large values are kilometers and miles
 * 
 * Speeds are always KPH or MPH
 * 
 */


public class GPSUnits
{
	public static String METRIC = "Metric";
	public static String IMPERIAL = "Imperial";
	
	public static String LARGE = "Large";
	public static String SMALL = "Small";
	
	public static CharSequence METERS = "Meters";
	public static CharSequence FEET = "Feet";
	public static CharSequence MILES = "Miles";
	public static CharSequence KILOMETERS = "Kilometers";
	
	public static CharSequence SECONDS = "Seconds";
	public static CharSequence MINUTES = "Minutes";
	public static CharSequence HOURS = "Hours";
	
	
	public String UnitSystem = null;
	
	public String AltitudeUnits = null;
	public String SpeedDistanceUnits = null;
	public String SpeedTimeUnits = null;
	public String AccuracyUnits = null;
	public String DistanceUnits = null;
	
	public GPSUnits()
	{
		SetUnitSystem(METRIC);
	}
	
	public GPSUnits(String system)
	{
		SetUnitSystem(system);
	}
	
	public void SetUnitSystem(String system)
	{
		UnitSystem = system;
		if(system.equals(METRIC))
		{
			AltitudeUnits = METERS.toString();
			SpeedDistanceUnits = KILOMETERS.toString();
			SpeedTimeUnits = HOURS.toString();
			AccuracyUnits = METERS.toString();
			DistanceUnits = KILOMETERS.toString();
		}
		else
		{
			AltitudeUnits = FEET.toString();
			SpeedDistanceUnits = MILES.toString();
			SpeedTimeUnits = HOURS.toString();
			AccuracyUnits = FEET.toString();
			DistanceUnits = MILES.toString();
		}
	}
	
	/* Converters */
	public static float ConvertDistance(String unitsIn, float value, String unitsOut)
	{
		float ret = value;
		//Distances supported: feet, meters, miles, km
		if(unitsIn.equals(unitsOut))
			return ret;
		
		if(unitsIn.equals(METERS))
		{
			if(unitsOut.equals(FEET))
				ret = value * 3.28084f;
			else if(unitsOut.equals(MILES))
				ret = value / 1609.34f;
			else if(unitsOut.equals(KILOMETERS))
				ret = value / 1000;
		}
		else if(unitsIn.equals(FEET))
		{
			if(unitsOut.equals(METERS))
				ret = value / 3.28084f;
			else if(unitsOut.equals(MILES))
				ret = value / 5280;
			else if(unitsOut.equals(KILOMETERS))
				ret = value / 3280.84f;
		}
		else if(unitsIn.equals(MILES))
		{
			if(unitsOut.equals(FEET))
				ret = value * 5280;
			else if(unitsOut.equals(METERS))
				ret = value * 1609.34f;
			else if(unitsOut.equals(KILOMETERS))
				ret = value * 1.60934f;
		}
		else if(unitsIn.equals(KILOMETERS))
		{
			if(unitsOut.equals(FEET))
				ret = value * 3280.84f;
			else if(unitsOut.equals(MILES))
				ret = value / 1.60934f;
			else if(unitsOut.equals(METERS))
				ret = value * 1000;
		}
		
		return ret;
	}
	
	public static double ConvertDistance(String unitsIn, double value, String unitsOut)
	{
		double ret = value;
		//Distances supported: feet, meters, miles, km
		if(unitsIn.equals(unitsOut))
			return ret;
		
		if(unitsIn.equals(METERS))
		{
			if(unitsOut.equals(FEET))
				ret = value * 3.28084;
			else if(unitsOut.equals(MILES))
				ret = value / 1609.34;
			else if(unitsOut.equals(KILOMETERS))
				ret = value / 1000;
		}
		else if(unitsIn.equals(FEET))
		{
			if(unitsOut.equals(METERS))
				ret = value / 3.28084;
			else if(unitsOut.equals(MILES))
				ret = value / 5280;
			else if(unitsOut.equals(KILOMETERS))
				ret = value / 3280.84;
		}
		else if(unitsIn.equals(MILES))
		{
			if(unitsOut.equals(FEET))
				ret = value * 5280;
			else if(unitsOut.equals(METERS))
				ret = value * 1609.34;
			else if(unitsOut.equals(KILOMETERS))
				ret = value * 1.60934;
		}
		else if(unitsIn.equals(KILOMETERS))
		{
			if(unitsOut.equals(FEET))
				ret = value * 3280.84;
			else if(unitsOut.equals(MILES))
				ret = value / 1.60934;
			else if(unitsOut.equals(METERS))
				ret = value * 1000;
		}
		
		return ret;
	}
	
	public static float ConvertTime(String unitsIn, float value, String unitsOut)
	{
		float ret = value;
		
		if(unitsIn.equals(unitsOut))
			return ret;
		
		if(unitsIn.equals(SECONDS))
		{
			if(unitsOut.equals(MINUTES))
				ret = value / 60;
			else if(unitsOut.equals(HOURS))
				ret = value / 3600;
		}
		else if(unitsIn.equals(MINUTES))
		{
			if(unitsOut.equals(SECONDS))
				ret = value * 60;
			else if(unitsOut.equals(HOURS))
				ret = value / 60;
		}
		else if(unitsIn.equals(HOURS))
		{
			if(unitsOut.equals(MINUTES))
				ret = value * 60;
			else if(unitsOut.equals(SECONDS))
				ret = value * 3600;
		}
		
		return ret;
	}
	
	public static float ConvertSpeed(String distanceUnitsIn, String timeUnitsIn, float value, String distanceUnitsOut, String timeUnitsOut)
	{
		float ret = ConvertDistance(distanceUnitsIn, value, distanceUnitsOut);
		ret /= ConvertTime(timeUnitsIn, 1, timeUnitsOut);
		
		return ret;
	}
	
	public String GetAltitudeAbbreviation()
	{
		return GetDistanceAbbreviation(AltitudeUnits);
	}
	
	public String GetAccuracyAbbreviation()
	{
		return GetDistanceAbbreviation(AccuracyUnits);
	}
	
	public static String GetDistanceAbbreviation(String units)
	{
		String ret = "-";
		
		if(units.equals(METERS))
			ret = "m";
		else if(units.equals(FEET))
			ret = "f";
		else if(units.equals(MILES))
			ret = "M";
		else if(units.equals(KILOMETERS))
			ret = "K";
		
		return ret;
	}
	
	public String GetTimeAbbreviation()
	{
		return GetTimeAbbreviation(SpeedTimeUnits);
	}
	
	public static String GetTimeAbbreviation(String units)
	{
		String ret = "-";
		
		if(units.equals(SECONDS))
			ret = "S";
		else if(units.equals(MINUTES))
			ret = "M";
		else if(units.equals(HOURS))
			ret = "H";
		
		return ret;
	}
	
	public String GetSpeedAbbreviation()
	{
		return GetSpeedAbbreviation(SpeedDistanceUnits, SpeedTimeUnits);
	}
	
	public static String GetSpeedAbbreviation(String distanceUnits, String timeUnits)
	{
		return GetDistanceAbbreviation(distanceUnits) + "P" + GetTimeAbbreviation(timeUnits);
	}
}
