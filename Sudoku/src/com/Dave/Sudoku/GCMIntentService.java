package com.Dave.Sudoku;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService
{

	@Override
	protected void onError(Context arg0, String arg1)
	{
		Log.i("GCM", "##### ERROR ##### " + arg1);
	}

	@Override
	protected void onMessage(Context arg0, Intent arg1)
	{
		Log.i("GCM", "##### Message #####");
	
		int notificationID = 1;
		
		Intent i = new Intent(this, SudokuActivity.class);
		i.putExtra("notificationID", notificationID);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);
		
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); 
		Notification notif = new Notification(R.drawable.ic_launcher, "Game ready!", System.currentTimeMillis());
		
		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		
		CharSequence from = "Twodoku";
		CharSequence message = "It's your turn!";
		
		notif.setLatestEventInfo(this, from, message, pendingIntent);
		
		notif.vibrate = new long[] { 100, 250, 100, 500};
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		nm.notify(notificationID, notif);
		
		
	}

	@Override
	protected void onRegistered(Context arg0, String arg1)
	{
		Log.i("GCM", "##### Registered ##### " + arg1);
	}

	@Override
	protected void onUnregistered(Context arg0, String arg1)
	{
		Log.i("GCM", "##### Unregistered ##### " + arg1);
	}
}
