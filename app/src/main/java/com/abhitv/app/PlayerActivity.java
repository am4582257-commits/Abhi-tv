package com.abhitv.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
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
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    public static final String EXTRA_TITLE = "com.abhitv.app.PLAYER_TITLE";
    public static final String EXTRA_URL = "com.abhitv.app.PLAYER_URL";

    private ExoPlayer player;
    private PlayerView playerView;

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

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem.Builder itemBuilder = new MediaItem.Builder().setUri(Uri.parse(url));
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains(".m3u8")) {
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        if (title != null && !title.trim().isEmpty()) {
            playerView.setContentDescription(title);
        }
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Video play nahi hua. Link/format ya server access check karo.",
                        Toast.LENGTH_LONG).show();
            }
        });
        player.setMediaItem(itemBuilder.build());
        player.prepare();
        player.play();
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
