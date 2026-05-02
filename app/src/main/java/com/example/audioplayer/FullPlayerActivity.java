package com.example.audioplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.services.AudioPlayerService;
import com.example.audioplayer.utils.PlaybackManager;
import java.io.File;

public class FullPlayerActivity extends AppCompatActivity {

    private ImageView ivBackground, ivTrackCover;
    private TextView tvSongTitle, tvSongArtist, tvCurrentTime, tvTotalTime;
    private TextView tabSong, tabText;
    private ImageButton btnBack, btnPlayPause, btnNext, btnPrev;
    private SeekBar seekBar;
    private View songContent, lyricsContent;

    private boolean isUserDragging = false;

    private final AudioPlayerService.OnPlaybackListener playbackListener = new AudioPlayerService.OnPlaybackListener() {
        @Override public void onPlaybackStateChanged(boolean isPlaying) { runOnUiThread(() -> updatePlayPauseIcon(isPlaying)); }
        @Override public void onProgressUpdated(int currentPosition, int duration) { runOnUiThread(() -> updateProgress(currentPosition, duration)); }
        @Override public void onTrackChanged(AudioTrack track) { runOnUiThread(() -> loadTrack(track)); }
        @Override public void onTrackCompleted() {}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        initViews();
        setupClickListeners();
        setupTabs();
        setupSeekBar();

        PlaybackManager pm = PlaybackManager.getInstance();
        pm.addUiListener(playbackListener);

        AudioTrack current = pm.getCurrentTrack();
        if (current != null) loadTrack(current);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncPlaybackState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackManager.getInstance().removeUiListener(playbackListener);
    }

    private void initViews() {
        ivBackground = findViewById(R.id.ivBackground);
        ivTrackCover = findViewById(R.id.ivTrackCover);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tabSong = findViewById(R.id.tabSong);
        tabText = findViewById(R.id.tabText);
        btnBack = findViewById(R.id.btnBack);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);
        songContent = findViewById(R.id.songContent);
        lyricsContent = findViewById(R.id.lyricsContent);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finishWithAnimation());
        btnPlayPause.setOnClickListener(v -> PlaybackManager.getInstance().togglePlayback());
        btnNext.setOnClickListener(v -> PlaybackManager.getInstance().playNext());
        btnPrev.setOnClickListener(v -> PlaybackManager.getInstance().playPrevious());
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserDragging = false;
                PlaybackManager pm = PlaybackManager.getInstance();
                if (pm.isReady() && pm.getService() != null) {
                    pm.getService().seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void setupTabs() {
        tabSong.setOnClickListener(v -> switchTab(true));
        tabText.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean isSongTab) {
        if (isSongTab) {
            tabSong.setTextColor(0xFFFFFFFF); tabSong.setTypeface(null, android.graphics.Typeface.BOLD);
            tabText.setTextColor(0xFFAAAAAA); tabText.setTypeface(null, android.graphics.Typeface.NORMAL);
            songContent.setVisibility(View.VISIBLE); lyricsContent.setVisibility(View.GONE);
        } else {
            tabText.setTextColor(0xFFFFFFFF); tabText.setTypeface(null, android.graphics.Typeface.BOLD);
            tabSong.setTextColor(0xFFAAAAAA); tabSong.setTypeface(null, android.graphics.Typeface.NORMAL);
            songContent.setVisibility(View.GONE); lyricsContent.setVisibility(View.VISIBLE);
        }
    }

    private void loadTrack(AudioTrack track) {
        if (track == null) return;
        tvSongTitle.setText(track.getTitle());
        tvSongArtist.setText(track.getArtist());

        String artFile = track.getAlbumArtFileName();
        if (artFile != null && !artFile.isEmpty()) {
            File f = new File(getFilesDir(), artFile);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    ivTrackCover.setImageBitmap(bmp);
                    applyBlurEffect(bmp);
                    syncPlaybackState();
                    return;
                }
            }
        }
        ivTrackCover.setImageResource(R.drawable.ic_placeholder_cover);
        applyBlurEffect(null);
        syncPlaybackState();
    }

    private void syncPlaybackState() {
        PlaybackManager pm = PlaybackManager.getInstance();
        if (!pm.isReady() || pm.getService() == null) return;

        AudioPlayerService service = pm.getService();
        int duration = service.getDuration();
        int currentPos = service.getCurrentPosition();

        tvTotalTime.setText(formatTime(duration));
        tvCurrentTime.setText(formatTime(currentPos));
        if (duration > 0) {
            seekBar.setMax(duration);
            if (!isUserDragging) seekBar.setProgress(currentPos);
        }
        updatePlayPauseIcon(pm.isPlaying());
    }

    private void updateProgress(int current, int duration) {
        if (duration > 0) {
            tvCurrentTime.setText(formatTime(current));
            tvTotalTime.setText(formatTime(duration));
            if (!isUserDragging) {
                seekBar.setMax(duration);
                seekBar.setProgress(current);
            }
        }
    }

    private void applyBlurEffect(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        ivBackground.setImageBitmap(bitmap);
        if (bitmap != null) {
            RenderEffect blur = RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP);
            ivBackground.setRenderEffect(blur);
        } else {
            ivBackground.setRenderEffect(null);
        }
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private String formatTime(int ms) {
        int sec = (ms / 1000) % 60;
        int min = (ms / 1000) / 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(0, R.anim.slide_down);
    }

}