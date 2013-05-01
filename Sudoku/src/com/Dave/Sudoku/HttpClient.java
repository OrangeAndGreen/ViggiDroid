package com.Dave.Sudoku;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class HttpClient
{
	private Context mContext = null;
	
	public HttpClient(Context context)
	{
		mContext = context;
	}
	
	private InputStream OpenHttpConnection(String urlString) throws IOException
	{
		InputStream stream = null;
		int response = -1;
		
		try
		{
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();
			
			if(!(connection instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");
			
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setAllowUserInteraction(false);
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("Method", "Gamelist");
			httpConnection.connect();
			response = httpConnection.getResponseCode();
			if(response == HttpURLConnection.HTTP_OK)
				stream = httpConnection.getInputStream();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new IOException("Error connecting");
		}
		
		return stream;
	}
	
	private String DownloadText(String urlString)
	{
		int bufferSize = 2000;
		InputStream stream = null;
		
		try
		{
			stream = OpenHttpConnection(urlString);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "";
		}
		
		InputStreamReader reader = new InputStreamReader(stream);
		int charRead = 0;
		String str = "";
		char[] inputBuffer = new char[bufferSize];
		
		try
		{
			while((charRead = reader.read(inputBuffer)) > 0)
			{
				String readString = String.copyValueOf(inputBuffer, 0, charRead);
				str += readString;
				inputBuffer = new char[bufferSize];
			}
			stream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "";
		}
		
		return str;
	}
	
	private class DownloadTextTask extends AsyncTask<String, Void, String>
	{
		private Context mContext = null;
		
		public DownloadTextTask(Context context)
		{
			mContext = context;
		}
		
		protected String doInBackground(String... urls)
		{
			String result = null;
			try
			{
				result = DownloadText(urls[0]);
			}
			catch(Exception e)
			{
				Log.e("HttpClient", "Error communicating with server");
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(String result)
		{
			if(mContext != null)
				Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
		}
	}
	
	public void TestCommunication()
	{
		DownloadTextTask task = new DownloadTextTask(mContext);
		task.execute("http://10.0.2.2:8080");
	}
}
