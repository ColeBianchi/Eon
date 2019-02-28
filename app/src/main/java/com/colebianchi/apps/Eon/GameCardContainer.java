package com.colebianchi.apps.Eon;

import org.json.JSONObject;

public class GameCardContainer
{
	public String url;
	public JSONObject game;
	public JSONObject item;
	public String data;

	public GameCardContainer(String url, JSONObject game, JSONObject item)
	{
		this.url = url;
		this.game = game;
		this.item = item;
	}
}
