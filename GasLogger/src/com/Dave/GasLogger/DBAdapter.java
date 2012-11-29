package com.Dave.GasLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

//For each table:
//- Retrieve all entries
//- Search for entries
//- Add entry
//- Delete entry
//- Edit entry


public class DBAdapter
{
	public static final String KEY_ROWID = "_id";
	public static final String KEY_DATE = "date";
	public static final String KEY_MILEAGE = "mileage";
	public static final String KEY_OCTANE = "octane";
	public static final String KEY_GALLONS = "gallons";
	public static final String KEY_COSTPERGALLON = "costpergallon";
	public static final String KEY_INDICATOR = "indicator";
	public static final String KEY_STATION = "station";
	public static final String KEY_LOCATION = "location";
	public static final String KEY_LOWMILEAGE = "lowmileage";
	public static final String KEY_LOWDAYS = "lowdays";
	public static final String[] FillupsColumnNames = { KEY_ROWID, KEY_DATE, KEY_MILEAGE, KEY_OCTANE, KEY_GALLONS, KEY_COSTPERGALLON,
														KEY_INDICATOR, KEY_STATION, KEY_LOCATION, KEY_LOWMILEAGE, KEY_LOWDAYS };
	
	public static final String KEY_NAME = "_name";
	public static final String KEY_VALUE = "_value";
	public static final String[] RecentsColumnNames = { KEY_ROWID, KEY_NAME, KEY_VALUE };
	public static final String RECENT_DATE = "LastEntryDate";
	public static final String RECENT_LOW_MILE = "LowMileage";
	public static final String RECENT_LOW_DATE = "LowMileageDate";
	
    public static final String[] ErrorsColumnNames = { KEY_ROWID, KEY_DATE, KEY_VALUE };
    
    private static final String TAG = "DBAdapter";
    
    private static final String DB_NAME = "fuel";
    private static String DB_TABLE_FILLUPS = "fillups";
    private static String DB_TABLE_RECENT = "recent";
    private static String DB_TABLE_ERRORS = "errors";
    private static final int DATABASE_VERSION = 1;
        
    private Context mContext = null;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    private Resources mResources = null;

    public DBAdapter(Context ctx, Resources res)
    {
        mContext = ctx;
        mResources = res;
        DBHelper = new DatabaseHelper(mContext);
    }
        
