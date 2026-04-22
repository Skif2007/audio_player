package com.example.audioplayer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.models.Playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

    private final Context context;
    private List<Playlist> playlists = new ArrayList<>();
    private OnPlaylistClickListener clickListener;
    private OnPlaylistMenuClickListener menuClickListener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistMenuClickListener {
        void onMenuClick(Playlist playlist, View anchorView);
    }

    public PlaylistsAdapter(Context context) {
        this.context = context;
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnPlaylistMenuClickListener(OnPlaylistMenuClickListener listener) {
        this.menuClickListener = listener;
    }

    public void updatePlaylists(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists != null ? newPlaylists : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvName.setText(playlist.getName());

        int count = playlist.getTrackCount();
        String tracksText = count + " " + getTracksWordForm(count);
        holder.tvTrackCount.setText(tracksText);

        loadAlbumArtAsync(playlist.getFirstAlbumArtFileName(), holder.ivCover);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlaylistClick(playlist);
            }
        });

        holder.btnMenu.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick(playlist, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    private String getTracksWordForm(int count) {
        int mod10 = count % 10;
        int mod100 = count % 100;
        if (mod10 == 1 && mod100 != 11) return "трек";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return "трека";
        return "треков";
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

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvTrackCount;
        ImageButton btnMenu;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivPlaylistCover);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvTrackCount = itemView.findViewById(R.id.tvTrackCount);
            btnMenu = itemView.findViewById(R.id.btnPlaylistMenu);
        }
    }
}
