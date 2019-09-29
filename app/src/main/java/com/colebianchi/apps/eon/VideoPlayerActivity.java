package com.colebianchi.apps.eon;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoPlayerActivity extends AppCompatActivity implements ExoPlayer.EventListener
{
	private String TAG = this.getClass().getName();
	private SimpleExoPlayer player;
	private ProgressBar progressBar;
	private String streamURL;

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * Some older devices needs a small delay between UI widget updates
	 * and a change of the status and navigation bar.
	 */
	private static final int UI_ANIMATION_DELAY = 300;
	private final Handler mHideHandler = new Handler();
	private View mContentView;
	private final Runnable mHidePart2Runnable = new Runnable()
	{
		@SuppressLint("InlinedApi")
		@Override
		public void run()
		{
			// Delayed removal of status and navigation bar

			// Note that some of these constants are new as of API 16 (Jelly Bean)
			// and API 19 (KitKat). It is safe to use them, as they are inlined
			// at compile-time and do nothing on earlier devices.
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
	};
	private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable()
	{
		@Override
		public void run()
		{
			// Delayed display of UI elements
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null)
			{
				actionBar.show();
			}
			mControlsView.setVisibility(View.VISIBLE);
		}
	};
	private boolean mVisible;
	private final Runnable mHideRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			hide();
		}
	};
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener()
	{
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent)
		{
			if (AUTO_HIDE)
			{
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_video_player);

		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = findViewById(R.id.fullscreen_content);


		// Set up the user interaction to manually show or hide the system UI.
		mContentView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				toggle();
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.

		Bundle b = getIntent().getExtras();
		streamURL = b.getString("streamUrl");

		Log.i(TAG, streamURL);

		Handler mainHandler = new Handler();
		BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);

		TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

		//Load control
		LoadControl loadControl = new DefaultLoadControl();

		//Create player
		player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

		SimpleExoPlayerView simpleExoPlayerView = findViewById(R.id.fullscreen_content);
		simpleExoPlayerView.setPlayer(player);

		progressBar = findViewById(R.id.progressBar);

		//Data Source instance stuff
		DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Exo2"), (DefaultBandwidthMeter) bandwidthMeter);

		//Media Source
		final HlsMediaSource hlsMediaSource = new HlsMediaSource(Uri.parse(streamURL), dataSourceFactory, mainHandler, new AdaptiveMediaSourceEventListener()
		{
			@Override
			public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId)
			{

			}

			@Override
			public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId)
			{

			}

			@Override
			public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData)
			{

			}

			@Override
			public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData)
			{

			}

			@Override
			public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData)
			{

			}

			@Override
			public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled)
			{

			}

			@Override
			public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId)
			{

			}

			@Override
			public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData)
			{

			}

			@Override
			public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData)
			{

			}
		});

		player.addListener(this);
		player.prepare(hlsMediaSource);
		simpleExoPlayerView.requestFocus();
		player.setPlayWhenReady(true);
	}

	@Override
	public void onBackPressed()
	{
		player.stop();
		finish();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	private void toggle()
	{
		if (mVisible)
		{
			hide();
		}
		else
		{
			show();
		}
	}

	private void hide()
	{
		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
		{
			actionBar.hide();
		}
		mControlsView.setVisibility(View.GONE);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}

	@SuppressLint("InlinedApi")
	private void show()
	{
		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
	}

	/**
	 * Schedules a call to hide() in delay milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis)
	{
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason)
	{

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections)
	{

	}

	@Override
	public void onLoadingChanged(boolean isLoading)
	{

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
	{

	}

	@Override
	public void onRepeatModeChanged(int repeatMode)
	{

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled)
	{

	}

	@Override
	public void onPlayerError(ExoPlaybackException error)
	{

	}

	@Override
	public void onPositionDiscontinuity(int reason)
	{

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters)
	{

	}

	@Override
	public void onSeekProcessed()
	{

	}
}
