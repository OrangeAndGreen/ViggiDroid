package com.Dave.TravelTracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.Dave.Files.ErrorFile;
import com.Dave.Files.GPSUnits;

public class TravelTrackerSettings
{
	public GPSUnits Units = null;
	
	public String LogDirectory = null;
	public int LogInterval = 0;
	public int HistoryLength = 0;
	public int RunningAverageLength = 0;
	public float StoppedThreshold = 0;
	
	public boolean ShowSatellites = false;
	public boolean ShowTraffic = false;
	
	public String Filename = null;
	public String CurrentPage = null;
	public boolean IsRecording = false;
	public boolean IsStationary = false;
	
	//Keys
	public static final String LogDirectoryKey = "LogDirectory";
	public static final String LogIntervalKey = "LogInterval";
	public static final String HistoryLengthKey = "HistoryLength";
	public static final String RunningAverageKey = "RunningAverage";
	public static final String StoppedThresholdKey = "StoppedThreshold";
	public static final String ShowSatellitesKey = "ShowSatellites";
	public static final String ShowTrafficKey = "ShowTraffic";
	public static final String FilenameKey = "LastFilename";
	public static final String CurrentPageKey = "CurrentPage";
	public static final String IsRecordingKey = "IsRecording";
	public static final String IsStationaryKey = "IsStationary";
	public static final String UnitSystemKey = "UnitSystem";
	public static final String AltitudeUnitsKey = "AltitudeUnits";
	public static final String SpeedDistanceUnitsKey = "SpeedDistanceUnits";
	public static final String SpeedTimeUnitsKey = "SpeedTimeUnits";
	public static final String AccuracyUnitsKey = "AccuracyUnits";
	public static final String DistanceUnitsKey = "DistanceUnits";
	
	
	public TravelTrackerSettings(SharedPreferences prefs, Context context)
	{
		try
		{
			LogDirectory = prefs.getString(LogDirectoryKey, "01Tracks");
			LogInterval = prefs.getInt(LogIntervalKey, 1000);
			HistoryLength = prefs.getInt(HistoryLengthKey, 5 * 60000);
			RunningAverageLength = prefs.getInt(RunningAverageKey, 60);
			StoppedThreshold = prefs.getFloat(StoppedThresholdKey, 1);
			
			ShowSatellites = prefs.getBoolean(ShowSatellitesKey, false);
			ShowTraffic = prefs.getBoolean(ShowTrafficKey, false);
			
			Filename = prefs.getString(FilenameKey, null);
			CurrentPage = prefs.getString(CurrentPageKey, null);
			IsRecording = prefs.getBoolean(IsRecordingKey, false);
			IsStationary = prefs.getBoolean(IsStationaryKey, false);
			
			Units = new GPSUnits();
			
			Units.UnitSystem = prefs.getString(UnitSystemKey, GPSUnits.IMPERIAL);
			Units.SetUnitSystem(Units.UnitSystem);
			Units.AltitudeUnits = prefs.getString(AltitudeUnitsKey, GPSUnits.FEET.toString());
			Units.SpeedDistanceUnits = prefs.getString(SpeedDistanceUnitsKey, GPSUnits.MILES.toString());
			Units.SpeedTimeUnits = prefs.getString(SpeedTimeUnitsKey, GPSUnits.HOURS.toString());
			Units.AccuracyUnits = prefs.getString(AccuracyUnitsKey, GPSUnits.FEET.toString());
			Units.DistanceUnits = prefs.getString(DistanceUnitsKey, GPSUnits.MILES.toString());
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, context);
		}
	}
	
	public void Save(SharedPreferences prefs, Context context)
	{
		try
		{
			SharedPreferences.Editor editor = prefs.edit();
			
			editor.putString(LogDirectoryKey, LogDirectory);
			editor.putInt(LogIntervalKey, LogInterval);
			editor.putInt(HistoryLengthKey, HistoryLength);
			editor.putInt(RunningAverageKey, RunningAverageLength);
			editor.putFloat(StoppedThresholdKey, StoppedThreshold);
			
			editor.putBoolean(ShowSatellitesKey, ShowSatellites);
			editor.putBoolean(ShowTrafficKey, ShowTraffic);
			
			editor.putString(FilenameKey, Filename);
			editor.putString(CurrentPageKey, CurrentPage);
			editor.putBoolean(IsRecordingKey, IsRecording);
			editor.putBoolean(IsStationaryKey, IsStationary);
			
			editor.putString(UnitSystemKey, Units.UnitSystem);
			editor.putString(AltitudeUnitsKey, Units.AltitudeUnits);
			editor.putString(SpeedDistanceUnitsKey, Units.SpeedDistanceUnits);
			editor.putString(SpeedTimeUnitsKey, Units.SpeedTimeUnits);
			editor.putString(AccuracyUnitsKey, Units.AccuracyUnits);
			editor.putString(DistanceUnitsKey, Units.DistanceUnits);
			
			editor.commit();
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, context);
		}
	}

}
