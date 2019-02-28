package com.colebianchi.apps.Eon;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.github.xfalcon.vhosts.R;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;



/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity implements ExoPlayer.EventListener
{
	private SimpleExoPlayer player;
	private ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.video_player);

		Bundle b = getIntent().getExtras();
		String streamURL = b.getString("streamURL");


		//Create handler
		Handler mainHandler = new Handler();
		BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
		TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);

		TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

		//Load control
		LoadControl loadControl = new DefaultLoadControl();

		//Create player
		player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

		SimpleExoPlayerView simpleExoPlayerView = findViewById(R.id.player_view);
		simpleExoPlayerView.setPlayer(player);

		progressBar = findViewById(R.id.progressBar);

		//Data Source instance stuff
		DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Exo2"), (DefaultBandwidthMeter)bandwidthMeter);

		//Media Source
		HlsMediaSource hlsMediaSource = new HlsMediaSource(Uri.parse(streamURL), dataSourceFactory, mainHandler, new AdaptiveMediaSourceEventListener()
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
	public void onSeekProcessed()
	{

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters p)
	{

	}

	@Override
	public void onPositionDiscontinuity(int i)
	{

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean b)
	{

	}

	@Override
	public void onRepeatModeChanged(int num)
	{

	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int num) {

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

		switch (playbackState) {
			case Player.STATE_BUFFERING:
				//You can use progress dialog to show user that video is preparing or buffering so please wait
				progressBar.setVisibility(View.VISIBLE);
				break;
			case Player.STATE_IDLE:
				//idle state
				break;
			case Player.STATE_READY:
				// dismiss your dialog here because our video is ready to play now
				progressBar.setVisibility(View.GONE);
				break;
			case Player.STATE_ENDED:
				// do your processing after ending of video
				break;
		}
	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {

		AlertDialog.Builder adb = new AlertDialog.Builder(PlayerActivity.this);
		adb.setTitle("Could not able to stream video");
		adb.setMessage("It seems that something is going wrong.\nPlease try again.");
		adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish(); // take out user from this activity. you can skip this
			}
		});
		AlertDialog ad = adb.create();
		ad.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (player != null) {
			player.setPlayWhenReady(false); //to pause a video because now our video player is not in focus
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		player.release();
	}
}