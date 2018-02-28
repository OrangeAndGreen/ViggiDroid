package com.viggi.lib_logger;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.Log;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.example.dave.lib_logger.R;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogEntry;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LogItem;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_file.PhoneLogEntry;
import com.viggi.lib_file.PhoneLogFile;
import com.viggi.lib_graph.APZoomListener;
import com.viggi.lib_graph.FloatRectangle;
import com.viggi.lib_graph.GraphLabel;
import com.viggi.lib_graph.GraphPlot;
import com.viggi.lib_graph.GraphView;
import com.viggi.lib_math.ArrayMath;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Dave on 9/7/2015.
 * Methods for drawing the graphs or text info in LogViewer
 */
public class GraphHelpers
{
    public static void DrawDailyCountsGraph(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LogItem item, int midnightHour, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the data specified by "category"
        float[] data;
        if(item.Name.equals("Phone calls"))
        {
            data = PhoneLogFile.ExtractDailyTotals(phoneLog.GetLogEntries(), startDate, midnightHour, settings.filter, settings.dayFilters, false);
        }
        else
        {
            data = LogFile.ExtractDailyTotals(log.GetLogEntries(), item, startDate, midnightHour, settings.filter, settings.dayFilters);
        }

        //If this is a toggle, possibly convert from hours to minutes
        String units = "";
        if(item.IsToggle)
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
            //Extract the desired history length
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

            String title = String.format(Locale.getDefault(), "All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
                    average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));

            if(xyPlot != null)
            {
                //Prepare graph
                PrepareXYPlot(xyPlot, title, "Date", settings.dataType);

                //Build array of entry date strings for the x-axis labels
                final String[] domainLabels = new String[data.length];
                Calendar curDate = (Calendar) startDate.clone();
                for(int i=0; i<data.length; i++)
                {
                    int monthDay = curDate.get(Calendar.DAY_OF_MONTH);
                    int month = curDate.get(Calendar.MONTH) + 1;
                    int year = curDate.get(Calendar.YEAR) % 100;

                    domainLabels[i] = String.format(Locale.getDefault(), "%02d%02d%02d", year, month, monthDay);

                    curDate.add(Calendar.HOUR, 24);
                }

                //Add the 3 plots
                AddPlot(context, xyPlot, data, "Data", false, R.xml.line_point_formatter_with_labels);
                AddPlot(context, xyPlot, allAve, "Ave", false, R.xml.line_point_formatter_ave_all);
                AddPlot(context, xyPlot, ave, "Running Ave", false, R.xml.line_point_formatter_with_labels_2);

                //Configure X and Y axis labels
                ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
                ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.BOTTOM, domainLabels);

