package com.colebianchi.apps.eon;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.colebianchi.apps.eon.vpn.util.LogUtils;
import com.colebianchi.apps.eon.vpn.vservice.VhostsService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity
{
	private String TAG = this.getClass().getName();

	private SectionsPagerAdapter pageAdapter;
	private ViewPager mViewPager;

	private String todaysDate;
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private DatePickerDialog.OnDateSetListener datePicker;

	private UpdateService updateService;

	//VPN
	private static final int VPN_REQUEST_CODE = 0x0F;
	public static final String PREFS_NAME = MainActivity.class.getName();
	public static final String IS_LOCAL = "IS_LOCAL";
	public static final String HOSTS_URI = "HOST_URI";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LogUtils.context = getApplicationContext();

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		pageAdapter = new SectionsPagerAdapter();
		mViewPager = findViewById(R.id.container);
		mViewPager.setAdapter(pageAdapter);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy);

		try
		{
			TrustManager[] victimizedManager = new TrustManager[]
			{
				new X509TrustManager()
				{

					public X509Certificate[] getAcceptedIssuers()
					{

						X509Certificate[] myTrustedAnchors = new X509Certificate[0];

						return myTrustedAnchors;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType)
					{
					}

					@Override
					public void checkServerTrusted(X509Certificate[] certs, String authType)
					{
					}
				}};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, victimizedManager, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
			{
				@Override
				public boolean verify(String s, SSLSession sslSession)
				{
					return true;
				}
			});
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}

		updateService = new UpdateService(getBaseContext());

		Date date = new Date();
		todaysDate = formatter.format(date);

		Bundle bundle = getIntent().getExtras();

		if (getIntent().hasExtra("date"))
		{
			todaysDate = bundle.getString("date");
			LogUtils.i(TAG, "Changing date to " + todaysDate);
		}
		else
		{
			LogUtils.i(TAG, "No custom date specified");
		}
	}

	public void selectDateListenerSetup()
	{
		final Calendar cal = Calendar.getInstance();

		datePicker = new DatePickerDialog.OnDateSetListener()
		{
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
			{
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, monthOfYear);
				cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				todaysDate = formatter.format(cal.getTime());

				LogUtils.i(TAG, "New Date Selected: " + todaysDate);

				pageAdapter.clearViews(mViewPager);

				pageAdapter.notifyDataSetChanged();

				onResume();
			}
		};
	}

	private void openDateSelector()
	{
		try
		{
			Date d = formatter.parse(todaysDate);
			Calendar today = Calendar.getInstance();
			today.setTime(d);

			new DatePickerDialog(MainActivity.this, R.style.datepicker, datePicker, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();
		}
		catch (Exception e)
		{
			LogUtils.e(TAG, e.getMessage());
		}
	}

	private void startVPN()
	{
		Intent vpnIntent = VhostsService.prepare(this);
		if (vpnIntent != null)
		{
			startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
		}
		else
		{
			onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK)
		{
			startService(new Intent(this, VhostsService.class).setAction(VhostsService.ACTION_CONNECT));
		}
	}

	private void updateApp(final UpdateService updateService)
	{
		Log.i(TAG, updateService.getLatestVersion().getDownloadURL());

		try
		{
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateService.getLatestVersion().getDownloadURL()));
			request.setDescription(updateService.getLatestVersion().getUpdateMessage());
			request.setTitle(updateService.getLatestVersion().getVersionName());

			final Uri uri = Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Eon/" + updateService.getLatestVersion().getFileName());

			request.setDestinationUri(uri);

			final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			final long downloadID = manager.enqueue(request);

			BroadcastReceiver onComplete = new BroadcastReceiver()
			{
				@Override
				public void onReceive(Context context, Intent i)
				{
					File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Eon/", updateService.getLatestVersion().getFileName());
					Uri fileUri = Uri.fromFile(file);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
					{
						Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName(), file);
						Intent openFileIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(contentUri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(openFileIntent);
						unregisterReceiver(this);
						finish();
					}
					else
					{

						Intent openFileIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(openFileIntent);
						unregisterReceiver(this);
						finish();
					}
				}
			};

			registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		}
		catch (SecurityException e)
		{
			int permissionCheck = ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

			if (permissionCheck != PackageManager.PERMISSION_GRANTED)
			{
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Thread waitForUpdateServiceReady = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					while (!updateService.isReady())
					{
						Thread.sleep(100);
					}

					LogUtils.i(TAG, "Current Version: " + updateService.getCurrentVersion().getVersionNumber());


					if (updateService.isUpdateAvailable())
					{
						Snackbar notification = Snackbar.make(findViewById(R.id.main_content), "An update is available", Snackbar.LENGTH_INDEFINITE);
						notification.setAction(R.string.more_info, new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.Theme_AppCompat_Dialog)).setTitle(updateService.getLatestVersion().getUpdateMessage()).setMessage(updateService.getLatestVersion().getVersionName()).setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										Log.i(TAG, "Clicked Update");
										updateApp(updateService);
									}
								}).setNegativeButton(R.string.update_later, null).setIcon(android.R.drawable.ic_dialog_info).show();
							}
						});
						notification.show();
						LogUtils.i(TAG, "Update is available");
					}
					else
					{
						LogUtils.i(TAG, "No updates available");
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		waitForUpdateServiceReady.start();

		startVPN();

		selectDateListenerSetup();

		AsyncHTMLGetRequest getGameInfo = new AsyncHTMLGetRequest(new AsyncHTMLGetRequest.AsyncResponse()
		{
			@Override
			public void processFinish(String output)
			{
				parseGameInfo(output);
			}
		});

		String[] url = new String[1];
		url[0] = "http://statsapi.web.nhl.com/api/v1/schedule?teamId=&startDate=" + todaysDate + "&endDate=" + todaysDate + "&expand=schedule.teams,schedule.game.content.media.epg";

		getGameInfo.execute(url[0]);
	}

	private void parseGameInfo(String rawJSON)
	{
		try
		{
			JSONObject data = new JSONObject(rawJSON);
			JSONArray games = data.getJSONArray("dates").getJSONObject(0).getJSONArray("games");
			for (int i = 0; i < games.length(); i++)
			{
				JSONObject game = games.getJSONObject(i);
				JSONObject content = game.getJSONObject("content");

				if (!content.has("media"))
				{
					Log.e(TAG, "Content for game does not have media");
					continue;
				}

				HashMap<String, String> mediaPlaybackIds = new HashMap<>();

				JSONArray medias = game.getJSONObject("content").getJSONObject("media").getJSONArray("epg");
				for (int x = 0; x < medias.length(); x++)
				{
					JSONObject media = medias.getJSONObject(x);

					if (media.getString("title").equalsIgnoreCase("NHLTV"))
					{
						JSONArray items = media.getJSONArray("items");
						for (int y = 0; y < items.length(); y++)
						{
							JSONObject item = items.getJSONObject(y);

							String callName = item.getString("callLetters");

							if (callName.equalsIgnoreCase(""))
							{
								callName = "Goalie Cams";
							}

							mediaPlaybackIds.put(callName, item.getString("mediaPlaybackId"));
						}
					}
				}

				String homeTeam = game.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getString("abbreviation");
				String awayTeam = game.getJSONObject("teams").getJSONObject("away").getJSONObject("team").getString("abbreviation");
				String awayScore = game.getJSONObject("teams").getJSONObject("away").getString("score");
				String homeScore = game.getJSONObject("teams").getJSONObject("home").getString("score");
				String status = game.getJSONObject("status").getString("detailedState");
				String awayWins = game.getJSONObject("teams").getJSONObject("away").getJSONObject("leagueRecord").getString("wins");
				String awayLosses = game.getJSONObject("teams").getJSONObject("away").getJSONObject("leagueRecord").getString("losses");
				String homeWins = game.getJSONObject("teams").getJSONObject("home").getJSONObject("leagueRecord").getString("wins");
				String homeLosses = game.getJSONObject("teams").getJSONObject("home").getJSONObject("leagueRecord").getString("losses");


				addPage(homeTeam, awayTeam, homeScore, awayScore, status, mediaPlaybackIds, awayWins, awayLosses, homeWins, homeLosses);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private void addPage(String homeTeam, String awayTeam, String homeScore, String awayScore, String status, final HashMap<String, String> mediaPlaybackIds, String homeWins, String homeLosses, String awayWins, String awayLosses)
	{
		FrameLayout page = (FrameLayout) getLayoutInflater().inflate(R.layout.fragment_main, null);

		final Spinner sourceSelector = page.findViewById(R.id.streamSource);

		List<String> sources = new ArrayList<>();
		Set<String> keys = mediaPlaybackIds.keySet();

		for (String key : keys)
		{
			if (!status.equalsIgnoreCase("Scheduled"))
			{
				sources.add(key);
			}
		}

		ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(getBaseContext(), R.layout.support_simple_spinner_dropdown_item, sources);
		sourceAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		sourceSelector.setAdapter(sourceAdapter);

		ImageView awayLogo = page.findViewById(R.id.awayLogo);
		ImageView homeLogo = page.findViewById(R.id.homeLogo);

		awayLogo.setImageResource(getResources().getIdentifier(awayTeam.toLowerCase(), "drawable", getPackageName()));
		homeLogo.setImageResource(getResources().getIdentifier(homeTeam.toLowerCase(), "drawable", getPackageName()));

		TextView gameLabel = page.findViewById(R.id.gameTitle);
		gameLabel.setText(awayTeam + " @ " + homeTeam);

		TextView statusLabel = page.findViewById(R.id.gameStatus);
		statusLabel.setText(status);

		TextView homeScoreLabel = page.findViewById(R.id.homeScore);
		homeScoreLabel.setText(homeScore);

		TextView awayScoreLabel = page.findViewById(R.id.awayScore);
		awayScoreLabel.setText(awayScore);

		TextView homeRecord = page.findViewById(R.id.homeRecord);
		homeRecord.setText(homeWins + "-" + homeLosses);

		TextView awayRecord = page.findViewById(R.id.awayRecord);
		awayRecord.setText(awayWins + "-" + awayLosses);

		Button watchBtn = page.findViewById(R.id.watchButton);
		watchBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				String selectedItem = sourceSelector.getSelectedItem().toString();
				String mediaplaybackid = mediaPlaybackIds.get(selectedItem);

				String[] url = new String[1];
				url[0] = "http://freesports.ddns.net/getM3U8.php?league=NHL&id=" + mediaplaybackid + "&cdn=akc&date=" + todaysDate;

				startVPN();

				AsyncHTMLGetRequest getM3U8StreamUrl = new AsyncHTMLGetRequest(new AsyncHTMLGetRequest.AsyncResponse()
				{
					@Override
					public void processFinish(String output)
					{
						Bundle b = new Bundle();
						b.putString("streamUrl", output);

						Intent intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
						intent.putExtras(b);
						startActivity(intent);
					}
				});

				getM3U8StreamUrl.execute(url);
			}
		});

		pageAdapter.addView(page);
		pageAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_pick_date)
		{
			openDateSelector();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static class GameFragment extends Fragment
	{
		public GameFragment() { }

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
}
