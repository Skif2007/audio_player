package com.example.audioplayer.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.audioplayer.R;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.services.AudioPlayerService;

import java.io.File;

public class MiniPlayerPanel extends FrameLayout {

    private static final String TAG = "MiniPlayerPanel";


    private ImageView ivCover, ivPlayPauseIcon;
    private TextView tvTitle, tvArtist;

    private com.example.audioplayer.view.CircularProgressButton btnProgress;
    private View root;

    private AudioTrack currentTrack;
    private boolean isBoundToService = false;
    private OnMiniPlayerListener externalListener;

    public void setOnMiniPlayerListener(OnMiniPlayerListener listener) {
        this.externalListener = listener;
    }

    private OnMiniPlayerClickListener clickListener;

    public interface OnMiniPlayerClickListener {
        void onMiniPlayerClicked(AudioTrack track);
    }

    public interface OnMiniPlayerListener {
        void onTrackClicked(AudioTrack track);
        void onTrackChanged(AudioTrack track);
        void onTrackCompleted();
    }


    public MiniPlayerPanel(@NonNull Context context) {
        this(context, null);
    }

    public MiniPlayerPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiniPlayerPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        root = LayoutInflater.from(context).inflate(R.layout.mini_player_panel, this, true);

        ivCover = root.findViewById(R.id.ivMiniCover);
        tvTitle = root.findViewById(R.id.tvMiniTitle);
        tvArtist = root.findViewById(R.id.tvMiniArtist);
        btnProgress = root.findViewById(R.id.btnPlayPauseProgress);
        ivPlayPauseIcon = root.findViewById(R.id.ivPlayPauseIcon);

        root.setOnClickListener(v -> {
            if (clickListener != null && currentTrack != null) {
                clickListener.onMiniPlayerClicked(currentTrack);
            }
        });

        btnProgress.setOnPlayPauseClickListener(wasPlaying -> {
            PlaybackManager.getInstance().togglePlayback();
            updatePlayPauseIcon(!wasPlaying);
        });

        setupPlaybackListener();

        setVisibility(GONE);

    }
    private void setupPlaybackListener() {
        AudioPlayerService.OnPlaybackListener listener = new AudioPlayerService.OnPlaybackListener() {
            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                updatePlayPauseIcon(isPlaying);
            }

            @Override
            public void onProgressUpdated(int currentPosition, int duration) {
                if (duration > 0) {
                    float fraction = (float) currentPosition / duration;
                    btnProgress.animateProgress(fraction, 200);
                }
            }

            @Override
            public void onTrackChanged(AudioTrack track) {
                updateTrackInfo(track);
                if (externalListener != null) {
                    externalListener.onTrackChanged(track);
                }
            }

            @Override
            public void onTrackCompleted() {
                if (externalListener != null) {
                    externalListener.onTrackCompleted();
                }
            }
        };
        PlaybackManager.getInstance().setUiListener(listener);
    }

    public void updateTrackInfo(AudioTrack track) {
        if (track == null) {
            setVisibility(GONE);
            return;
        }

        this.currentTrack = track;
        tvTitle.setText(track.getTitle());
        tvArtist.setText(track.getArtist());
        loadAlbumArt(track.getAlbumArtFileName());
        setVisibility(VISIBLE);

        btnProgress.resetProgress();
    }

    private void loadAlbumArt(String artFileName) {
        ivCover.setImageResource(R.drawable.ic_placeholder_cover);

        if (artFileName == null || artFileName.isEmpty()) return;

        new Thread(() -> {
            File artFile = new File(getContext().getFilesDir(), artFileName);
            if (!artFile.exists()) return;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = 2;

            Bitmap bitmap = BitmapFactory.decodeFile(artFile.getAbsolutePath(), options);

            if (bitmap != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        ivCover.setImageBitmap(bitmap)
                );
            }
        }).start();
    }

    public void updatePlayPauseIcon(boolean isPlaying) {
        ivPlayPauseIcon.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
        );
        btnProgress.setPlaying(isPlaying);
    }
    public void showPanel() {
        setVisibility(VISIBLE);
    }

    public void hidePanel() {
        setVisibility(GONE);
    }

    public void setOnMiniPlayerClickListener(OnMiniPlayerClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PlaybackManager.getInstance().setUiListener(null);
    }
}