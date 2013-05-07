package com.Dave.Sudoku;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Point;
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
	
	private class Header
	{
		public String Key = null;
		public String Value = null;
		
		public Header(String key, String value)
		{
			Key = key;
			Value = value;
		}
	}
	
	private boolean OpenHttpPostConnection(String urlString, List<Header> headers, List<Header> postData) throws IOException
	{
		try
		{
			String dataString = "";
			for(int i=0; i<postData.size(); i++)
			{
				//Add the POST data to the string
				if(i > 0)
					dataString += "&";
				dataString += postData.get(i).Key + "=" + postData.get(i).Value;
			}
			
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();
			
			if(!(connection instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");
			
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setAllowUserInteraction(false);
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpConnection.setRequestProperty("Content-Length", "" + Integer.toString(dataString.length()));
			httpConnection.setRequestProperty("Content-Language", "en-US");
			httpConnection.setUseCaches (false);
		    httpConnection.setDoInput(true);
		    httpConnection.setDoOutput(true);
		      
			for(int i=0; i<headers.size(); i++)
			{
				httpConnection.setRequestProperty(headers.get(i).Key, headers.get(i).Value);
			}

		    //Send request
		    DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
		    wr.writeBytes (dataString);
		    wr.flush ();
		    wr.close ();
		    
			httpConnection.connect();
			int response = httpConnection.getResponseCode();
			if(response == HttpURLConnection.HTTP_OK)
				return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new IOException("Error connecting");
		}
		
		return false;
	}
	
	private InputStream OpenHttpGetConnection(String urlString, List<Header> headers) throws IOException
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
			
			for(int i=0; i<headers.size(); i++)
			{
				httpConnection.setRequestProperty(headers.get(i).Key, headers.get(i).Value);
			}
			
			//httpConnection.setRequestProperty("Method", "Gamelist");
			//httpConnection.setRequestProperty("Player1", "Dave");
			
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
	
	private String DownloadText(String urlString, List<Header> headers)
	{
		int bufferSize = 2000;
		InputStream stream = null;
		
		//Send the request
		try
		{
			stream = OpenHttpGetConnection(urlString, headers);
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
			//Read the response data
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
	
	
	///// Get game list /////
	
	public void GetGameList(String server, String player, GameListReadyListener listener)
	{
		GetGameListTask task = new GetGameListTask(player, listener);
		task.execute(server);
	}
	
	private class GetGameListTask extends AsyncTask<String, Void, List<TwoPlayerGameInfo>>
	{
		private String mPlayer = null;
		private GameListReadyListener mListener = null;
		
		public GetGameListTask(String player, GameListReadyListener listener)
		{
			mPlayer = player;
			mListener = listener;
		}

		@Override
		protected List<TwoPlayerGameInfo> doInBackground(String... urls)
		{
			List<TwoPlayerGameInfo> result = new ArrayList<TwoPlayerGameInfo>();
			List<Header> headers = new ArrayList<Header>();
			headers.add(new Header("Method", "Gamelist"));
			//TODO: Change this header to "Player"
			headers.add(new Header("Player1", mPlayer));
			try
			{
				String strResult = DownloadText(urls[0], headers);
				String[] lines = strResult.split("\n");
				for(String line : lines)
					result.add(TwoPlayerGameInfo.FromString(line));
			}
			catch(Exception e)
			{
				Log.e("HttpClient", "Error communicating with server" + e.getLocalizedMessage());
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(List<TwoPlayerGameInfo> result)
		{
			mListener.OnGameListReady(result);
		}
	}
	
	public interface GameListReadyListener
	{
		public void OnGameListReady(List<TwoPlayerGameInfo> gameList);
	}


	///// Get game /////
	
	public void GetGame(String server, int gameId, GameReadyListener listener)
	{
		GetGameTask task = new GetGameTask(gameId, listener);
		task.execute(server);
	}
	
	private class GetGameTask extends AsyncTask<String, Void, SudokuGameTwoPlayer>
	{
		private Integer mGameId = 0;
		private GameReadyListener mListener = null;
		
		public GetGameTask(int gameId, GameReadyListener listener)
		{
			mGameId = gameId;
			mListener = listener;
		}
		
		@Override
		protected SudokuGameTwoPlayer doInBackground(String... server)
		{
			//TODO: Retrieve the game from the server
			return null;
		}
		
		@Override
		protected void onPostExecute(SudokuGameTwoPlayer game)
		{
			mListener.OnGameReady(game);
		}
	}
	
	public interface GameReadyListener
	{
		public void OnGameReady(SudokuGameTwoPlayer game);
	}
	

	///// Update game /////
	
	public void UpdateGame(String server, SudokuGameTwoPlayer game, GameUpdatedListener listener)
	{
		UpdateGameTask task = new UpdateGameTask(game, listener);
		task.execute(server);
	}
	
	private class UpdateGameTask extends AsyncTask<String, Void, Boolean>
	{
		private SudokuGameTwoPlayer mGame = null;
		private GameUpdatedListener mListener = null;
		
		public UpdateGameTask(SudokuGameTwoPlayer game, GameUpdatedListener listener)
		{
			mGame = game;
			mListener = listener;
		}
		
		@Override
		protected Boolean doInBackground(String... server)
		{
			boolean success = false;
			try
			{
				List<Header> headers = new ArrayList<Header>();
				headers.add(new Header("GameID", Integer.toString(mGame.GameID)));
				
				List<Header> data = new ArrayList<Header>();
				data.add(new Header("Player1", mGame.GetPlayer1Name()));
				data.add(new Header("Player1Score", Integer.toString(mGame.GetPlayer1Score())));
				data.add(new Header("Player2", mGame.GetPlayer2Name()));
				data.add(new Header("Player2Score", Integer.toString(mGame.GetPlayer2Score())));
				data.add(new Header("Active", "true"));
				data.add(new Header("Turn", Integer.toString(mGame.GetCurrentPlayer())));
				
				//String board = "";
				byte[][] array = mGame.GetBoard().GetSubBoard(2);
		    	//for(int x = 0 ; x<SudokuBoard.BoardSize; x++)
		    	//	for(int y=0; y<SudokuBoard.BoardSize; y++)
		    	//		board += Integer.toString(array[x][y]);
		    	
		    	//Log.d("", "Initial board: " + board);
				
				//Build the strings for the different boards
				String playerBoard = "";
				String startingBoard = "";
				String multipliers = "";
				for(int x = 0; x < SudokuBoard.BoardSize; x++)
					for(int y = 0; y < SudokuBoard.BoardSize; y++)
					{
						byte cell = mGame.GetBoard().GetSubBoard(-1)[x][y];
						startingBoard += Integer.toString(cell);
						if(cell > 0)
							cell = 0;
						else
							cell = mGame.GetBoard().GetCell(new Point(x, y), true);
						playerBoard+= Integer.toString(cell);
						
						multipliers += Integer.toString(mGame.GetBoard().GetCellMultiplier(new Point(x, y)));
					}
				
				data.add(new Header("PlayerBoard", playerBoard));
				
				if(mGame.GameID <= 0)
				{
					//This data only needs to be sent the first time a game is sent to the server
					data.add(new Header("HandSystem", mGame.GetHandSystem()));
					data.add(new Header("HandSize", Integer.toString(mGame.GetHand().size())));
					data.add(new Header("ScoringSystem", mGame.GetScoringSystem()));
					data.add(new Header("StartingBoard", startingBoard));
					data.add(new Header("Multipliers",  multipliers));
				}
				
				success = OpenHttpPostConnection(server[0], headers, data);
			}
			catch(IOException e)
			{
				Log.e("HttpClient", "Error communicating with server" + e.getLocalizedMessage());
			}
			return success;
		}
		
		@Override
		protected void onPostExecute(Boolean success)
		{
			mListener.OnGameUpdated(success);
		}
	}
	
	public interface GameUpdatedListener
	{
		public void OnGameUpdated(boolean success);
	}
}
