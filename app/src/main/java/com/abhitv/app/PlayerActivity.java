package com.abhitv.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.mp4.Mp4Extractor;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    public static final String EXTRA_TITLE = "com.abhitv.app.PLAYER_TITLE";
    public static final String EXTRA_URL = "com.abhitv.app.PLAYER_URL";

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView durationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "Valid video URL nahi mili", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setKeepScreenOn(true);
        frame.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        durationStatus = new TextView(this);
        durationStatus.setTextColor(Color.WHITE);
        durationStatus.setTextSize(12);
        durationStatus.setPadding(10, 6, 10, 6);
        durationStatus.setBackgroundColor(0x99000000);
        durationStatus.setText("Duration load ho rahi hai…");
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM | android.view.Gravity.END);
        statusParams.setMargins(0, 0, 18, 76);
        frame.addView(durationStatus, statusParams);

        TextView back = new TextView(this);
        back.setText("✕");
        back.setTextColor(Color.WHITE);
        back.setTextSize(22);
        back.setGravity(android.view.Gravity.CENTER);
        back.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(52, 52,
                android.view.Gravity.TOP | android.view.Gravity.START);
        frame.addView(back, backParams);
        back.setOnClickListener(v -> finish());
        setContentView(frame);

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("ABHI-TV/1.0 (Android; Media3)")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(20000)
                .setReadTimeoutMs(30000);
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory)
                        .setDataSourceFactory(httpFactory))
                .build();
        playerView.setPlayer(player);

        MediaItem.Builder itemBuilder = new MediaItem.Builder().setUri(Uri.parse(url));
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains(".m3u8")) {
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        } else if (lower.contains(".mp4")) {
            itemBuilder.setMimeType(MimeTypes.VIDEO_MP4);
        }
        if (title != null && !title.trim().isEmpty()) {
            playerView.setContentDescription(title);
        }

        player.addListener(new Player.Listener() {
            @Override
            public void onTimelineChanged(androidx.media3.common.Timeline timeline, int reason) {
                updateDurationStatus();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updateDurationStatus();
            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                updateDurationStatus();
            }

            @Override
            public void onEvents(Player player, Player.Events events) {
                updateDurationStatus();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                String detail = "Playback error (code " + error.errorCode + ")";
                Throwable cause = error.getCause();
                if (cause != null && cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
                    detail += ": " + cause.getMessage();
                }
                durationStatus.setText("Playback failed: " + detail);
                android.util.Log.e("ABHI-TV-PLAYER", "Playback failed for URL: " + url, error);
                Toast.makeText(PlayerActivity.this,
                        "Video error: " + detail,
                        Toast.LENGTH_LONG).show();
            }
        });
        player.setMediaItem(itemBuilder.build());
        player.prepare();
        player.play();
    }

    private void updateDurationStatus() {
        if (player == null || durationStatus == null) return;
        long durationMs = player.getDuration();
        if (durationMs == androidx.media3.common.C.TIME_UNSET || durationMs < 0) {
            durationStatus.setText(player.getPlaybackState() == Player.STATE_BUFFERING
                    ? "Duration load ho rahi hai…" : "Duration source se nahi mili");
            return;
        }
        long totalSeconds = durationMs / 1000;
        durationStatus.setText(String.format(java.util.Locale.ROOT, "%02d:%02d",
                totalSeconds / 60, totalSeconds % 60));
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private void releasePlayer() {
        if (player != null) {
            if (playerView != null) playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
