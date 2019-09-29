package com.colebianchi.apps.eon;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncHTMLGetRequest extends AsyncTask<String, Void, String>
{
	private String TAG = this.getClass().getName();

	public interface AsyncResponse
	{
		void processFinish(String output);
	}

	public AsyncResponse delegate;

	public AsyncHTMLGetRequest(AsyncResponse delegate)
	{
		this.delegate = delegate;
	}

	@Override
	protected String doInBackground(String[] param)
	{
		try
		{
			Log.i(TAG, "Requesting: "+param[0]);
			return getHTML(param[0]);
		}
		catch (Exception e)
		{
			return e.getMessage();
		}
	}

	@Override
	protected void onPostExecute(String result)
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