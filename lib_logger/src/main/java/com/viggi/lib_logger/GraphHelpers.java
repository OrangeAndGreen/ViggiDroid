package com.viggi.lib_logger;

import android.graphics.Color;
import android.util.Log;

import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogEntry;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_graph.FloatRectangle;
import com.viggi.lib_graph.GraphPlot;
import com.viggi.lib_graph.GraphView;
import com.viggi.lib_math.ArrayMath;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Dave on 9/7/2015.
 * Methods for drawing the graphs or text info in LogViewer
 */
public class GraphHelpers
{
    private static int GetDesiredLengthInDays(String timeRange)
    {
        //{ "All-time", "1 Year", "6 Months", "3 Months", "1 Month", "2 Weeks", "1 Week" }
        int desiredLength;

        switch(timeRange)
        {
            case "All-time":
                desiredLength = -1;
                break;
            case "1 Year":
                desiredLength = 365;
                break;
            case "6 Months":
                desiredLength = 180;
                break;
            case "3 Months":
                desiredLength = 90;
                break;
            case "1 Month":
                desiredLength = 30;
                break;
            case "2 Weeks":
                desiredLength = 14;
                break;
            case "1 Week":
                desiredLength = 7;
                break;
            default:
                desiredLength = 7;
                break;
        }

        return desiredLength;
    }

    public static void DrawDailyCountsGraph(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the data specified by "category"
        float[] data = LogFile.ExtractDailyTotals(log.GetLogEntries(), settings.dataType, startDate, config, settings.filter, settings.dayFilters);
        boolean isToggle = config.Toggles.indexOf(settings.dataType) >= 0;

        //If this is a toggle, possibly convert from hours to minutes
        String units = "";
        if(isToggle)
        {
            units = " hours";
            if(ArrayMath.GetMax(data) < 3)
            {
                units = " minutes";
                for(int i=0; i<data.length; i++)
                    data[i] *= 60;
            }
        }

        //Calculate the average curves
        float[] ave = ArrayMath.GetRunningAverageCurve(data, 30);
        float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
        float average = ArrayMath.GetAverage(data);

        try
        {
            if (settings.numDays > 0)
            {
                if (settings.numDays > data.length)
                    settings.numDays = data.length;

                float[] tempData = new float[settings.numDays];
                float[] tempAve = new float[settings.numDays];
                float[] tempAllAve = new float[settings.numDays];
                for (int i = 0; i < settings.numDays; i++)
                {
                    int grabIndex = data.length - settings.numDays + i;
                    tempData[i] = data[grabIndex];
                    tempAve[i] = ave[grabIndex];
                    tempAllAve[i] = allAve[grabIndex];
                }
                data = tempData;
                ave = tempAve;
                allAve = tempAllAve;
                startDate.setTimeInMillis(Calendar.getInstance().getTimeInMillis() - ((long) (settings.numDays - 1) * 24 * 3600 * 1000));
            }

            graph.EasyGraph(data);

            //Setup the all-data plot
            graph.Plots.get(0).SetColor(Color.WHITE);
            graph.Plots.get(0).DrawPoints = true;
            graph.Plots.get(0).PointColor = Color.YELLOW;

            //Setup the all-time average plot
            graph.Plots.add(new GraphPlot(allAve, 1));
            graph.Plots.get(1).SetColor(Color.RED);
            graph.Plots.get(1).DrawPoints = false;

            //Setup the running average plot
            graph.Plots.add(new GraphPlot(ave, 1));
            graph.Plots.get(2).SetColor(Color.GREEN);
            graph.Plots.get(2).DrawPoints = false;

            graph.RightAxis.GenerateLabels(graph.Plots);
            graph.LeftAxis.GenerateLabels(graph.Plots);

            //Setup the title
            graph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
                    average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));

            //Add the weekend shading and start-of-month indicators
            graph.AddDateInfo(startDate, true);

