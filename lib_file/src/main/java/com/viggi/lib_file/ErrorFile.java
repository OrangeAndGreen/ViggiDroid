package com.viggi.lib_file;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.viggi.lib_datestring.DateStrings;

import java.io.FileWriter;
import java.util.Calendar;


/**
 * Created by Dave on 2/22/2015.
 */
public class ErrorFile
{
    public static String ErrorFile = "ErrorLog.txt";

    public static String Write(String who, String error, Context context)
    {
        Calendar date = Calendar.getInstance();
        String entry = String.format("%s - %s: %s\n", DateStrings.GetDateTimeString(date), who, error);
        Log.e("ErrorFile", entry);
        try
        {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/" + ErrorFile, true);
            fw.write(entry);
            fw.close();
        }
        catch (Exception e)
        {
            Log.e("ErrorFile", "Failed to write log entry");
        }

        if(context != null)
        {
            Toast t = Toast.makeText(context, entry, Toast.LENGTH_LONG);
            t.show();
        }

        return entry;
    }

    public static String WriteException(Exception e, Context context)
    {
        String stackTrace = "";
        for (StackTraceElement ste : e.getStackTrace())
            stackTrace += ste.toString() + "\n";

        Calendar date = Calendar.getInstance();
        String entry = String.format("%s:\n%s\n", DateStrings.GetDateTimeString(date), e.getCause());
        if(e.getLocalizedMessage() != null)
            entry += e.getLocalizedMessage() + "\n";
        entry += stackTrace + "\n";
        Log.e("ErrorFile", entry);
        try
        {
            FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/" + ErrorFile, true);
            fw.write(entry);
            fw.close();
        }
        catch (Exception e1)
        {
            Log.e("ErrorFile", "Failed to write log entry");
        }

        if(context != null)
        {
            Toast t = Toast.makeText(context, e.getClass().getSimpleName()+"\n"+e.getMessage()+"\n\n"+stackTrace, Toast.LENGTH_LONG);
            t.show();
        }

        return entry;
    }
}
