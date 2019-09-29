package com.colebianchi.apps.eon;

public class AppRelease
{
	private String versionNumber;
	private String releaseMessage;
	private String releaseName;
	private String downloadURL;
	private String fileName;

	public AppRelease(String versionNumber, String releaseMessage, String releaseName, String downloadURL, String fileName)
	{
		this.versionNumber = versionNumber;
		this.releaseMessage = releaseMessage;
		this.releaseName = releaseName;
		this.downloadURL = downloadURL;
		this.fileName = fileName;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getDownloadURL()
	{
		return downloadURL;
	}

	public String getVersionName()
	{
		return releaseName;
	}

	public String getVersionNumber()
	{
		return versionNumber;
	}

	public String getUpdateMessage()
	{
		return releaseMessage;
	}
}
