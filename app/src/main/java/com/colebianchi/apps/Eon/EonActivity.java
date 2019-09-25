package com.colebianchi.apps.Eon;

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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.BaseInputConnection;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Toast;
import com.baidu.mobstat.StatService;
import com.colebianchi.apps.Eon.adapters.GameCard;
import com.colebianchi.apps.Eon.adapters.RVAdapter;
import com.colebianchi.apps.Eon.listeners.RecyclerViewClickListener;
import com.colebianchi.apps.Eon.util.LogUtils;
import com.colebianchi.apps.Eon.vservice.VhostsService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.File;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EonActivity extends AppCompatActivity implements GestureDetector.OnGestureListener
{

	private static final String TAG = EonActivity.class.getSimpleName();
	private static final int VPN_REQUEST_CODE = 0x0F;
	private static final int SELECT_FILE_CODE = 0x05;
	public static final String PREFS_NAME = EonActivity.class.getName();
	public static final String IS_LOCAL = "IS_LOCAL";
	public static final String HOSTS_URI = "HOST_URI";
	public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
	UpdateService updateService;

	private boolean waitingForVPNStart;

	//NHL Stuff
	private List<GameCard> gameCards;
	private RecyclerView rv;
	private RVAdapter adapter;
	private String todaysDate;
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private MenuItem datePickerMenuItem;
	private int cursorPosition = 0;
	private DatePickerDialog.OnDateSetListener datePicker;
	private String rawJSON;

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

		LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver, new IntentFilter(VhostsService.BROADCAST_VPN_STATE));

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
			e.printStackTrace();
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

				onResume();
			}
		};

		datePickerMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem m)
			{
				openDateSelector();

				return true;
			}
		});
	}

	private void openDateSelector()
	{
		try
		{
			Date d = formatter.parse(todaysDate);
			Calendar today = Calendar.getInstance();
			today.setTime(d);

			new DatePickerDialog(EonActivity.this, R.style.datepicker, datePicker, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();
		}
		catch (Exception e)
		{
			LogUtils.e(TAG, e.getMessage());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.main, menu);

		datePickerMenuItem = menu.findItem(R.id.action_pick_date);

		selectDateListenerSetup();

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
			}
			catch (NoSuchFieldException e)
			{
				LogUtils.e(TAG, e.getMessage(), e);
				SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
			}
			intent.putExtra(SHOW_ADVANCED, true);
		}
		catch (Throwable e)
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
		return 1;
	}

	private void setUriByPREFS(Intent intent)
	{
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		Uri uri = intent.getData();
		final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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

		}
		catch (Exception e)
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
						Snackbar notification = Snackbar.make(findViewById(R.id.activity_main), "An update is available", Snackbar.LENGTH_INDEFINITE);
						notification.setAction(R.string.more_info, new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								new AlertDialog.Builder(new ContextThemeWrapper(EonActivity.this, R.style.Theme_AppCompat_Dialog)).setTitle(updateService.getLatestVersion().getUpdateMessage()).setMessage(updateService.getLatestVersion().getVersionName()).setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog, int which)
									{
										updateApp(updateService);
									}
								})

										.setNegativeButton(R.string.update_later, null).setIcon(android.R.drawable.ic_dialog_info).show();
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

		rv = findViewById(R.id.rv);

		LinearLayoutManager llm = new LinearLayoutManager(getBaseContext());
		rv.setLayoutManager(llm);
		rv.setHasFixedSize(true);

		gameCards = new ArrayList<>();

		String url = "http://statsapi.web.nhl.com/api/v1/schedule?teamId=&startDate=" + todaysDate + "&endDate=" + todaysDate + "&expand=schedule.teams,schedule.game.content.media.epg";

		String[] params = new String[1];
		params[0] = url;

		GetHTMLAsync getHTMLAsync = new GetHTMLAsync(new GetHTMLAsync.AsyncResponse()
		{
			@Override
			public void processFinish(String output)
			{
				rawJSON = output;
				displayCards(-1);
			}
		});

		getHTMLAsync.execute(params);
	}

	private void updateApp(final UpdateService updateService)
	{
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
				ActivityCompat.requestPermissions(EonActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch (requestCode)
		{
			case PERMISSION_WRITE_EXTERNAL_STORAGE:
				if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
				{
					updateApp(updateService);
				}
				break;

			default:
				break;
		}
	}

	private void displayCards(int focused)
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
					LogUtils.e(TAG, "Content for game does not have media");
					continue;
				}

				LogUtils.i(TAG, "Found media");

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
		}
		catch (Exception e)
		{
			//popup(e.getMessage(), "Error");
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

			if (callName.equalsIgnoreCase(""))
			{
				callName = "Goalie Cams";
			}

			if (!data.contains("Not available yet"))
			{
				gameCards.add(new GameCard(awayAbv + " @ " + homeAbv, callName, imgID, data));
			}
			else
			{
				gameCards.add(new GameCard(awayAbv + " @ " + homeAbv, "Game unavailable", imgID, data));
			}

			RecyclerViewClickListener listener = new RecyclerViewClickListener()
			{
				@Override
				public void onClick(View view, int position)
				{
					openStream(position);
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

	private void openStream(int position)
	{
		setButton(false);

		GameCard card = adapter.gameCards.get(position);

		if (!card.time.contains("Game unavailable"))
		{
			String streamURL = card.streamURL;

			LogUtils.i(TAG, streamURL);

			Intent intent = new Intent(getBaseContext(), PlayerActivity.class);

			Bundle b = new Bundle();
			b.putString("streamURL", streamURL);
			intent.putExtras(b);
			startActivity(intent);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		int direction = 1;

		switch (keyCode)
		{
			case KeyEvent.KEYCODE_DPAD_CENTER:
			//case KeyEvent.KEYCODE_Q:
					if (cursorPosition != -1)
					{
						openStream(cursorPosition);
						direction = 1;
					}
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
			//case KeyEvent.KEYCODE_W:
					if (cursorPosition != -1)
					{
						cursorPosition--;
						direction = -1;
					}
					else if (cursorPosition == -1)
					{
						openDateSelector();
					}
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
			//case KeyEvent.KEYCODE_S:
					if (cursorPosition != adapter.getItemCount()-1)
					{
						cursorPosition++;
					}
				break;
			default: break;
		}

		if (cursorPosition == -1)
		{
			datePickerMenuItem.setChecked(true);
		}
		else
		{
			rv.scrollToPosition(cursorPosition+direction);

			rv.findViewHolderForAdapterPosition(cursorPosition).itemView.requestFocus();
		}

		LogUtils.i(TAG, cursorPosition+" selected KEYCODE: "+keyCode);

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		//shutdownVPN();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		shutdownVPN();
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