            //Turn off labels for the bottom axis since they are drawn with the date info
            //mGraph.BottomAxis.DrawLabels = false;
        }
        catch(Exception e)
        {
            ErrorFile.WriteException(e, null);
        }
    }

    public static void DrawDailyTimingGraph(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        //Extract the data specified by "category"
        List<LogEntry> data = log.ExtractLog(settings.dataType, config, settings.filter, settings.dayFilters);
        boolean isToggle = config.Toggles.indexOf(settings.dataType) >= 0;

        Calendar firstDate = Calendar.getInstance();
        if(settings.numDays > 0)
        {
            if(settings.numDays > data.size())
                settings.numDays = data.size();

            Calendar filterDate = Calendar.getInstance();
            filterDate.add(Calendar.HOUR, (settings.numDays - 1) * -24);
            filterDate.set(Calendar.HOUR, 0);
            filterDate.set(Calendar.MINUTE, 0);
            filterDate.set(Calendar.SECOND, 0);
            firstDate = filterDate;

            List<LogEntry> tempData = new ArrayList<>();

            for(int i=0; i<data.size(); i++)
                if(data.get(i).GetDate().after(filterDate))
                    tempData.add(data.get(i));

            if(isToggle && tempData.size() > 0 && tempData.get(0).ToggleState.equals("off"))
            {
                tempData.remove(0);
            }

            data = tempData;
        }
        else
        {
            if(data.size() > 0)
                firstDate = data.get(0).GetDate();
        }

        //List<Calendar> dates = new ArrayList<>();
        List<Float> xValues = new ArrayList<>();
        List<Float> yValues = new ArrayList<>();
        if(data.size() > 0)
        {
            if(isToggle)
            {
                LogEntry lastEntry = data.get(data.size() - 1);
                if(lastEntry.ToggleState.equals("on"))
                {
                    LogEntry newLast = new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()), lastEntry.GetType(), "off", null);
                    data.add(newLast);
                }
            }

            LogEntry lastEntry = null;
            int lastX = 0;

            for(int i=0; i<data.size(); i++)
            {
                LogEntry curEntry = data.get(i);
                Calendar date = curEntry.GetDate();

                float hour = date.get(Calendar.HOUR_OF_DAY);
                float minute = date.get(Calendar.MINUTE);
                float second = date.get(Calendar.SECOND);

                if(isToggle)
                {
                    int curX = DateStrings.GetActiveDiffInDays(firstDate, date, 0);

                    if(lastEntry != null)
                    {
                        //Code for "off" entries
                        for(int j=0; j<curX - lastX; j++)
                        {
                            xValues.add((float)(lastX + j));
                            yValues.add((float)24);

                            xValues.add((float)(lastX + j + 1));
                            yValues.add((float)0);
                        }

                        lastEntry = null;
                    }
                    else
                    {
                        //Code for "on" entries
                        lastEntry = curEntry;
                    }

                    lastX = curX;
                }

                xValues.add((float)DateStrings.GetActiveDiffInDays(firstDate, date, 0));
                yValues.add(hour + (minute / 60) + (second / 3600));
                //dates.add(date);
            }
        }
        float[] x = new float[xValues.size()];
        float[] y = new float[yValues.size()];
        for(int i = 0; i < xValues.size(); i++)
        {
            x[i] = xValues.get(i);
            y[i] = yValues.get(i);
        }

        graph.EasyScatterPlot(x, y, isToggle, new FloatRectangle(0, 24, (float) DateStrings.GetActiveDiffInDays(firstDate, Calendar.getInstance(), 0), 0));
        graph.Plots.get(0).PointColor = Color.YELLOW;

        //Setup the title
        //mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
        //							average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));

        //Add the weekend shading and start-of-month indicators
        graph.AddDateInfo(firstDate, Calendar.getInstance(), true);

        //Turn off labels for the bottom axis since they are drawn with the date info
        //mGraph.BottomAxis.DrawLabels = false;
    }

    public static void DrawDistribution(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the data specified by "category"
        float[] data = LogFile.ExtractDailyTotals(log.GetLogEntries(), settings.dataType, startDate, config, settings.filter, settings.dayFilters);
        boolean isToggle = config.Toggles.indexOf(settings.dataType) >= 0;

        //If this is a toggle, possibly convert from hours to minutes
        int binMultiplier = 1;
        String units = "";
        int axisMultiplier = 1;
        int dataMultiplier = 1;
        if(isToggle)
        {
            units = " hours";
            if(ArrayMath.GetMax(data) < 2)
            {
                units = " minutes";
                binMultiplier = 60;
                axisMultiplier = 60 / binMultiplier;
                dataMultiplier = 60;
                for(int i=0; i<data.length; i++)
                    data[i] *= dataMultiplier;
            }
        }

        //Generate the distribution curve
        int numEntries = data.length;
        int numBins = (Math.round(ArrayMath.GetCeiling(data) / dataMultiplier) + 1) * binMultiplier + 1;
        float[] histogram = new float[numBins];
        for(float dataEntry : data)
        {
            int bin = Math.round(dataEntry / dataMultiplier * binMultiplier);
            if(bin < 0)
                bin = 0;
            if(bin >= numBins)
                bin = numBins - 1;
            histogram[bin] += 1.f / numEntries * 100;
        }

        int gaussLength = 101;
        float average = ArrayMath.GetAverage(data);
        float stddev = ArrayMath.GetStandardDeviation(data);
        float[] gaussian = ArrayMath.GetGaussianEstimate(data, 0, (histogram.length - 1) * axisMultiplier, gaussLength);
        int ceiling = (int)ArrayMath.GetCeiling(histogram);

        graph.EasyGraph(histogram);
        graph.Plots.get(0).SetColor(Color.WHITE);
        graph.Plots.get(0).DrawPoints = true;
        graph.Plots.get(0).PointColor = Color.YELLOW;

        graph.Plots.add(new GraphPlot(gaussian, (float) 1 / (gaussLength - 1)));
        graph.Plots.get(1).SetColor(Color.RED);
        graph.Plots.get(1).DrawPoints = false;

        graph.Title.Text = String.format("Average: %.02f%s/day\nStd. Dev.: %.02f%s/day", average, units, stddev, units);

        graph.LeftAxis.GenerateLabels(0, ceiling, 1, false);
        graph.RightAxis.GenerateLabels(0, ceiling, 1, false);

        graph.BottomAxis.GenerateLabels(0, numBins - 1, axisMultiplier, false);
        graph.BottomAxis.DrawLabels = true;
    }

    public static void DrawHistogramGraph(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        List<LogEntry> entries = log.ExtractLog(settings.dataType, config, settings.filter, settings.dayFilters);

        boolean isToggle = config.Toggles.indexOf(settings.dataType) >= 0;

        float[] hist = log.GetHistogram(entries, settings.timeScale);
        float yMax = ArrayMath.GetCeiling(hist);

        //Toast.makeText(this, "Hist: " + hist.length, Toast.LENGTH_SHORT).show();

        graph.EasyGraph(hist, new FloatRectangle(0, yMax, hist.length - 1, 0));
        graph.Plots.get(0).SetColor(Color.WHITE);
        graph.Plots.get(0).DrawPoints = false;

        graph.BottomAxis.DrawLabels = true;
        graph.Title.Text = settings.timeScale + " Histogram";

        //{"Hour of day", "Day of week", "Day of month", "Day of year","Week of year", "Month of year"};
        switch(settings.timeScale)
        {
            case "Hour of day":
                //Add a data point for every entry showing the exact time-of-day
                for(int i=0; i<entries.size(); i++)
                {
                    float hour = entries.get(i).GetDate().get(Calendar.HOUR_OF_DAY);
                    float minute = entries.get(i).GetDate().get(Calendar.MINUTE);
                    float second = entries.get(i).GetDate().get(Calendar.SECOND);

                    float x = hour + minute / 60 + second / 3600;

                    int color = Color.GREEN;
                    if(isToggle && entries.get(i).ToggleState.equals("off"))
                        color = Color.RED;

                    graph.AddDataPoint(x, yMax / 2, color);
                }

                graph.BottomAxis.GenerateLabels(0, hist.length, 1, false);
                break;
            case "Day of week":
            case "Day of month":
            case "Day of year":
            {
                //Extract the specified data
                Calendar startDate = Calendar.getInstance();
                float[] allData = LogFile.ExtractDailyTotals(log.GetLogEntries(), settings.dataType, startDate, config, settings.filter, settings.dayFilters);

                //Possibly scale data and determine graph units
                String units = "";
                if(isToggle)
                {
                    units = " (in hours)";
                    if(ArrayMath.GetMax(allData) < 3)
                    {
                        units = " (in minutes)";
                        for(int i=0; i<allData.length; i++)
                            allData[i] *= 60;
                    }
                }
                graph.Title.Text += units;

                //Calculate x values by adding 24 hours to the startDate in a loop (and extracting desired value for this timeScale)
                int[] x = new int[allData.length];
                for(int i=0; i<allData.length; i++)
                {
                    x[i] = LogFile.GetDataValueForHistogramTimeScale(settings.timeScale, startDate);
                    startDate.add(Calendar.HOUR, 24);
                }

                //Calculate y values
                for(int i=0; i<allData.length; i++)
                {
                    graph.AddDataPoint(x[i], allData[i], Color.GREEN);
                }

                graph.RightAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);
                graph.LeftAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);

                startDate = Calendar.getInstance();
                int day = startDate.get(Calendar.DAY_OF_WEEK) % 7;
                if(day > 0)
                    startDate.add(Calendar.HOUR, -24 * day);

                if(settings.timeScale.equals("Day of week"))
                    graph.AddDateInfo(startDate, false);

                String[] labels = {"S", "S", "M", "T", "W", "T", "F"};
                graph.BottomAxis.SetLabels(labels);
                break;
            }
            case "Week of year":
            {
                //Extract the specified data
                Calendar startDate = Calendar.getInstance();
                float[] allData = LogFile.ExtractDailyTotals(log.GetLogEntries(), settings.dataType, startDate, config, settings.filter, settings.dayFilters);

                Calendar dateCopy = (Calendar)startDate.clone();
                float[] weeklyTotals = new float[53];
                for(float dataPoint : allData)
                {
                    int weekNum = LogFile.GetDataValueForHistogramTimeScale(settings.timeScale, dateCopy);
                    weeklyTotals[weekNum] += dataPoint;
                    dateCopy.add(Calendar.HOUR, 24);
                }

                //Possibly scale data and determine graph units
                String units = "";
                if(isToggle)
                {
                    units = " (in hours)";
                    if(ArrayMath.GetMax(weeklyTotals) < 3)
                    {
                        units = " (in minutes)";
                        for(int i=0; i<weeklyTotals.length; i++)
                            weeklyTotals[i] *= 60;
                    }
                }
                graph.Title.Text += units;

                //Calculate x values by adding 24 hours to the startDate in a loop (and extracting desired value for this timeScale)
                int[] x = new int[allData.length];
                for(int i=0; i<allData.length; i++)
                {
                    x[i] = LogFile.GetDataValueForHistogramTimeScale(settings.timeScale, startDate);
                    startDate.add(Calendar.HOUR, 24);
                }

                //Calculate y values
                for(int i=0; i<allData.length; i++)
                {
                    graph.AddDataPoint(x[i], allData[i], Color.GREEN);
                }

                graph.RightAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);
                graph.LeftAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);

                startDate = Calendar.getInstance();
                int day = startDate.get(Calendar.DAY_OF_WEEK) % 7;
                if(day > 0)
                    startDate.add(Calendar.HOUR, -24 * day);
                //graph.AddDateInfo(startDate, false);

                graph.BottomAxis.GenerateLabels(1, 53, 1, false);
                break;
            }
        }
    }

    public static void DrawIntervalsGraph(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the specified data
        int multiplier = 1;
        int numEntries;
        List<LogEntry> entries = log.ExtractLog(settings.dataType, config, settings.filter, settings.dayFilters);
        boolean isToggle = config.Toggles.indexOf(settings.dataType) >= 0;
        if(isToggle)
        {
            if(entries.size() % 2 != 0)
                entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
                        entries.get(entries.size() - 1).GetType(), "off", null));
            multiplier = 2;
            numEntries = entries.size() / 2;
        }
        else
        {
            entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
                    entries.get(entries.size() - 1).GetType(), null, null));
            numEntries = entries.size();
        }

        if(settings.numDays > 0)
        {
            if(settings.numDays > entries.size())
                settings.numDays = entries.size();

            Calendar filterDate = Calendar.getInstance();
            filterDate.add(Calendar.HOUR, (settings.numDays - 1) * -24);
            filterDate.set(Calendar.HOUR, 0);
            filterDate.set(Calendar.MINUTE, 0);
            filterDate.set(Calendar.SECOND, 0);

            List<LogEntry> tempData = new ArrayList<>();

            for(int i=0; i<entries.size(); i++)
                if(entries.get(i).GetDate().after(filterDate))
                    tempData.add(entries.get(i));

            if(isToggle && tempData.get(0).ToggleState.equals("off"))
                tempData.remove(0);

            entries = tempData;

            if(isToggle)
                numEntries = entries.size() / 2;
            else
                numEntries = entries.size();
        }

        //Calculate the intervals
        float[] intervals = new float[numEntries];
        Calendar lastDate = null;
        for(int i=0; i<numEntries*multiplier; i++)
        {
            Calendar curDate = entries.get(i).GetDate();
            if(i%multiplier != 0 || multiplier == 1 && lastDate != null)
            {
                try
                {
                    intervals[i / multiplier] = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float) 3600000;
                }
                catch(NullPointerException e)
                {
                    Log.e("GraphHelpers", "lastDate is null");
                }
            }

            lastDate = curDate;
        }

        String units = "hours";
        if(ArrayMath.GetMax(intervals) >= 96)
        {
            units = "days";
            for(int i=0; i<intervals.length; i++)
                intervals[i] /= 24;
        }
        else if(isToggle & ArrayMath.GetMax(intervals) < 3)
        {
            units = "minutes";
            for(int i=0; i<intervals.length; i++)
                intervals[i] *= 60;
        }

        float[] ave = ArrayMath.GetRunningAverageCurve(intervals, 30);
        float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(intervals);
        float average = ArrayMath.GetAverage(intervals);

        graph.EasyGraph(intervals);
        graph.Plots.get(0).SetColor(Color.WHITE);
        graph.Plots.get(0).DrawPoints = false;

        graph.Plots.add(new GraphPlot(allAve, 1));
        graph.Plots.get(1).SetColor(Color.RED);
        graph.Plots.get(1).DrawPoints = false;

        graph.Plots.add(new GraphPlot(ave, 1));
        graph.Plots.get(2).SetColor(Color.GREEN);
        graph.Plots.get(2).DrawPoints = false;

        graph.Title.Text = String.format("All-time: %.02f %s, currently: %.02f %s\nFirst entry: %s",
                average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
        //mGraph.AddDateInfo(startDate);

        graph.BottomAxis.DrawLabels = false;
    }

    public static void DrawValuesGraph(GraphView graph, LogFile log, LoggerConfig config, GraphSettings settings)
    {
        //Extract the data specified by "category"
        List<LogEntry> data = log.ExtractLog(settings.dataType, config, settings.filter, settings.dayFilters);
        Calendar[] dates = new Calendar[data.size()];
        float[] values = new float[data.size()];
        for(int i=0; i<data.size(); i++)
        {
            dates[i] = data.get(i).GetDate();
            try
            {
                values[i] = Float.parseFloat(data.get(i).GetComment());
            }
            catch(Exception e)
            {
                Log.e("GraphHelpers", "Error parsing value from log entry");
            }
        }
        float[] allAves = ArrayMath.GetAllTimeRunningAverageCurve(values);
        float[] runningAves = ArrayMath.GetRunningAverageCurve(values, 30);

        Calendar firstDate = Calendar.getInstance();
        if(settings.numDays > 0)
        {
            if(settings.numDays > data.size())
                settings.numDays = data.size();

            Calendar filterDate = Calendar.getInstance();
            filterDate.add(Calendar.HOUR, (settings.numDays - 1) * -24);
            filterDate.set(Calendar.HOUR, 0);
            filterDate.set(Calendar.MINUTE, 0);
            filterDate.set(Calendar.SECOND, 0);
            firstDate = filterDate;

            List<Calendar> dateSubset = new ArrayList<>();
            List<Float> subset = new ArrayList<>();
            List<Float> allAveSubset = new ArrayList<>();
            List<Float> runningAveSubset = new ArrayList<>();
            for(int i=0; i<data.size(); i++)
                if(data.get(i).GetDate().after(filterDate))
                {
                    dateSubset.add(dates[i]);
                    subset.add(values[i]);
                    allAveSubset.add(allAves[i]);
                    runningAveSubset.add(runningAves[i]);
                }

            int subsetSize = subset.size();
            dates = new Calendar[subsetSize];
            values = new float[subsetSize];
            allAves = new float[subsetSize];
            runningAves = new float[subsetSize];

            for(int i=0; i<subsetSize; i++)
            {
                dates[i] = dateSubset.get(i);
                values[i] = subset.get(i);
                allAves[i] = allAveSubset.get(i);
                runningAves[i] = runningAveSubset.get(i);
            }
        }
        else
        {
            if(dates.length > 0)
                firstDate = dates[0];
        }

        float[] x = new float[dates.length];
        for(int i=0; i<dates.length; i++)
        {
            x[i] = (float)DateStrings.GetActiveDiffInDays(firstDate, dates[i], 0);
        }

        float min = ArrayMath.GetMin(values);
        float max = ArrayMath.GetMax(values);
        graph.EasyLineGraph(x, values, new FloatRectangle(0, max, (float)DateStrings.GetActiveDiffInDays(firstDate, Calendar.getInstance(), 0), min));
        graph.Plots.get(0).PointColor = Color.YELLOW;
        graph.Plots.get(0).SetColor(Color.WHITE);

        //Setup the all-time average plot
        graph.Plots.add(new GraphPlot(x, allAves));
        graph.Plots.get(1).SetColor(Color.RED);
        graph.Plots.get(1).DrawPoints = false;

        //Setup the running average plot
        graph.Plots.add(new GraphPlot(x, runningAves));
        graph.Plots.get(2).SetColor(Color.GREEN);
        graph.Plots.get(2).DrawPoints = false;

        //Setup the title
        graph.Title.Text = String.format("All-time: %.02f, currently: %.02f",
                allAves[allAves.length - 1], runningAves[runningAves.length - 1]);

        //Add the weekend shading and start-of-month indicators
        graph.AddDateInfo(firstDate, Calendar.getInstance(), true);

        //Turn off labels for the bottom axis since they are drawn with the date info
        //mGraph.BottomAxis.DrawLabels = false;
    }

    public static String DrawStats(LogFile log, LoggerConfig config, GraphSettings settings)
    {
        return log.GetStats(settings.dataType, config, settings.filter, settings.dayFilters);
    }

    public static String DrawComments(LogFile log, LoggerConfig config, GraphSettings settings)
    {
        return log.GetCommentSummary(settings.dataType, config, settings.filter, settings.dayFilters);
    }

    public static String DrawRecentHistory(LogFile log, LoggerConfig config, GraphSettings settings)
    {
        List<LogEntry> entries = log.ExtractLog(settings.dataType, config, settings.filter, settings.dayFilters);

        //Optionally filter by history length (in days)
        if(settings.numDays > 0)
        {
            if(settings.numDays > entries.size())
                settings.numDays = entries.size();

            Calendar filterDate = Calendar.getInstance();
            filterDate.add(Calendar.HOUR, (settings.numDays - 1) * -24);
            filterDate.set(Calendar.HOUR, 0);
            filterDate.set(Calendar.MINUTE, 0);
            filterDate.set(Calendar.SECOND, 0);

            List<LogEntry> tempData = new ArrayList<>();

            for(int i=0; i<entries.size(); i++)
                if(entries.get(i).GetDate().after(filterDate))
                    tempData.add(entries.get(i));

            entries = tempData;
        }

        int numEntries = entries.size();
        int historyLength = 200; //only show the first 200 entries
        int startIndex = 0;
        if(numEntries >= historyLength)
            startIndex = numEntries - historyLength;


        //Build the list string
        String statsText = "";
        LogEntry lastLogEntry = null;
        for(int i=startIndex; i<numEntries; i++)
        {
            //Add the interval to the end of the previous line (after 1st entry), then start a new line and write the main entry
            LogEntry logEntry = entries.get(i);
            String lineEnd = "\n";

            if(lastLogEntry != null)
                lineEnd = String.format(" (%s)\n", DateStrings.GetElapsedTimeString(lastLogEntry.GetDate(), logEntry.GetDate(), 2));

            statsText += lineEnd + logEntry.GetEntryString();
            lastLogEntry = logEntry;
        }

        //Write the last log entry
        if(lastLogEntry != null)
            statsText += String.format(" (%s)\n", DateStrings.GetElapsedTimeString(lastLogEntry.GetDate(), Calendar.getInstance(), 2));

        return statsText;
    }

    /*
	public void DrawIntervalsHistogram(String category)
	{
		mGraph.setVisibility(View.VISIBLE);
		mTextScroller.setVisibility(View.GONE);

		//Extract the specified data
		int multiplier = 1;
		int numEntries = 0;
		List<LogEntry> entries = null;
		int catIndex = ConfigFile.Buttons.indexOf(category);
		if(catIndex < 0)
		{
			catIndex = ConfigFile.Toggles.indexOf(category);
			entries = DaveLogger.Log.ExtractToggleLog(catIndex);
			if(entries.size() % 2 != 0)
				entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
						entries.get(entries.size() - 1).GetType(), "off", null));
			multiplier = 2;
			numEntries = entries.size() / 2;
		}
		else
		{
			entries = DaveLogger.Log.ExtractEventLog(catIndex);
			entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
					entries.get(entries.size() - 1).GetType(), null, null));
			numEntries = entries.size();
		}

		float[] intervals = new float[numEntries];
		Calendar lastDate = null;
		for(int i=0; i<numEntries*multiplier; i++)
		{
			Calendar curDate = entries.get(i).GetDate();
			if(i%multiplier != 0 || multiplier == 1 && i > 0)
				intervals[i/multiplier] = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float) 3600000;
			lastDate = curDate;
		}

		if(ArrayMath.GetMax(intervals) >= 96)
		{
			for(int i=0; i<intervals.length; i++)
				intervals[i] /= 24;
		}

		int numIntervals = intervals.length;
		int numBins = Math.round(ArrayMath.GetMax(intervals));
		float[] histogram = new float[numBins];
		int[] x = new int[numIntervals];
		for(int i=0; i<intervals.length; i++)
		{
			int bin = Math.round(intervals[i]);
			if(bin < 0)
				bin = 0;
			if(bin >= numBins)
				bin = numBins - 1;
			x[i] = bin;
			histogram[bin] += 1 / numIntervals;
		}

		mGraph.EasyGraph(histogram);
		mGraph.Plots.get(0).SetColor(Color.WHITE);
		mGraph.Plots.get(0).DrawPoints = false;

		for(int i=0; i<intervals.length; i++)
		{
			mGraph.AddDataPoint(x[i], intervals[i], Color.GREEN);
		}

		mGraph.Title.Text = "";

		mGraph.BottomAxis.GenerateLabels(histogram.length);
		mGraph.BottomAxis.DrawLabels = true;

		mGraph.invalidate();
	}
	*/


}
