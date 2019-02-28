package com.colebianchi.apps.Eon.adapters;

public class GameCard
{
	String title;
	String time;
	public String streamURL;
	int id;

	public GameCard(String title, String time, int id, String streamURL)
	{
		this.title = title;
		this.time = time;
		this.id = id;
		this.streamURL = streamURL;
	}
}