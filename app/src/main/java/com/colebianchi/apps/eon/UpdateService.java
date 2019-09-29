package com.colebianchi.apps.eon;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UpdateService
{
	private static final String TAG = UpdateService.class.getSimpleName();
	private AppRelease appVersion;
	private AppRelease[] githubReleases;
	private Context context;

	public UpdateService(Context context)
	{
		try
		{
			this.context = context;
			githubReleases = setGithubReleases();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isReady()
	{
		if (githubReleases != null && appVersion != null)
		{
			Log.i(TAG, "Update Service is ready");
			return true;
		}

		return false;
	}

	public boolean isUpdateAvailable()
	{
		int avail = versionCompare(getCurrentVersion().getVersionNumber(), getLatestVersion().getVersionNumber());

		Log.i(TAG, avail + "");

		if (avail == -1)
		{
			return true;
		}

		return false;
	}

	public AppRelease getCurrentVersion()
	{
		return appVersion;
	}

	public AppRelease getLatestVersion()
	{
		return githubReleases[0];
	}

	private AppRelease[] setGithubReleases()
	{
		AsyncHTMLGetRequest githubRequest = new AsyncHTMLGetRequest(new AsyncHTMLGetRequest.AsyncResponse()
		{
			@Override
			public void processFinish(String output)
			{
				try
				{
					JSONArray releases = new JSONArray(output);

					List<AppRelease> rels = new ArrayList<>();

					for (int i = 0; i < releases.length(); i++)
					{
						String rVersionNumber = releases.getJSONObject(i).getString("tag_name");
						String rReleaseName = releases.getJSONObject(i).getString("name");
						String rReleaseMessage = releases.getJSONObject(i).getString("body");
						JSONArray assets = releases.getJSONObject(i).getJSONArray("assets");
						String rReleaseDownloadURL = "";
						String rReleaseFileName = "";

						for (int j = 0; j < assets.length(); j++)
						{
							if (assets.getJSONObject(j).getString("name").contains(".apk"))
							{
								rReleaseDownloadURL = assets.getJSONObject(j).getString("browser_download_url");
								rReleaseFileName = assets.getJSONObject(j).getString("name");
							}
						}

						AppRelease rel = new AppRelease(rVersionNumber, rReleaseName, rReleaseMessage, rReleaseDownloadURL, rReleaseFileName);
						rels.add(rel);
					}


					githubReleases = new AppRelease[rels.size()];
					githubReleases = rels.toArray(githubReleases);

					PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
					appVersion = getGithubRelease(pInfo.versionName);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		githubRequest.execute(new String[]{"https://api.github.com/repos/RaviusSky/Eon/releases"});

		return githubReleases;
	}

	private AppRelease getGithubRelease(String versionName)
	{
		if (githubReleases != null)
		{
			for (AppRelease release : githubReleases)
			{
				if (release.getVersionNumber().equals(versionName))
				{
					return release;
				}
			}
		}

		Log.e(TAG, "releases are null, can't find specific release");

		return null;
	}

	private int versionCompare(String str1, String str2)
	{
		try (Scanner s1 = new Scanner(str1);
			 Scanner s2 = new Scanner(str2);)
		{
			s1.useDelimiter("\\.");
			s2.useDelimiter("\\.");

			while (s1.hasNextInt() && s2.hasNextInt())
			{
				int v1 = s1.nextInt();
				int v2 = s2.nextInt();
				if (v1 < v2)
				{
					return -1;
				}
				else if (v1 > v2)
				{
					return 1;
				}
			}

			if (s1.hasNextInt() && s1.nextInt() != 0)
			{
				return 1; //str1 has an additional lower-level version number
			}
			if (s2.hasNextInt() && s2.nextInt() != 0)
			{
				return -1; //str2 has an additional lower-level version
			}

			return 0;
		} // end of try-with-resources
	}
}