    private class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DB_NAME, null, DATABASE_VERSION);
        }

        //Creates the database
        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	//Create the "fillups" table
        	String createDBstring =
                "create table " + DB_TABLE_FILLUPS + " ("
                + KEY_ROWID + " integer primary key autoincrement, "
                + KEY_DATE + " text not null, "
                + KEY_MILEAGE + " real not null, "
                + KEY_OCTANE + "  integer not null, "
                + KEY_GALLONS + " real not null, "
                + KEY_COSTPERGALLON + " real not null, "
                + KEY_INDICATOR + " text not null, "
                + KEY_STATION + " text not null, "
                + KEY_LOCATION + " text not null, "
                + KEY_LOWMILEAGE + " real not null, "
                + KEY_LOWDAYS + " real not null" + ");";
        	
            db.execSQL(createDBstring);
            
            Toast t = Toast.makeText(mContext, "Created DB", Toast.LENGTH_LONG);
            t.show();
            
            //Create the "recents" table
            createDBstring =
                "create table " + DB_TABLE_RECENT + " ("
                + KEY_ROWID + " integer primary key autoincrement, "
                + KEY_NAME + " text not null, "
                + KEY_VALUE + " text not null" + ");";
        	
            db.execSQL(createDBstring);
            
            //Create the errors table
            createDBstring =
                "create table " + DB_TABLE_ERRORS + " ("
                + KEY_ROWID + " integer primary key autoincrement, "
                + KEY_DATE + " text not null, "
                + KEY_VALUE + " text not null);";
        	
            db.execSQL(createDBstring);

            //LoadFromFile();
        }

        //Handles upgrades to the database (currently wipes old database)
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                  + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_FILLUPS);
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_RECENT);
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_ERRORS);
            onCreate(db);
        }
    }    
    
    //---Opens the database---
    public DBAdapter Open() throws SQLException 
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---Initializes the database from a text file---
    public void LoadFromFile()
    {
    	//TODO
        //Log.w(TAG, "Initializing database from file, which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_WORDS_NEXT);
        //db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_WORDS_FINISH);
        //db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_HISTORY);
        //DBHelper.onCreate(db);

    	//try
    	//{
        //    InputStream in_s = mResources.openRawResource(R.raw.nextwords);

        //    byte[] b = new byte[in_s.available()];
        //    in_s.read(b);
        //    String listStr = new String(b);
            
        //    String[] lines = listStr.split("\n");

        //    for(int i=0; i<lines.length; i++)
        //    {
        //    	String[] parts = lines[i].split(", ");

        //    	long retVal = -1;
        //    	if(parts != null && parts.length >= 3)
        //    		retVal = AddSuggestion(DBTable.NextWords, parts[0].trim(), parts[1].trim(), parts[2].trim());
        //    	if(i == 0 && retVal < 0)
        //    	{
            		//The database has already been trained
        //    		mTrained = true;
        //    	}
        //    }
        //}
    	//catch (Exception e)
        //{
            // e.printStackTrace();
        //}
    	
    }
   
    //---closes the database---    
    public void Close() 
    {
        DBHelper.close();
    }
    
    
    //---------- FILLUP TRANSACTIONS ----------
    
    
    //Add a fillup to the database
    public boolean AddFillup(Fillup fillup)
    {
        ContentValues values = new ContentValues();
        
        //Make sure the pair isn't already in the database
        List<Fillup> fillups = FindFillups(KEY_DATE, fillup.Date);

        if(fillups == null || fillups.size() == 0)
        {
        	PopulateValues(fillup, values);
        	
        	return db.insert(DB_TABLE_FILLUPS, null, values) >= 0;
        }
        else
        {
        	return false;
        }
    }
    
    //Retrieves all fillups
    public List<Fillup> GetAllFillups(boolean ascending)
    {
    	String direction = "DESC";
    	if(ascending)
    		direction = "ASC";
    	Cursor cursor = db.query(DB_TABLE_FILLUPS, FillupsColumnNames, null, null, null, null, KEY_DATE + " " + direction);
    	
    	if(cursor != null)
    	{
    		//Query succeeded
    		List<Fillup> fillups = new ArrayList<Fillup>();
    		
    		cursor.moveToFirst();
    		
    		//Add a Fillup for each item in the Cursor
    		for(int i=0; i<cursor.getCount(); i++)
    		{
    			fillups.add(Fillup.FromCursor(cursor));
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return fillups;
    	}
    	
        return null;
    }

    //Searches for fillups
    public List<Fillup> FindFillups(String column, String value)
    {
        Cursor cursor = db.query(DB_TABLE_FILLUPS, FillupsColumnNames,
                		String.format("%s='%s'", column, value), null, null, null, KEY_DATE);
        
    	if(cursor != null)
    	{
    		//Query succeeded
    		List<Fillup> fillups = new ArrayList<Fillup>();
    		
    		cursor.moveToFirst();
    		
    		//Add a Fillup for each item in the Cursor
    		for(int i=0; i<cursor.getCount(); i++)
    		{
    			fillups.add(Fillup.FromCursor(cursor));
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return fillups;
    	}
    	
        return null;
    }
    
    //Retrieves a specific fillup
    public Fillup RetrieveFillup(long rowID)
    {
    	Fillup fillup = null;
    	
    	Cursor cursor = db.query(DB_TABLE_FILLUPS, FillupsColumnNames,
        		String.format("%s='%s'", KEY_ROWID, rowID), null, null, null, null, null);

    	if(cursor != null)
    	{
    		if(cursor.getCount() > 0)
    		{
    			//Query succeeded
    			cursor.moveToFirst();
	
    			fillup = Fillup.FromCursor(cursor);
    		}
    		cursor.close();
    	}
    	return fillup;
    }
    
    //Updates a fillup
    public boolean UpdateFillup(Fillup fillup)
    {
        ContentValues args = new ContentValues();
        
        PopulateValues(fillup, args);
                
        return db.update(DB_TABLE_FILLUPS, args, String.format("KEY_ROWID=%d", fillup.RowID), null) > 0;
    }
    
    //Deletes a particular fillup
    public boolean DeleteFillup(int rowId)
    {
        return db.delete(DB_TABLE_FILLUPS, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    private void PopulateValues(Fillup fillup, ContentValues values)
    {
    	values.put(KEY_ROWID, IDFromDate(fillup.Date));
    	values.put(KEY_DATE, fillup.Date);
    	values.put(KEY_MILEAGE, fillup.Mileage);
    	values.put(KEY_OCTANE, fillup.Octane);
    	values.put(KEY_GALLONS, fillup.Gallons);
    	values.put(KEY_COSTPERGALLON, fillup.CostPerGallon);
    	values.put(KEY_INDICATOR, fillup.Indicator);
    	values.put(KEY_STATION, fillup.Station);
    	values.put(KEY_LOCATION, fillup.Location);
    	values.put(KEY_LOWMILEAGE, fillup.LowMileage);
    	values.put(KEY_LOWDAYS, fillup.LowDays);
    }
    
    
    //---------- RECENTS TRANSACTIONS ----------
    
    
    //Retrieves all recents
    public Hashtable<String, String> GetAllRecents()
    {
    	Cursor cursor = db.query(DB_TABLE_RECENT, RecentsColumnNames, null, null, null, null, KEY_ROWID);
    	
    	if(cursor != null)
    	{
    		//Query succeeded
    		Hashtable<String, String> recents = new Hashtable<String, String>();
    		
    		cursor.moveToFirst();
    		
    		//Add a Fillup for each item in the Cursor
    		for(int i=0; i<cursor.getCount(); i++)
    		{
    			recents.put(cursor.getString(1), cursor.getString(2));
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return recents;
    	}
    	
        return null;
    }
    
    public String FindRecentValue(String name)
    {
    	String ret = null;
    	
    	Cursor cursor = db.query(DB_TABLE_RECENT, RecentsColumnNames,
        		String.format("%s='%s'", KEY_NAME, name), null, null, null, null, null);

    	if(cursor != null)
    	{
    		if(cursor.getCount() > 0)
    		{
    			//Query succeeded
    			cursor.moveToFirst();
	
    			ret = cursor.getString(2);
    		}
    		cursor.close();
    	}
    	
    	return ret;
    }
    
    public void UpdateRecentValue(String name, String value)
    {
    	ContentValues args = new ContentValues();
    	Log.e("Dave", String.format("Updating %s with %s", name, value));
    	
    	boolean exists = true;
    	try
    	{
    		args.put(KEY_VALUE, value);
    		exists = db.update(DB_TABLE_RECENT, args, String.format("%s='%s'", KEY_NAME, name), null) > 0;
    	}
    	catch(Exception e)
    	{
    		exists = false;

    		Log.e("Dave", String.format("Entry does not exist", name, value));
    		Toast t = Toast.makeText(mContext, "Entry doesn't exist", Toast.LENGTH_LONG);
			t.show();
    	}
    	
    	if(!exists)
    	{
    		Log.e("Dave", String.format("Inserting new entry", name, value));
    		//Item doesn't exist in DB yet
    		args.put(KEY_NAME, name);
    		args.put(KEY_VALUE, value);
    		
    		if(db.insert(DB_TABLE_RECENT, null, args) < 0)
    		{
    			//Failed to add value to DB
    			Log.e("Dave", String.format("Failed to insert new entry", name, value));
    			Toast t = Toast.makeText(mContext, "Failed to add recent item", Toast.LENGTH_LONG);
				t.show();
    		}
    	}
    }
    
    
    //---------- ERROR TRANSACTIONS ----------

    
    /*
    //Insert an error log entry into the database
    public long AddErrorLogEntry(String date, String entry) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_ENTRY, entry);
        
        return db.insert(DB_TABLE_ERRORS, null, initialValues);
    }
    */
    
    /*
    //Retrieves all the log entries
    public Cursor GetAllErrors()
    {
        return db.query(DB_TABLE_ERRORS, ErrorLogColumnNames, null, null, null, null, null);
    }
    */
    
    public static long IDFromDate(String date)
    {
    	if(date == null)
    		return -1;
    	
    	String[] parts = date.split("_");
    	
    	if(parts.length != 2)
    	{
    		return -1;
    	}
    	
    	long result = (long)Integer.parseInt(parts[0]) * 1000000;
    	result += Integer.parseInt(parts[1]);
    	
    	return result;
    }
    
    public enum DBTable
    {
		Fillups,
    	Recent,
    	Errors
    }

}
