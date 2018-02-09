package com.viggi.lib_logger;

/**
 * Created by Dave on 3/31/2015.
 */
public class LoggerConfigEntry
{
    public int ID;
    public boolean IsToggle;
    public int Order;
    public String Label;
    public boolean IncludeValue;
    public boolean Safe;

    public LoggerConfigEntry(boolean isToggle, int order, String label, boolean includeValue, boolean safe)
    {
        IsToggle = isToggle;
        Order = order;
        Label = label;
        IncludeValue = includeValue;
        Safe = safe;
    }

    public LoggerConfigEntry(int id, boolean type, int order, String label, boolean includeValue, boolean safe)
    {
        this(type, order, label, includeValue, safe);
        ID = id;
    }

    public boolean equalTo(LoggerConfigEntry other)
    {
        if(IsToggle != other.IsToggle)
            return false;

        if(Order != other.Order)
            return false;

        if(Label != other.Label)
            return false;

        if(IncludeValue != other.IncludeValue)
            return false;

        if(Safe != other.Safe)
            return false;

        return true;
    }
}
