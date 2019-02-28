package com.colebianchi.apps.Eon;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetGameCardAsync extends AsyncTask<GameCardContainer, Void, GameCardContainer>
{
	public interface AsyncResponse
	{
		void processFinish(GameCardContainer output);
	}

	public AsyncResponse delegate;

	public GetGameCardAsync(AsyncResponse delegate)
	{
		this.delegate = delegate;
	}

	@Override
	protected GameCardContainer doInBackground(GameCardContainer[] param)
	{
		try
		{
			param[0].data = getHTML(param[0].url);

			return param[0];
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	protected void onPostExecute(GameCardContainer result)
	{
		delegate.processFinish(result);
	}

	private String getHTML(String urlToRead) throws Exception
	{
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null)
		{
			result.append(line);
		}
		rd.close();
		return result.toString();
	}
}