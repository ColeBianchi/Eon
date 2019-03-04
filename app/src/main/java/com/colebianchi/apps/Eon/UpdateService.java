package com.colebianchi.apps.Eon;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.colebianchi.apps.Eon.util.LogUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

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
			LogUtils.i(TAG, "Update Service is ready");
			return true;
		}

		return false;
	}

	public boolean isUpdateAvailable()
	{
		if (githubReleases != null)
		{
			int[] cVersionParts = StringArrToIntArr(getCurrentVersion().getVersionNumber().split("\\."));

			for (AppRelease r : githubReleases)
			{
				int[] tVersionParts = StringArrToIntArr(r.getVersionNumber().split("\\."));

				if (cVersionParts[0] < tVersionParts[0])
				{
					return true;
				}
				else
				{
					if (cVersionParts[1] < tVersionParts[1])
					{
						return true;
					}
					else
					{
						if (cVersionParts[2] < tVersionParts[2])
						{
							return true;
						}
					}
				}
			}
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
		GetHTMLAsync githubRequest = new GetHTMLAsync(new GetHTMLAsync.AsyncResponse()
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

		LogUtils.e(TAG, "releases are null, can't find specific release");

		return null;
	}

	private int[] StringArrToIntArr(String[] s)
	{
		int[] result = new int[s.length];
		for (int i = 0; i < s.length; i++)
		{
			result[i] = Integer.parseInt(s[i]);
		}
		return result;
	}
}