                new APZoomListener(xyPlot, data.length, ArrayMath.GetMax(data));
            }
            else
            {
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
                graph.Title.Text = title;

                //Add the weekend shading and start-of-month indicators
                graph.AddDateInfo(startDate, true);

                //Turn off labels for the bottom axis since they are drawn with the date info
                //mGraph.BottomAxis.DrawLabels = false;
            }
        }
        catch(Exception e)
        {
            ErrorFile.WriteException(e, null);
        }
    }

    public static void DrawDailyTimingGraph(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LoggerConfig config, GraphSettings settings)
    {
        //Extract the all log entries for the specified "category"
        LogItem item = config.GetEntryByName(settings.dataType);
        List<LogEntry> data = log.ExtractLog(item, settings.filter, settings.dayFilters);

        //Filter to only entries after the cutoff date (using numDays)
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

            //Make sure the first entry isn't an "off"
            if(item.IsToggle && tempData.size() > 0 && tempData.get(0).ToggleState.equals("off"))
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


        List<Calendar> dates = new ArrayList<>();
        List<Float> xValues = new ArrayList<>();
        List<Float> yValues = new ArrayList<>();
        if(data.size() > 0)
        {
            //If a toggle and it's currently on, add an artificial "off" date using the current time
            if(item.IsToggle)
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

            //Add x/y/date entries for each point (plus extras for day rollovers)
            for(int i=0; i<data.size(); i++)
            {
                LogEntry curEntry = data.get(i);
                Calendar date = curEntry.GetDate();

                float hour = date.get(Calendar.HOUR_OF_DAY);
                float minute = date.get(Calendar.MINUTE);
                float second = date.get(Calendar.SECOND);

                //
                if(item.IsToggle)
                {
                    int curX = DateStrings.GetActiveDiffInDays(firstDate, date, 0);

                    //Setting/resetting lastEntry after each pair to handle toggle on/off
                    if(lastEntry != null)
                    {
                        //Toggle turned off

                        //Add "off" entry at 24-hours and 0 entry at 0-hours for each day rollover between last and current entry
                        Calendar rolloverDate = (Calendar)date.clone();
                        for(int j=0; j<curX - lastX; j++)
                        {
                            xValues.add((float)(lastX + j));
                            yValues.add((float)24);
                            dates.add(rolloverDate);

                            rolloverDate.add(Calendar.HOUR, 24);

                            xValues.add((float)(lastX + j + 1));
                            yValues.add((float)0);
                            dates.add(date);
                        }

                        lastEntry = null;
                    }
                    else
                    {
                        //Toggle turned on
                        lastEntry = curEntry;
                    }

                    lastX = curX;
                }

                //Finally, add the point for the entry itself
                xValues.add((float)DateStrings.GetActiveDiffInDays(firstDate, date, 0));
                yValues.add(hour + (minute / 60) + (second / 3600));
                dates.add(date);
            }
        }

        //Convert the lists to arrays
        Calendar[] allDates = new Calendar[dates.size()];
        float[] x = new float[xValues.size()];
        float[] y = new float[yValues.size()];
        for(int i = 0; i < xValues.size(); i++)
        {
            allDates[i] = dates.get(i);
            x[i] = xValues.get(i);
            y[i] = yValues.get(i);
        }

        if(xyPlot != null)
        {
            //Prepare graph
            PrepareXYPlot(xyPlot, settings.dataType + " Daily Timing", "Date", "Hour of Day");

            //Build array of entry date string for the x-axis labels
            int numDays = DateStrings.GetActiveDiffInDays(dates.get(0), dates.get(dates.size() - 1), config.MidnightHour);
            //final String[] domainLabels = new String[numDays];
//            Calendar curDate = (Calendar) firstDate.clone();
//            for(int i=0; i<numDays; i++)
//            {
//                int monthDay = curDate.get(Calendar.DAY_OF_MONTH);
//                int month = curDate.get(Calendar.MONTH) + 1;
//                int year = curDate.get(Calendar.YEAR) % 100;
//
//                domainLabels[i] = String.format(Locale.getDefault(), "%02d%02d%02d", year, month, monthDay);
//
//                curDate.add(Calendar.HOUR, 24);
//            }

            if(item.IsToggle)
            {
                //Handling toggles:
                //Want to add a new plot for each toggle pair
                for(int i=0; i<x.length / 2; i++)
                {
                    float[] toggleX = new float[2];
                    toggleX[0] = x[i*2];
                    toggleX[1] = x[i*2+1];

                    float[] toggleY = new float[2];
                    toggleY[0] = y[i*2];
                    toggleY[1] = y[i*2+1];

                    LineAndPointFormatter formatter = AddXYPlot(context, xyPlot, toggleX, toggleY, "Data", false, R.xml.line_point_toggle_formatter);

                    if(i>0)
                    {
                        formatter.setLegendIconEnabled(false);
                    }
                }
            }
            else
            {
                //Add all the points as a single plot
                AddXYPlot(context, xyPlot, x, y, "Data", false, R.xml.point_formatter);
            }

            //Configure X and Y axis labels
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
            ConfigureScatterAxisLabels(xyPlot, x, allDates);

            new APZoomListener(xyPlot, numDays, 24);

            xyPlot.setRangeBoundaries(0, BoundaryMode.FIXED, 24, BoundaryMode.FIXED);
            xyPlot.setDomainBoundaries(0, BoundaryMode.FIXED, numDays, BoundaryMode.FIXED);
        }
        else
        {
            graph.EasyScatterPlot(x, y, item.IsToggle, new FloatRectangle(0, 24, (float) DateStrings.GetActiveDiffInDays(firstDate, Calendar.getInstance(), 0), 0));
            graph.Plots.get(0).PointColor = Color.YELLOW;

            //Setup the title
            //mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
            //							average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));

            //Add the weekend shading and start-of-month indicators
            graph.AddDateInfo(firstDate, Calendar.getInstance(), true);

            //Turn off labels for the bottom axis since they are drawn with the date info
            //mGraph.BottomAxis.DrawLabels = false;
        }
    }

    public static void DrawDistribution(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LogItem item, int midnightHour, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the data specified by "category"
        float[] data = LogFile.ExtractDailyTotals(log.GetLogEntries(), item, startDate, midnightHour, settings.filter, settings.dayFilters);

        //If this is a toggle, possibly convert from hours to minutes
        int binMultiplier = 1;
        String units = "";
        int axisMultiplier = 1;
        int dataMultiplier = 1;
        if(item.IsToggle)
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

        if(xyPlot != null)
        {
            //Prepare graph
            PrepareXYPlot(xyPlot, settings.dataType + " Distribution", "Daily Total", "Frequency");

            //Add the 3 plots
            AddPlot(context, xyPlot, histogram, "Data", false, R.xml.line_point_formatter_with_labels);
            //AddPlot(context, xyPlot, gaussian, "Data", false, R.xml.line_point_formatter_with_labels);

            //Configure X and Y axis labels
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.BOTTOM, null);

            new APZoomListener(xyPlot, histogram.length, ArrayMath.GetMax(histogram));
        }
        else
        {
            graph.EasyGraph(histogram);
            graph.Plots.get(0).SetColor(Color.WHITE);
            graph.Plots.get(0).DrawPoints = true;
            graph.Plots.get(0).PointColor = Color.YELLOW;

            graph.Plots.add(new GraphPlot(gaussian, (float) 1 / (gaussLength - 1)));
            graph.Plots.get(1).SetColor(Color.RED);
            graph.Plots.get(1).DrawPoints = false;

            graph.Title.Text = String.format(Locale.getDefault(), "Average: %.02f%s/day\nStd. Dev.: %.02f%s/day", average, units, stddev, units);

            graph.LeftAxis.GenerateLabels(0, ceiling, 1, false);
            graph.RightAxis.GenerateLabels(0, ceiling, 1, false);

            graph.BottomAxis.GenerateLabels(0, numBins - 1, axisMultiplier, false);
            graph.BottomAxis.DrawLabels = true;
        }
    }

    public enum TimeUnit
    {
        Unknown,
        Hour,
        Day,
        Week,
        Month,
        Year
    }

    public enum HistogramValueMode
    {
        Unknown,
        Percentage,
        Cumulative,
        Average
    }

    public static void DrawHistogramGraph(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LogItem item, int midnightHour, GraphSettings settings)
    {
        TimeUnit timeBinUnit = TimeUnit.Hour;
        TimeUnit timeModUnit = TimeUnit.Day;
        HistogramValueMode mode = HistogramValueMode.Average;



        List<LogEntry> entries = log.ExtractLog(item, settings.filter, settings.dayFilters);

        float[] hist = log.GetHistogram(entries, settings.timeScale, true);
        float yMax = ArrayMath.GetCeiling(hist);

        //Toast.makeText(this, "Hist: " + hist.length, Toast.LENGTH_SHORT).show();

        //{"Hour of day", "Day of week", "Day of month", "Day of year","Week of year", "Month of year"};
        String[] xLabels = new String[1];
        int numX = -1;
        String units = "";
        boolean addPointsCommonCode = false;
        List<Float> xScatterValues = new ArrayList<>();
        List<Float> yScatterValues = new ArrayList<>();
        List<Integer> scatterColors = new ArrayList<>();
        switch(settings.timeScale)
        {
            case "Hour of day":
                numX = 25;
                xLabels = new String[numX];
                //Special labels here because we want a 0 (auto-code uses i+1)
                for(int i=0; i<numX; i++)
                {
                    xLabels[i] = String.format(Locale.getDefault(), "%d", i);
                }
                numX = -1; //so the auto-code below isn't used

                for(int i=0; i<entries.size(); i++)
                {
                    float hour = entries.get(i).GetDate().get(Calendar.HOUR_OF_DAY);
                    float minute = entries.get(i).GetDate().get(Calendar.MINUTE);
                    float second = entries.get(i).GetDate().get(Calendar.SECOND);

                    float x = hour + minute / 60 + second / 3600;

                    int color = Color.GREEN;
                    if(item.IsToggle && entries.get(i).ToggleState.equals("off"))
                        color = Color.RED;

                    xScatterValues.add(x);
                    yScatterValues.add(yMax / 2);
                    scatterColors.add(color);
                }

                break;
            case "Day of week":
            default:
            {
                addPointsCommonCode = true;

                String[] labels = {"S", "S", "M", "T", "W", "T", "F"};
                xLabels = new String[labels.length];
                for(int i=0; i<labels.length; i++)
                {
                    xLabels[i] = labels[i];
                }

                break;
            }
            case "Day of month":
                addPointsCommonCode = true;
                numX = 31;
                break;
            case "Day of year":
            {
                addPointsCommonCode = true;
                numX = 366;
                break;
            }
            case "Week of year":
            {
                numX= 52;

                //Extract the specified data
                Calendar startDate = Calendar.getInstance();
                float[] allData = LogFile.ExtractDailyTotals(log.GetLogEntries(), item, startDate, midnightHour, settings.filter, settings.dayFilters);

                Calendar dateCopy = (Calendar)startDate.clone();
                float[] weeklyTotals = new float[53];
                for(float dataPoint : allData)
                {
                    int weekNum = LogFile.GetDataValueForHistogramTimeScale(settings.timeScale, dateCopy);
                    weeklyTotals[weekNum] += dataPoint;
                    dateCopy.add(Calendar.HOUR, 24);
                }

                //Possibly scale data and determine graph units
                if(item.IsToggle)
                {
                    units = " (in hours)";
                    if(ArrayMath.GetMax(weeklyTotals) < 3)
                    {
                        units = " (in minutes)";
                        for(int i=0; i<weeklyTotals.length; i++)
                            weeklyTotals[i] *= 60;
                    }
                }

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
                    xScatterValues.add((float)x[i]);
                    yScatterValues.add(allData[i]);
                    scatterColors.add(Color.GREEN);
                }

                break;
            }
        }

        if(numX > 0)
        {
            xLabels = new String[numX];
            for(int i=0; i<numX; i++)
            {
                xLabels[i] = String.format(Locale.getDefault(), "%d", i+1);
            }
        }

        if(addPointsCommonCode)
        {
            //Extract the specified data
            Calendar startDate = Calendar.getInstance();
            float[] allData = LogFile.ExtractDailyTotals(log.GetLogEntries(), item, startDate, midnightHour, settings.filter, settings.dayFilters);

            //Possibly scale data and determine graph units
            units = "";
            if(item.IsToggle)
            {
                units = " (in hours)";
                if(ArrayMath.GetMax(allData) < 3)
                {
                    units = " (in minutes)";
                    for(int i=0; i<allData.length; i++)
                        allData[i] *= 60;
                }
            }


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
                xScatterValues.add((float)x[i]);
                yScatterValues.add(allData[i]);
                scatterColors.add(Color.GREEN);
            }
        }

        if(xyPlot != null)
        {
            //Prepare graph
            PrepareXYPlot(xyPlot, settings.dataType + " Histogram", settings.timeScale, "Daily Total"+units);

            //Add the plot
            AddPlot(context, xyPlot, hist, "Histogram", false, R.xml.line_point_formatter_with_labels);

            //Separate the scatter points by color
            HashMap<Integer,List<Float>> xListHash = new HashMap<>();
            HashMap<Integer,List<Float>> yListHash = new HashMap<>();
            for(int i=0; i<xScatterValues.size(); i++)
            {
                int key = scatterColors.get(i);

                if(!xListHash.containsKey(key))
                {
                    xListHash.put(key, new ArrayList<Float>());
                    yListHash.put(key, new ArrayList<Float>());
                }

                xListHash.get(key).add(xScatterValues.get(i));
                yListHash.get(key).add(yScatterValues.get(i));
            }

            //Now convert the x and y lists to arrays
            HashMap<Integer, float[]> xArrayHash = new HashMap<>();
            HashMap<Integer, float[]> yArrayHash = new HashMap<>();
            for(int key : xListHash.keySet())
            {
                List<Float> xList = xListHash.get(key);
                //Only include if this color got some points
                if(xList.size() > 0)
                {
                    List<Float> yList = yListHash.get(key);
                    float[] xArray = new float[xList.size()];
                    float[] yArray = new float[xList.size()];
                    for(int i=0; i<xList.size(); i++)
                    {
                        xArray[i] = xList.get(i);
                        yArray[i] = yList.get(i);
                    }

                    xArrayHash.put(key, xArray);
                    yArrayHash.put(key, yArray);
                }
            }

            //Now add each plot
            for(int key : xArrayHash.keySet())
            {
                //Determine the formatter and plot title based on the color (which is the key)
                int formatter;
                String title;
                switch(key)
                {
                    case Color.GREEN:
                        formatter = R.xml.point_formatter_green;
                        title = "On";
                        break;
                    case Color.RED:
                        formatter = R.xml.point_formatter_red;
                        title = "Off";
                        break;
                    default:
                        formatter = R.xml.point_formatter;
                        title = "Point";
                        break;
                }

                AddXYPlot(context, xyPlot, xArrayHash.get(key), yArrayHash.get(key), title, false, formatter);
            }

            //Configure X and Y axis labels
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.BOTTOM, xLabels);

            new APZoomListener(xyPlot, hist.length - 1, ArrayMath.GetMax(hist));
        }
        else
        {
            graph.EasyGraph(hist, new FloatRectangle(0, yMax, hist.length - 1, 0));
            graph.Plots.get(0).SetColor(Color.WHITE);
            graph.Plots.get(0).DrawPoints = false;

            graph.BottomAxis.DrawLabels = true;
            graph.Title.Text = settings.timeScale + " Histogram";
            graph.BottomAxis.SetLabels(xLabels);

            for(int i=0; i<xScatterValues.size(); i++)
            {
                graph.AddDataPoint(xScatterValues.get(i), yScatterValues.get(i), scatterColors.get(i));
            }

            graph.RightAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);
            graph.LeftAxis.GenerateLabels(0, Math.round(graph.GetDataRange().GetHeight()), 1, false);

            //Weekend shading
            if(settings.timeScale.equals("Day of week"))
            {
                Calendar startDate = Calendar.getInstance();
                LogFile.ExtractDailyTotals(log.GetLogEntries(), item, startDate, midnightHour, settings.filter, settings.dayFilters);

                if(settings.timeScale.equals("Day of week"))
                    graph.AddDateInfo(startDate, false);
            }
        }
    }

    public static void DrawIntervalsGraph(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LoggerConfig config, GraphSettings settings)
    {
        Calendar startDate = Calendar.getInstance();

        //Extract the specified data
        int multiplier = 1;
        int numEntries;
        LogItem item = config.GetEntryByName(settings.dataType);
        List<LogEntry> entries = log.ExtractLog(item, settings.filter, settings.dayFilters);
        if(item.IsToggle)
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

            if(item.IsToggle && tempData.get(0).ToggleState.equals("off"))
                tempData.remove(0);

            entries = tempData;

            if(item.IsToggle)
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
        else if(item.IsToggle & ArrayMath.GetMax(intervals) < 3)
        {
            units = "minutes";
            for(int i=0; i<intervals.length; i++)
                intervals[i] *= 60;
        }

        float[] ave = ArrayMath.GetRunningAverageCurve(intervals, 30);
        float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(intervals);
        float average = ArrayMath.GetAverage(intervals);

        String title = String.format(Locale.getDefault(), "All-time: %.02f %s, currently: %.02f %s\nFirst entry: %s",
                average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));

        if(xyPlot != null)
        {
            //Prepare graph
            PrepareXYPlot(xyPlot, title, "Entry", "Interval");

            //Add the 3 plots
            AddPlot(context, xyPlot, intervals, "Data", false, R.xml.line_point_formatter_with_labels);
            AddPlot(context, xyPlot, allAve, "Ave", false, R.xml.line_point_formatter_ave_all);
            AddPlot(context, xyPlot, ave, "Running Ave", false, R.xml.line_point_formatter_with_labels_2);

            //Configure X and Y axis labels
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.BOTTOM, null);

            new APZoomListener(xyPlot, intervals.length, ArrayMath.GetMax(intervals));
        }
        else
        {
            graph.EasyGraph(intervals);
            graph.Plots.get(0).SetColor(Color.WHITE);
            graph.Plots.get(0).DrawPoints = false;

            graph.Plots.add(new GraphPlot(allAve, 1));
            graph.Plots.get(1).SetColor(Color.RED);
            graph.Plots.get(1).DrawPoints = false;

            graph.Plots.add(new GraphPlot(ave, 1));
            graph.Plots.get(2).SetColor(Color.GREEN);
            graph.Plots.get(2).DrawPoints = false;

            graph.Title.Text = title;
            //mGraph.AddDateInfo(startDate);

            graph.BottomAxis.DrawLabels = false;
        }
    }

    public static void DrawValuesGraph(Context context, GraphView graph, XYPlot xyPlot, LogFile log, PhoneLogFile phoneLog, LoggerConfig config, GraphSettings settings)
    {
        Calendar[] dates;
        float[] values;
        if(settings.dataType.equals("Phone calls"))
        {
            List<PhoneLogEntry> data = phoneLog.ExtractLog(settings.filter, settings.dayFilters);
            dates = new Calendar[data.size()];
            values = new float[data.size()];
            for(int i=0; i<data.size(); i++)
            {
                dates[i] = data.get(i).GetDate();

                try
                {
                    values[i] = (float)data.get(i).GetDuration();
                }
                catch(Exception e)
                {
                    Log.e("GraphHelpers", "Error parsing value from phone log entry");
                }
            }
        }
        else
        {
            //Extract the data specified by "category"
            LogItem item = config.GetEntryByName(settings.dataType);
            List<LogEntry> data = log.ExtractLog(item, settings.filter, settings.dayFilters);
            dates = new Calendar[data.size()];
            values = new float[data.size()];
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
        }

        float[] allAves = ArrayMath.GetAllTimeRunningAverageCurve(values);
        float[] runningAves = ArrayMath.GetRunningAverageCurve(values, 30);

        Calendar firstDate = Calendar.getInstance();
        if(settings.numDays > 0)
        {
            if(settings.numDays > values.length)
                settings.numDays = values.length;

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
            for(int i=0; i<values.length; i++)
                if(dates[i].after(filterDate))
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

        String title = String.format(Locale.getDefault(), "All-time: %.02f, currently: %.02f",
                allAves[allAves.length - 1], runningAves[runningAves.length - 1]);

        if(xyPlot != null)
        {
            //Prepare graph
            PrepareXYPlot(xyPlot, title, "Date", settings.dataType);

            //Add the plot
            AddXYPlot(context, xyPlot, x, values, "Data", false, R.xml.line_point_formatter_with_labels);
            AddXYPlot(context, xyPlot, x, allAves, "Ave", false, R.xml.line_point_formatter_ave_all);
            AddXYPlot(context, xyPlot, x, runningAves, "Running Ave", false, R.xml.line_point_formatter_with_labels_2);

            //Configure X and Y axis labels
            ConfigureAxisLabels(xyPlot, XYGraphWidget.Edge.LEFT, null);
            ConfigureScatterAxisLabels(xyPlot, x, dates);

            double xMax = ArrayMath.GetMax(x);
            new APZoomListener(xyPlot, xMax, max);

            xyPlot.setRangeBoundaries(min, BoundaryMode.FIXED, max, BoundaryMode.FIXED);
            xyPlot.setDomainBoundaries(0, BoundaryMode.FIXED, xMax, BoundaryMode.FIXED);
        }
        else
        {
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
            graph.Title.Text = title;

            //Add the weekend shading and start-of-month indicators
            graph.AddDateInfo(firstDate, Calendar.getInstance(), true);

            //Turn off labels for the bottom axis since they are drawn with the date info
            //mGraph.BottomAxis.DrawLabels = false;
        }
    }

    public static String DrawStats(LogFile log, PhoneLogFile phoneLog, LogItem item, int midnightHour, GraphSettings settings)
    {
        if(item.Name.equals("Phone calls"))
        {
            return phoneLog.GetStats(midnightHour, settings.filter, settings.dayFilters);
        }

        return log.GetStats(item, midnightHour, settings.filter, settings.dayFilters);
    }

    public static String DrawComments(LogFile log, PhoneLogFile phoneLog, LoggerConfig config, GraphSettings settings)
    {
        if(settings.dataType.equals("Phone calls"))
        {
            return phoneLog.GetCommentSummary(settings.filter, settings.dayFilters);
        }

        LogItem item = config.GetEntryByName(settings.dataType);
        return log.GetCommentSummary(item, settings.filter, settings.dayFilters);
    }

    public static String DrawRecentHistory(LogFile log, PhoneLogFile phoneLog, LoggerConfig config, GraphSettings settings)
    {
        if(settings.dataType.equals("Phone calls"))
        {
            List<PhoneLogEntry> entries = phoneLog.ExtractLog(settings.filter, settings.dayFilters);

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

                List<PhoneLogEntry> tempData = new ArrayList<>();

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
            PhoneLogEntry lastLogEntry = null;
            for(int i=startIndex; i<numEntries; i++)
            {
                //Add the interval to the end of the previous line (after 1st entry), then start a new line and write the main entry
                PhoneLogEntry logEntry = entries.get(i);
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
        else
        {
            LogItem item = config.GetEntryByName(settings.dataType);
            List<LogEntry> entries = log.ExtractLog(item, settings.filter, settings.dayFilters);

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
    }

    private static void PrepareXYPlot(XYPlot xyPlot, String title, String xTitle, String yTitle)
    {
        xyPlot.setTitle(title);
        xyPlot.setDomainLabel(xTitle);
        xyPlot.setRangeLabel(yTitle);

//        xyPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 1);
        xyPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 1);
        xyPlot.getGraph().setLinesPerDomainLabel(1);
        xyPlot.getGraph().setLinesPerRangeLabel(1);

        xyPlot.getGraph().getDomainGridLinePaint().setColor(Color.GRAY);
        xyPlot.getGraph().getRangeGridLinePaint().setColor(Color.GRAY);
        xyPlot.getGraph().getDomainOriginLinePaint().setColor(Color.GRAY);
        xyPlot.getGraph().getRangeOriginLinePaint().setColor(Color.GRAY);
    }

    private static void AddPlot(Context context, XYPlot xyPlot, float[] data, String title, boolean interpolate, int formatResource)
    {
        Number[] series1Numbers = new Number[data.length];
        for(int i=0; i<data.length; i++)
        {
            series1Numbers[i] = data[i];
        }

        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, title);

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter series1Format =
                new LineAndPointFormatter(context, formatResource);

        // add an "dash" effect to the series2 line:
        //series2Format.getLinePaint().setPathEffect(new DashPathEffect(new float[] {

        // always use DP when specifying pixel sizes, to keep things consistent across devices:
        //        PixelUtils.dpToPix(20),
        //        PixelUtils.dpToPix(15)}, 0));

        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
        if(interpolate)
        {
            series1Format.setInterpolationParams(
                    new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        }

        // add a new series' to the xyplot:
        xyPlot.addSeries(series1, series1Format);
    }

    private static LineAndPointFormatter AddXYPlot(Context context, XYPlot xyPlot, float[] xVals, float[] yVals, String title, boolean interpolate, int formatResource)
    {
        List<Number> series1Numbers = new ArrayList<>();
        for(int i=0; i<xVals.length; i++)
        {
            series1Numbers.add(xVals[i]);
            series1Numbers.add(yVals[i]);
        }

        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        XYSeries series1 = new SimpleXYSeries(series1Numbers, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, title);

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter(context, formatResource);

        // add an "dash" effect to the series2 line:
        //series2Format.getLinePaint().setPathEffect(new DashPathEffect(new float[] {

        // always use DP when specifying pixel sizes, to keep things consistent across devices:
        //        PixelUtils.dpToPix(20),
        //        PixelUtils.dpToPix(15)}, 0));

        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
        if(interpolate)
        {
            series1Format.setInterpolationParams(
                    new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        }

        // add a new series' to the xyplot:
        xyPlot.addSeries(series1, series1Format);

        return series1Format;
    }

    private static void ConfigureAxisLabels(XYPlot xyPlot, final XYGraphWidget.Edge edge, final String[] labels)
    {
        xyPlot.getGraph().getLineLabelStyle(edge).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                int i = Math.round(((Number) obj).floatValue());
                if(labels != null)
                {
                    Log.d("GH", i+"..."+edge);
                    return toAppendTo.append(labels[i]);
                }

                Log.d("GH", i+"..."+edge);
                return toAppendTo.append(i);

            }
            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });
    }

    private static void ConfigureScatterAxisLabels(XYPlot xyPlot, final float[] xValues, final Calendar[] dates)
    {
        xyPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            @Override
            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                float f = ((Number) obj).floatValue();
                if(dates != null && xValues != null && xValues.length > 0)
                {
                    //Find closest xValues
                    int before = 0;
                    int after = 0;
                    for(int i=0; i<xValues.length; i++)
                    {
                        float x = xValues[i];
                        if(x == f)
                        {
                            before = i;
                            after = i;
                            break;
                        }

                        if(x < f)
                            before = i;

                        if(x > f)
                        {
                            after = i;
                            break;
                        }
                    }

                    double ratio = 0;
                    if(after != before)
                        ratio = (f - xValues[before]) / (xValues[after] - xValues[before]);

                    long milliseconds = (long)(dates[before].getTimeInMillis() + (ratio * (dates[after].getTimeInMillis() - dates[before].getTimeInMillis())));
                    //milliseconds = dates[(int)f].getTimeInMillis();

                    Calendar xDate = Calendar.getInstance();
                    xDate.setTimeInMillis(milliseconds);

                    Calendar useDate = xDate; //dates[(int)f]
                    int monthDay = useDate.get(Calendar.DAY_OF_MONTH);
                    int month = useDate.get(Calendar.MONTH) + 1;
                    int year = useDate.get(Calendar.YEAR) % 100;
                    String dateStr = String.format(Locale.getDefault(), "%02d%02d%02d", year, month, monthDay);

                    Log.d("GraphHelpers", ratio+","+year+","+month+","+monthDay+","+f);

                    return toAppendTo.append(dateStr);
                }

                return toAppendTo.append(Math.round(f));

            }
            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });
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
