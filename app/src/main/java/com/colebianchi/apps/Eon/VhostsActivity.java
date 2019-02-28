package com.colebianchi.apps.Eon;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.colebianchi.apps.Eon.adapters.GameCard;
import com.colebianchi.apps.Eon.adapters.RVAdapter;
import com.colebianchi.apps.Eon.listeners.RecyclerViewClickListener;
import com.colebianchi.apps.Eon.util.LogUtils;
import com.colebianchi.apps.Eon.vservice.VhostsService;
import com.github.xfalcon.vhosts.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class VhostsActivity extends AppCompatActivity implements GestureDetector.OnGestureListener
{

	private static final String TAG = VhostsActivity.class.getSimpleName();
	private static final int VPN_REQUEST_CODE = 0x0F;
	private static final int SELECT_FILE_CODE = 0x05;
	public static final String PREFS_NAME = VhostsActivity.class.getName();
	public static final String IS_LOCAL = "IS_LOCAL";
	public static final String HOSTS_URL = "HOSTS_URL";
	public static final String HOSTS_URI = "HOST_URI";
	public static final String NET_HOST_FILE = "net_hosts";

	private boolean waitingForVPNStart;

	//NHL Stuff
	private List<GameCard> gameCards;
	private RecyclerView rv;
	private RVAdapter adapter;

	private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (VhostsService.BROADCAST_VPN_STATE.equals(intent.getAction()))
			{
				if (intent.getBooleanExtra("running", false))
				{
					waitingForVPNStart = false;
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		launch();
		StatService.autoTrace(this, true, false);
		setContentView(R.layout.activity_vhosts);
		LogUtils.context = getApplicationContext();

		RecyclerView recyclerView = findViewById(R.id.rv);
		final GestureDetector gestureDetector = new GestureDetector(this);

		recyclerView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent)
			{
				return gestureDetector.onTouchEvent(motionEvent);
			}
		});

		SwitchCompat vpnButton = findViewById(R.id.button_start_vpn);

		vpnButton.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked)
			{
				if (isChecked)
				{
					if (checkHostUri() == -1)
					{
						showDialog();
					}
					else
					{
						startVPN();
					}
				}
				else
				{
					shutdownVPN();
				}
			}
		});

		LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
				new IntentFilter(VhostsService.BROADCAST_VPN_STATE));

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy);

		try
		{
			TrustManager[] victimizedManager = new TrustManager[]{

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
					}
			};

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
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.main, menu);

		final MenuItem toggleservice = menu.findItem(R.id.menuSwitch);
		SwitchCompat actionView = (SwitchCompat) MenuItemCompat.getActionView(toggleservice);

		actionView.setChecked(VhostsService.isRunning());

		actionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					setButton(false);
				}
				else
				{
					setButton(true);
				}
			}
		});

		return true;
	}

	private void launch()
	{
		Uri uri = getIntent().getData();
		if (uri == null)
		{
			return;
		}
		String data_str = uri.toString();
		if ("on".equals(data_str))
		{
			if (!VhostsService.isRunning())
			{
				VhostsService.startVService(this, 1);
			}
			finish();
		}
		else if ("off".equals(data_str))
		{
			VhostsService.stopVService(this);
			finish();
		}
	}

	private void selectFile()
	{
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType("*/*");
		try
		{
			String SHOW_ADVANCED;
			try
			{
				Field f = android.provider.DocumentsContract.class.getField("EXTRA_SHOW_ADVANCED");
				SHOW_ADVANCED = f.get(f.getName()).toString();
			} catch (NoSuchFieldException e)
			{
				LogUtils.e(TAG, e.getMessage(), e);
				SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
			}
			intent.putExtra(SHOW_ADVANCED, true);
		} catch (Throwable e)
		{
			LogUtils.e(TAG, "SET EXTRA_SHOW_ADVANCED", e);
		}

	}

	private void startVPN()
	{
		waitingForVPNStart = false;
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

	private int checkHostUri()
	{
		/*SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		if (settings.getBoolean(VhostsActivity.IS_LOCAL, true))
		{
			try
			{
				getContentResolver().openInputStream(Uri.parse(settings.getString(HOSTS_URI, null))).close();
				return 1;
			} catch (Exception e)
			{
				LogUtils.e(TAG, "HOSTS FILE NOT FOUND", e);
				return -1;
			}
		}
		else
		{
			try
			{
				openFileInput(VhostsActivity.NET_HOST_FILE).close();
				return 2;
			} catch (Exception e)
			{
				LogUtils.e(TAG, "NET HOSTS FILE NOT FOUND", e);
				return -2;
			}
		}*/

		return 1;
	}

	private void setUriByPREFS(Intent intent)
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		Uri uri = intent.getData();
		final int takeFlags = intent.getFlags()
				& (Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		try
		{
			getContentResolver().takePersistableUriPermission(uri, takeFlags);
			editor.putString(HOSTS_URI, uri.toString());
			editor.apply();
			if (checkHostUri() == 1)
			{
				setButton(true);
				setButton(false);
			}
			else
			{
				Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show();
			}

		} catch (Exception e)
		{
			LogUtils.e(TAG, "permission error", e);
		}

	}

	private void shutdownVPN()
	{
		if (VhostsService.isRunning())
		{
			startService(new Intent(this, VhostsService.class).setAction(VhostsService.ACTION_DISCONNECT));
		}
		setButton(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK)
		{
			waitingForVPNStart = true;
			startService(new Intent(this, VhostsService.class).setAction(VhostsService.ACTION_CONNECT));
			setButton(false);
		}
		else if (requestCode == SELECT_FILE_CODE && resultCode == RESULT_OK)
		{
			setUriByPREFS(data);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		setButton(!waitingForVPNStart && !VhostsService.isRunning());

		rv = findViewById(R.id.rv);

		LinearLayoutManager llm = new LinearLayoutManager(getBaseContext());
		rv.setLayoutManager(llm);
		rv.setHasFixedSize(true);

		gameCards = new ArrayList<>();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String todaysDate = formatter.format(date);
		String url = "http://statsapi.web.nhl.com/api/v1/schedule?teamId=&startDate=" + todaysDate + "&endDate=" + todaysDate + "&expand=schedule.teams,schedule.game.content.media.epg";

		String[] params = new String[1];
		params[0] = url;

		GetHTMLAsync getHTMLAsync = new GetHTMLAsync(new GetHTMLAsync.AsyncResponse()
		{
			@Override
			public void processFinish(String output)
			{
				displayCards(output);
			}
		});

		getHTMLAsync.execute(params);
	}

	private void displayCards(String rawJSON)
	{
		try
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			String todaysDate = formatter.format(date);
			//String rawJSON = getHTML(url);


			JSONObject data = new JSONObject(rawJSON);
			JSONArray games = data.getJSONArray("dates").getJSONObject(0).getJSONArray("games");
			for (int i = 0; i < games.length(); i++)
			{
				JSONObject game = games.getJSONObject(i);

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

							String streamURL = "http://freesports.ddns.net/getM3U8.php?league=NHL&id=" + item.getString("mediaPlaybackId") + "&cdn=akc&date=" + todaysDate;

							//streamURL = getHTML(streamURL);

							GameCardContainer[] params = new GameCardContainer[1];
							params[0] = new GameCardContainer(streamURL, game, item);

							GetGameCardAsync getGameCardAsyncAsync = new GetGameCardAsync(new GetGameCardAsync.AsyncResponse()
							{
								@Override
								public void processFinish(GameCardContainer output)
								{
									addCard(output);
								}
							});

							getGameCardAsyncAsync.execute(params);
						}
					}
				}
			}
		} catch (Exception e)
		{
			popup(e.getMessage(), "Error");
			LogUtils.e(TAG, e.getMessage(), e);
		}
	}

	private void addCard(GameCardContainer card)
	{
		JSONObject game = card.game;
		JSONObject item = card.item;
		String data = card.data;

		try
		{
			int imgID = getResources().getIdentifier(game.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getString("abbreviation").toLowerCase(), "drawable", getPackageName());
			String homeAbv = game.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getString("abbreviation");
			String awayAbv = game.getJSONObject("teams").getJSONObject("away").getJSONObject("team").getString("abbreviation");
			String callName = item.getString("callLetters");

			if (!data.contains("Not available yet"))
			{
				gameCards.add(new GameCard( awayAbv + " @ " + homeAbv, callName, imgID, data));
			}
			else
			{
				LogUtils.i(TAG, "Not including stream link: " + data);
			}

			RecyclerViewClickListener listener = new RecyclerViewClickListener()
			{
				@Override
				public void onClick(View view, int position)
				{
					GameCard card = adapter.gameCards.get(position);
					String streamURL = card.streamURL;

					LogUtils.i(TAG, streamURL);

					Intent intent = new Intent(getBaseContext(), PlayerActivity.class);

					Bundle b = new Bundle();
					b.putString("streamURL", streamURL);
					intent.putExtras(b);
					startActivity(intent);
				}
			};


			adapter = new RVAdapter(gameCards, getLayoutInflater(), listener);
			rv.setAdapter(adapter);
		}
		catch (Exception e)
		{
			LogUtils.e(TAG, e.getMessage());
		}
	}

	private void popup(String msg, String title)
	{
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(msg)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
					}
				})
				.setNegativeButton(android.R.string.no, null)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

	private void setButton(boolean enable)
	{
		final SwitchCompat vpnButton = findViewById(R.id.button_start_vpn);
		if (enable)
		{
			vpnButton.setChecked(false);
		}
		else
		{
			vpnButton.setChecked(true);
		}
	}

	private void showDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setTitle(R.string.dialog_title);
		builder.setMessage(R.string.dialog_message);
		builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				selectFile();
			}
		});

		builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				setButton(true);
			}
		});
		builder.show();
	}

	@Override
	public boolean onDown(MotionEvent motionEvent)
	{
		return false;
	}

	@Override
	public void onShowPress(MotionEvent motionEvent)
	{

	}

	@Override
	public boolean onSingleTapUp(MotionEvent motionEvent)
	{
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1)
	{
		return false;
	}

	@Override
	public void onLongPress(MotionEvent motionEvent)
	{

	}

	@Override
	public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY)
	{
		int SWIPE_MIN_DISTANCE = 120;
		int SWIPE_MAX_OFF_PATH = 250;
		int SWIPE_THRESHOLD_VELOCITY = 200;

		/*//Bad Swipe
		if (Math.abs(motionEvent.getX() - motionEvent1.getX()) > SWIPE_MAX_OFF_PATH)
		{
			return false;
		}

		//Swipe Down
		if (Math.abs(motionEvent.getY() - motionEvent1.getY()) > SWIPE_MIN_DISTANCE && Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY)
		{
			LogUtils.i(TAG, "Fling down");
		}
		//Swipe Up
		else if (Math.abs(motionEvent1.getY() - motionEvent.getY()) > SWIPE_MIN_DISTANCE && Math.abs(vY) > SWIPE_THRESHOLD_VELOCITY)
		{
			LogUtils.i(TAG, "Fling up");
		}*/

		return false;
	}
}
