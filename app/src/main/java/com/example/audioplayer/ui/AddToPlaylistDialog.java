package com.example.audioplayer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.models.Playlist;
import com.example.audioplayer.utils.PlaylistManager;

import java.util.List;

public class AddToPlaylistDialog extends DialogFragment {

    private AudioTrack trackToAdd;

    public void setTrackToAdd(AudioTrack track) {
        this.trackToAdd = track;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        PlaylistManager.getInstance().loadFromPrefs(context);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_to_playlist, null);

        LinearLayout btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);
        RecyclerView rvPlaylists = view.findViewById(R.id.rvPlaylists);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        btnCreatePlaylist.setOnClickListener(v -> {
            dialog.dismiss();
            showCreateAndAddDialog(context);
        });

        List<Playlist> playlists = PlaylistManager.getInstance().getAllPlaylists();
        rvPlaylists.setLayoutManager(new LinearLayoutManager(context));

        SimplePlaylistAdapter adapter = new SimplePlaylistAdapter(playlists, playlist -> {
            if (trackToAdd != null) {
                PlaylistManager.getInstance().addTrackToPlaylist(playlist.getId(), trackToAdd);
                PlaylistManager.getInstance().saveToPrefs(context);
                Toast.makeText(context, "Добавлено в \"" + playlist.getName() + "\"", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        rvPlaylists.setAdapter(adapter);

        return dialog;
    }

    private void showCreateAndAddDialog(Context context) {
        EditText input = new EditText(context);
        input.setHint("Название плейлиста");
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new AlertDialog.Builder(context)
                .setTitle("Новый плейлист")
                .setView(input)
                .setPositiveButton("Создать", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Playlist newPlaylist = PlaylistManager.getInstance().createPlaylist(name);
                    if (trackToAdd != null) {
                        PlaylistManager.getInstance().addTrackToPlaylist(newPlaylist.getId(), trackToAdd);
                        PlaylistManager.getInstance().saveToPrefs(context);
                        Toast.makeText(context, "Плейлист создан и трек добавлен", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    static class SimplePlaylistAdapter extends RecyclerView.Adapter<SimplePlaylistAdapter.ViewHolder> {

        private final List<Playlist> playlists;
        private final OnPlaylistClickListener listener;

        interface OnPlaylistClickListener {
            void onClick(Playlist playlist);
        }

        SimplePlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
            this.playlists = playlists;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dialog_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Playlist playlist = playlists.get(position);
            holder.tvName.setText(playlist.getName());
            holder.itemView.setOnClickListener(v -> listener.onClick(playlist));
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView tvName;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPlaylistName);
            }
        }
    }
}
