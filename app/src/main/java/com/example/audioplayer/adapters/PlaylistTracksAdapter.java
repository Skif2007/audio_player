package com.example.audioplayer.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.PlaybackManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistTracksAdapter extends RecyclerView.Adapter<PlaylistTracksAdapter.TrackViewHolder> {

    private final Context context;
    private List<AudioTrack> tracks;
    private final OnTrackMenuClickListener menuClickListener;
    private OnTrackClickListener trackClickListener;
    private int playingPosition = -1;

    public interface OnTrackMenuClickListener {
        void onMenuClick(AudioTrack track, View anchorView);
    }

    public interface OnTrackClickListener {
        void onTrackClick(AudioTrack track);
    }

    public PlaylistTracksAdapter(Context context, List<AudioTrack> tracks, OnTrackMenuClickListener listener) {
        this.context = context;
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        this.menuClickListener = listener;
    }

    public void setOnTrackClickListener(OnTrackClickListener listener) {
        this.trackClickListener = listener;
    }

    public void updateTracks(List<AudioTrack> newTracks) {
        this.tracks = newTracks != null ? newTracks : new ArrayList<>();
        playingPosition = -1;
        notifyDataSetChanged();
    }

    public List<AudioTrack> getCurrentTracks() {
        return tracks;
    }

    public void setPlayingTrack(AudioTrack track) {
        int oldPosition = playingPosition;
        playingPosition = -1;

        if (track != null) {
            for (int i = 0; i < tracks.size(); i++) {
                if (tracks.get(i).getFilePath().equals(track.getFilePath())) {
                    playingPosition = i;
                    break;
                }
            }
        }

        if (oldPosition != -1 && oldPosition < tracks.size()) {
            notifyItemChanged(oldPosition);
        }
        if (playingPosition != -1) {
            notifyItemChanged(playingPosition);
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= tracks.size()) return;
        if (toPosition < 0 || toPosition >= tracks.size()) return;
        AudioTrack track = tracks.remove(fromPosition);
        tracks.add(toPosition, track);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_track, parent, false);
        return new TrackViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        AudioTrack track = tracks.get(position);
        holder.bind(track, position == playingPosition, menuClickListener, trackClickListener);
    }

    @Override
    public void onViewRecycled(@NonNull TrackViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopEqualizer();
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {

        private final Context context;
        private final ImageView ivCover;
        private final TextView tvTitle, tvArtist;
        private final ImageButton btnMenu;
        private final View playingBorder, equalizerLayout;
        private final View bar1, bar2, bar3;

        private AnimatorSet equalizerAnimator;
        private boolean isAnimatorInitialized = false;

        public TrackViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            playingBorder = itemView.findViewById(R.id.viewPlayingBorder);
            equalizerLayout = itemView.findViewById(R.id.layoutEqualizer);
            bar1 = itemView.findViewById(R.id.bar1);
            bar2 = itemView.findViewById(R.id.bar2);
            bar3 = itemView.findViewById(R.id.bar3);
        }

        public void bind(AudioTrack track, boolean isPlaying,
                         OnTrackMenuClickListener menuClickListener,
                         OnTrackClickListener trackClickListener) {
            tvTitle.setText(track.getTitle());
            tvArtist.setText(track.getArtist());

            loadAlbumArtAsync(track.getAlbumArtFileName(), ivCover);

            setPlayingState(isPlaying);

            btnMenu.setOnClickListener(v -> {
                if (menuClickListener != null) {
                    menuClickListener.onMenuClick(track, v);
                }
            });

            itemView.setOnClickListener(v -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                }
                if (trackClickListener != null) {
                    trackClickListener.onTrackClick(track);
                } else {
                    PlaybackManager.getInstance().playTrack(track);
                }
            });
        }

        private void setPlayingState(boolean isPlaying) {
            if (playingBorder != null) {
                playingBorder.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            }
            if (equalizerLayout != null) {
                equalizerLayout.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            }

            if (isPlaying) {
                startEqualizer();
            } else {
                stopEqualizer();
                resetBars();
            }
        }

        private void resetBars() {
            if (bar1 != null) bar1.setScaleY(1f);
            if (bar2 != null) bar2.setScaleY(1f);
            if (bar3 != null) bar3.setScaleY(1f);
        }

        private void initEqualizerAnimator() {
            if (isAnimatorInitialized) return;

            ObjectAnimator anim1 = ObjectAnimator.ofFloat(bar1, View.SCALE_Y, 0.4f, 1f, 0.4f);
            ObjectAnimator anim2 = ObjectAnimator.ofFloat(bar2, View.SCALE_Y, 0.4f, 1f, 0.4f);
            ObjectAnimator anim3 = ObjectAnimator.ofFloat(bar3, View.SCALE_Y, 0.4f, 1f, 0.4f);

            for (ObjectAnimator anim : new ObjectAnimator[]{anim1, anim2, anim3}) {
                anim.setDuration(700);
                anim.setRepeatCount(ValueAnimator.INFINITE);
                anim.setRepeatMode(ValueAnimator.REVERSE);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
            }

            anim2.setStartDelay(230);
            anim3.setStartDelay(460);

            equalizerAnimator = new AnimatorSet();
            equalizerAnimator.playTogether(anim1, anim2, anim3);

            isAnimatorInitialized = true;
        }

        private void startEqualizer() {
            if (!isAnimatorInitialized) {
                initEqualizerAnimator();
            }
            if (equalizerAnimator != null && !equalizerAnimator.isRunning()) {
                equalizerAnimator.start();
            }
        }

        public void stopEqualizer() {
            if (equalizerAnimator != null && equalizerAnimator.isRunning()) {
                equalizerAnimator.cancel();
            }
        }

        private void loadAlbumArtAsync(String artFileName, ImageView imageView) {
            imageView.setImageResource(R.drawable.ic_placeholder_cover);
            if (artFileName == null || artFileName.isEmpty()) return;

            new Thread(() -> {
                File artFile = new File(context.getFilesDir(), artFileName);
                if (!artFile.exists()) return;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(artFile.getAbsolutePath(), options);

                if (bitmap != null) {
                    new Handler(Looper.getMainLooper()).post(() -> imageView.setImageBitmap(bitmap));
                }
            }).start();
        }
    }
}
