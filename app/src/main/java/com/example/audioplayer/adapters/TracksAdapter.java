package com.example.audioplayer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.models.AudioTrack;

import java.io.File;
import java.util.List;

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.TrackViewHolder>{
    private Context context;
    private List<AudioTrack> tracks;
    private OnTrackMenuClickListener menuClickListener;

    public interface OnTrackMenuClickListener {
        void onMenuClick(AudioTrack track, View anchorView);
    }

    public TracksAdapter(Context context, List<AudioTrack> tracks, OnTrackMenuClickListener listener) {
        this.context = context;
        this.tracks = tracks;
        this.menuClickListener = listener;
    }

    public void updateTracks(List<AudioTrack> newTracks) {
        this.tracks = newTracks;
        notifyDataSetChanged();

    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        AudioTrack track = tracks.get(position);
        holder.tvTitle.setText(track.getTitle());
        holder.tvArtist.setText(track.getArtist());

        if (track.getAlbumArtFileName() != null) {
            File artFile = new File(context.getFilesDir(), track.getAlbumArtFileName());
            if (artFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap bitmap = BitmapFactory.decodeFile(artFile.getAbsolutePath());
                if (bitmap != null) {
                    holder.ivCover.setImageBitmap(bitmap);
                }
                else holder.ivCover.setImageResource(R.drawable.ic_placeholder_cover);
            }
            else {
                holder.ivCover.setImageResource(R.drawable.ic_placeholder_cover);
            }
        }
        else holder.ivCover.setImageResource(R.drawable.ic_placeholder_cover);
        holder.btnMenu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (menuClickListener != null) {
                    menuClickListener.onMenuClick(track, v);

                }
            }
        });
    }
    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        ImageButton btnMenu;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }

    }
}