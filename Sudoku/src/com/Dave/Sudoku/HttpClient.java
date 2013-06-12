package com.Dave.Sudoku;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.Dave.Sudoku.Game.SudokuGameTwoPlayer;


import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

public class HttpClient
{
	public boolean Failed = false;

	public HttpClient()
	{
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

	private void OpenHttpPostConnection(String urlString, List<Header> headers, List<Header> postData) throws IOException, ConnectionException, LoginException
	{
		Failed = false;
		try
		{
			String dataString = "";
			for (int i = 0; i < postData.size(); i++)
			{
				// Add the POST data to the string
				if (i > 0)
					dataString += "&";
				dataString += postData.get(i).Key + "=" + postData.get(i).Value;
			}

			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();

			if (!(connection instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");

			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setAllowUserInteraction(false);
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpConnection.setRequestProperty("Content-Length", "" + Integer.toString(dataString.length()));
			httpConnection.setRequestProperty("Content-Language", "en-US");
			httpConnection.setUseCaches(false);
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);

			for (int i = 0; i < headers.size(); i++)
			{
				httpConnection.setRequestProperty(headers.get(i).Key, headers.get(i).Value);
			}

			// Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(dataString);
			wr.flush();
			wr.close();

			httpConnection.connect();
			int response = httpConnection.getResponseCode();
			if (response != HttpURLConnection.HTTP_OK)
				throw new LoginException();
		}
		catch (IOException e)
		{
			Failed = true;
			e.printStackTrace();
			throw new ConnectionException();
		}
	}

	private InputStream OpenHttpGetConnection(String urlString,
			List<Header> headers) throws IOException
	{
		Failed = false;
		InputStream stream = null;
		int response = -1;

		try
		{
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();

			if (!(connection instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");

			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setAllowUserInteraction(false);
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestMethod("GET");

			for (int i = 0; i < headers.size(); i++)
			{
				httpConnection.setRequestProperty(headers.get(i).Key,
						headers.get(i).Value);
			}

			// httpConnection.setRequestProperty("Method", "Gamelist");
			// httpConnection.setRequestProperty("Player1", "Dave");

			httpConnection.connect();
			response = httpConnection.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK)
				stream = httpConnection.getInputStream();
		} catch (Exception e)
		{
			Failed = true;
			e.printStackTrace();
			throw new IOException("Error connecting");
		}

		return stream;
	}

	private String DownloadText(String urlString, List<Header> headers) throws ConnectionException
	{
		int bufferSize = 2000;
		InputStream stream = null;

		// Send the request
		try
		{
			stream = OpenHttpGetConnection(urlString, headers);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ConnectionException();
		}

		InputStreamReader reader = new InputStreamReader(stream);
		int charRead = 0;
		String str = "";
		char[] inputBuffer = new char[bufferSize];

		try
		{
			// Read the response data
			while ((charRead = reader.read(inputBuffer)) > 0)
			{
				String readString = String
						.copyValueOf(inputBuffer, 0, charRead);
				str += readString;
				inputBuffer = new char[bufferSize];
			}
			stream.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ConnectionException();
		}

		return str;
	}
	
	private boolean CheckHttpResponse(String line)
	{
		return line.startsWith("HTTP/1.0 200 OK");
	}
	
	private class ConnectionException extends Exception
	{ }
	
	private class LoginException extends Exception
	{ }
	
	
	
	// /// Create new player /////
	
	public void CreatePlayer(String server, String player, String password, String gcmId, CreatePlayerListener listener)
	{
		CreatePlayerTask task = new CreatePlayerTask(player, password, gcmId, listener);
		task.execute(server);
	}

	//TODO: Figure out how to get rid of the Boolean return value (should be Void)
	private class CreatePlayerTask extends AsyncTask<String, Void, Boolean>
	{
		private String mPlayer = null;
		private String mPassword = null;
		private String mGcmId = null;
		private CreatePlayerListener mListener = null;
		private boolean mConnectionFailed = false;

		public CreatePlayerTask(String player, String password, String gcmId, CreatePlayerListener listener)
		{
			mPlayer = player;
			mPassword = password;
			mGcmId = gcmId;
			mListener = listener;
		}

		@Override
		protected Boolean doInBackground(String... urls)
		{
			Boolean ret = false;
			
			try
			{
				ret = DoCreatePlayer(urls[0], mPlayer, mPassword, mGcmId);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			
			return ret;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(result)
				mListener.OnPlayerCreated();
			else
				mListener.OnCreateFailed();
		}
	}

	private Boolean DoCreatePlayer(String server, String player, String password, String gcmId) throws ConnectionException
	{
		try
		{
			List<Header> headers = new ArrayList<Header>();
			headers.add(new Header("Method", "AddPlayer"));
			headers.add(new Header("Player", player));
			headers.add(new Header("Password", password));
			if(gcmId != null)
				headers.add(new Header("GcmId", gcmId));

			try
			{
				OpenHttpPostConnection(server, headers, new ArrayList<Header>());
				return true;
			}
			catch(LoginException e)
			{
				//A login exception here means we couldn't create the new player, not that login failed
				return false;
			}
		}
		catch (IOException e)
		{
			Log.e("HttpClient", "Error communicating with server" + e.getLocalizedMessage());
			throw new ConnectionException();
		}
	}
	
	public interface CreatePlayerListener
	{
		public void OnPlayerCreated();
		
		public void OnCreateFailed();
		
		public void OnConnectionFailed();
	}
	
	
	
	
	// /// Change password /////

	public void ChangePassword(String server, String player, String password, String newPassword, PasswordChangedListener listener)
	{
		ChangePasswordTask task = new ChangePasswordTask(player, password, newPassword, listener);
		task.execute(server);
	}

	private class ChangePasswordTask extends AsyncTask<String, Void, List<SudokuGameTwoPlayer>>
	{
		private String mPlayer = null;
		private String mPassword = null;
		private String mNewPassword = null;
		private PasswordChangedListener mListener = null;
		private boolean mConnectionFailed = false;
		private boolean mLoginFailed = false;

		public ChangePasswordTask(String player, String password, String newPassword, PasswordChangedListener listener)
		{
			mPlayer = player;
			mPassword = password;
			mNewPassword = newPassword;
			mListener = listener;
		}

		@Override
		protected List<SudokuGameTwoPlayer> doInBackground(String... urls)
		{
			List<SudokuGameTwoPlayer> ret = null;
			
			try
			{
				ret = DoChangePassword(urls[0], mPlayer, mPassword, mNewPassword);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			catch(LoginException e)
			{
				mLoginFailed = true;
			}
			
			return ret;
		}

		@Override
		protected void onPostExecute(List<SudokuGameTwoPlayer> result)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(mLoginFailed)
				mListener.OnLoginFailed();
			else
				mListener.OnPasswordChanged(result, mNewPassword);
		}
	}

	private List<SudokuGameTwoPlayer> DoChangePassword(String server, String player, String password, String newPassword) throws ConnectionException, LoginException
	{
		List<SudokuGameTwoPlayer> result = new ArrayList<SudokuGameTwoPlayer>();
			
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Method", "ChangePassword"));
		headers.add(new Header("Player", player));
		headers.add(new Header("Password", password));
		headers.add(new Header("NewPassword", newPassword));

		
		String strResult = DownloadText(server, headers);

		//Log.d("", "Gamelist response: " + strResult);
			
		String[] lines = strResult.split("\n");
		if(!CheckHttpResponse(lines[0]))
			throw new LoginException();
			
		for (int i=1; i<lines.length; i++)
			result.add(SudokuGameTwoPlayer.FromString(lines[i], true));
		
		return result;
	}
		
	public interface PasswordChangedListener
	{
		public void OnPasswordChanged(List<SudokuGameTwoPlayer> gameList, String newPassword);
		
		public void OnLoginFailed();
		
		public void OnConnectionFailed();
	}
	
	
	
	
	// /// Get player stats /////
	
	public void GetPlayerStats(String server, String player, String password, StatsReadyListener listener)
	{
		GetPlayerStatsTask task = new GetPlayerStatsTask(player, password, listener);
		task.execute(server);
	}
	
	private class GetPlayerStatsTask extends AsyncTask<String, Void, PlayerStats>
	{
		private String mPlayer = null;
		private String mPassword = null;
		private StatsReadyListener mListener = null;
		private boolean mConnectionFailed = false;
		private boolean mLoginFailed = false;
		
		public GetPlayerStatsTask(String player, String password, StatsReadyListener listener)
		{
			mPlayer = player;
			mPassword = password;
			mListener = listener;
		}
		
		protected PlayerStats doInBackground(String... urls)
		{
			PlayerStats ret = null;
			
			try
			{
				ret = DoGetPlayerStats(urls[0], mPlayer, mPassword);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			catch(LoginException e)
			{
				mLoginFailed = true;
			}
			
			return ret;
		}
		
		protected void onPostExecute(PlayerStats result)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(mLoginFailed)
				mListener.OnLoginFailed();
			else
				mListener.OnStatsReady(result);
		}
	}
	
	private PlayerStats DoGetPlayerStats(String server, String player, String password) throws ConnectionException, LoginException
	{
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Method", "PlayerStats"));
		headers.add(new Header("Player", player));
		headers.add(new Header("Password", password));

		String strResult = DownloadText(server, headers);
		
		String[] lines = strResult.split("\n");
		if(!CheckHttpResponse(lines[0]))
			throw new LoginException();
		
		PlayerStats stats = new PlayerStats();
		
		for(int i=1; i<lines.length; i++)
		{
			String[] parts = lines[i].split("=");
			if(parts[0].equals("Wins"))
				stats.Wins = Integer.parseInt(parts[1].trim());
			else if(parts[0].equals("Losses"))
				stats.Losses = Integer.parseInt(parts[1].trim());
			else if(parts[0].equals("Streak"))
				stats.Streak = Integer.parseInt(parts[1].trim());
		}
		
		return stats;
	}
	
	public interface StatsReadyListener
	{
		public void OnStatsReady(PlayerStats stats);
		
		public void OnLoginFailed();
		
		public void OnConnectionFailed();
	}
	
	public class PlayerStats
	{
		public int Wins = 0;
		public int Losses = 0;
		public int Streak = 0;
	}
	
	
	
	// /// Get game list /////

	public void GetGameList(String server, String player, String password, String gcmId, GameListReadyListener listener)
	{
		GetGameListTask task = new GetGameListTask(player, password, gcmId, listener);
		task.execute(server);
	}

	private class GetGameListTask extends AsyncTask<String, Void, List<SudokuGameTwoPlayer>>
	{
		private String mPlayer = null;
		private String mPassword = null;
		private String mGcmId = null;
		private GameListReadyListener mListener = null;
		private boolean mConnectionFailed = false;
		private boolean mLoginFailed = false;

		public GetGameListTask(String player, String password, String gcmId, GameListReadyListener listener)
		{
			mPlayer = player;
			mPassword = password;
			mGcmId = gcmId;
			mListener = listener;
		}

		@Override
		protected List<SudokuGameTwoPlayer> doInBackground(String... urls)
		{
			List<SudokuGameTwoPlayer> ret = null;
			
			try
			{
				ret = DoGetGameList(urls[0], mPlayer, mPassword, mGcmId);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			catch(LoginException e)
			{
				mLoginFailed = true;
			}
			
			return ret;
		}

		@Override
		protected void onPostExecute(List<SudokuGameTwoPlayer> result)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(mLoginFailed)
				mListener.OnLoginFailed();
			else
				mListener.OnGameListReady(result);
		}
	}

	private List<SudokuGameTwoPlayer> DoGetGameList(String server, String player, String password, String gcmId) throws ConnectionException, LoginException
	{
		List<SudokuGameTwoPlayer> result = new ArrayList<SudokuGameTwoPlayer>();
		
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Method", "Gamelist"));
		headers.add(new Header("Player", player));
		headers.add(new Header("Password", password));
		if(gcmId != null)
			headers.add(new Header("GcmId", gcmId));
		
		String strResult = DownloadText(server, headers);

		//Log.d("", "Gamelist response: " + strResult);
			
		String[] lines = strResult.split("\n");
		if(!CheckHttpResponse(lines[0]))
			throw new LoginException();
			
		for (int i=1; i<lines.length; i++)
			result.add(SudokuGameTwoPlayer.FromString(lines[i], true));
		
		return result;
	}
	
	public interface GameListReadyListener
	{
		public void OnGameListReady(List<SudokuGameTwoPlayer> gameList);
		
		public void OnLoginFailed();
		
		public void OnConnectionFailed();
	}

	
	
	
	// /// Get game /////

	public void GetGame(String server, int gameId, String player, String password, GameReadyListener listener)
	{
		GetGameTask task = new GetGameTask(gameId, player, password, listener);
		task.execute(server);
	}
	

	private class GetGameTask extends AsyncTask<String, Void, SudokuGameTwoPlayer>
	{
		private Integer mGameId = 0;
		private GameReadyListener mListener = null;
		private String mPlayer = null;
		private String mPassword = null;
		private boolean mConnectionFailed = false;
		private boolean mLoginFailed = false;

		public GetGameTask(int gameId, String player, String password, GameReadyListener listener)
		{
			mGameId = gameId;
			mPlayer = player;
			mPassword = password;
			mListener = listener;
		}

		@Override
		protected SudokuGameTwoPlayer doInBackground(String... server)
		{
			SudokuGameTwoPlayer ret = null;
			
			try
			{
				ret = DoGetGame(server[0], mPlayer, mPassword, mGameId);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			catch(LoginException e)
			{
				mLoginFailed = true;
			}
			
			return ret;
		}

		@Override
		protected void onPostExecute(SudokuGameTwoPlayer game)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(mLoginFailed)
				mListener.OnLoginFailed();
			else
				mListener.OnGameReady(game);
		}
	}
	

	private SudokuGameTwoPlayer DoGetGame(String server, String player, String password, int gameId) throws ConnectionException, LoginException
	{
		SudokuGameTwoPlayer result = null;
		List<Header> headers = new ArrayList<Header>();
		headers.add(new Header("Method", "Game"));
		headers.add(new Header("GameId", Integer.toString(gameId)));
		headers.add(new Header("Player", player));
		headers.add(new Header("Password", password));
		try
		{
			String strResult = DownloadText(server, headers);
			
			String[] lines = strResult.split("\n");
			if(!CheckHttpResponse(lines[0]))
				throw new LoginException();
			
			result = SudokuGameTwoPlayer.FromString(lines[1], false);
		}
		catch (ConnectionException e)
		{
			Log.e("HttpClient", "Error communicating with server: " + e.getLocalizedMessage());
			StackTraceElement[] trace = e.getStackTrace();
			for (StackTraceElement element : trace)
			{
				Log.e("", element.getClassName() + " " + element.getLineNumber());
			}
			throw new ConnectionException();
		}
		return result;
	}
	
	public interface GameReadyListener
	{
		public void OnGameReady(SudokuGameTwoPlayer game);
		
		public void OnLoginFailed();
		
		public void OnConnectionFailed();
	}

	
	
	
	// /// Update game /////

	public void UpdateGame(String server, SudokuGameTwoPlayer game, String player, String password, GameUpdatedListener listener)
	{
		UpdateGameTask task = new UpdateGameTask(game, player, password, listener);
		task.execute(server);
	}

	
	//TODO: Figure out how to get rid of the Boolean return value (should be Void)
	private class UpdateGameTask extends AsyncTask<String, Void, Boolean>
	{
		private SudokuGameTwoPlayer mGame = null;
		private String mPlayer = null;
		private String mPassword = null;
		private GameUpdatedListener mListener = null;
		private boolean mConnectionFailed = false;
		private boolean mLoginFailed = false;

		public UpdateGameTask(SudokuGameTwoPlayer game, String player, String password, GameUpdatedListener listener)
		{
			mGame = game;
			mPlayer = player;
			mPassword = password;
			mListener = listener;
		}

		@Override
		protected Boolean doInBackground(String... server)
		{
			try
			{
				DoUpdateGame(server[0], mPlayer, mPassword, mGame);
			}
			catch(ConnectionException e)
			{
				mConnectionFailed = true;
			}
			catch(LoginException e)
			{
				mLoginFailed = true;
			}
			
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success)
		{
			if(mConnectionFailed)
				mListener.OnConnectionFailed();
			else if(mLoginFailed)
				mListener.OnLoginFailed();
			else
				mListener.OnGameUpdated();
		}
	}

	private void DoUpdateGame(String server, String player, String password, SudokuGameTwoPlayer game) throws ConnectionException, LoginException
	{
		try
		{
			List<Header> headers = new ArrayList<Header>();
			headers.add(new Header("Method", "Update"));
			headers.add(new Header("GameId", Integer.toString(game.GameId)));
			headers.add(new Header("Player", player));
			headers.add(new Header("Password", password));

			List<Header> data = new ArrayList<Header>();
			data.add(new Header("Player1", game.GetPlayer1Name()));
			data.add(new Header("Player1Score", Integer.toString(game.GetPlayer1Score())));
			data.add(new Header("Player2", game.GetPlayer2Name()));
			data.add(new Header("Player2Score", Integer.toString(game.GetPlayer2Score())));
			data.add(new Header("Status", Integer.toString(game.Status)));
			data.add(new Header("Turn", Integer.toString(game.GetCurrentPlayer())));

			// Build the strings for the different boards
			String playerBoard = "";
			String startingBoard = "";
			String multipliers = "";
			for (int x = 0; x < SudokuBoard.BoardSize; x++)
				for (int y = 0; y < SudokuBoard.BoardSize; y++)
				{
					byte cell = game.GetBoard().GetSubBoard(-1)[x][y];
					startingBoard += Integer.toString(cell);
					if (cell > 0)
						cell = 0;
					else
						cell = game.GetBoard().GetCell(new Point(x, y),true);
					playerBoard += Integer.toString(cell);

					multipliers += Integer.toString(game.GetBoard().GetCellMultiplier(new Point(x, y)));
				}

			data.add(new Header("LastMove", game.CurrentMove));
			data.add(new Header("PlayerBoard", playerBoard));

			if (game.GameId <= 0)
			{
				// This data only needs to be sent the first time a game is sent to the server
				data.add(new Header("HandSystem", game.GetHandSystem()));
				data.add(new Header("HandSize", Integer.toString(game.GetHand().size())));
				data.add(new Header("ScoringSystem", game.GetScoringSystem()));
				data.add(new Header("MultiplierSystem", game.GetMultiplierSystem()));
				data.add(new Header("BonusSystem", game.BonusSystem));
				data.add(new Header("StartingBoard", startingBoard));
				data.add(new Header("Multipliers", multipliers));
			}

			OpenHttpPostConnection(server, headers, data);
		}
		catch (IOException e)
		{
			Log.e("HttpClient", "Error communicating with server" + e.getLocalizedMessage());
			throw new ConnectionException();
		}
	}
	
	public interface GameUpdatedListener
	{
		public void OnGameUpdated();
		
		public void OnLoginFailed();
		
		public void OnConnectionFailed();
	}
}
