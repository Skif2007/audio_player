package com.example.audioplayer.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.PlaylistDetailActivity;
import com.example.audioplayer.R;
import com.example.audioplayer.adapters.PlaylistsAdapter;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.models.Playlist;
import com.example.audioplayer.ui.PlaylistMenuPopup;
import com.example.audioplayer.utils.PlaybackManager;
import com.example.audioplayer.utils.PlaylistManager;

import java.util.List;

public class PlaylistsFragment extends Fragment {

    private RecyclerView rvPlaylists;
    private LinearLayout btnCreatePlaylist;
    private PlaylistsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlaylistsAdapter(requireContext());
        rvPlaylists.setAdapter(adapter);

        adapter.setOnPlaylistClickListener(playlist -> {
            Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
            startActivity(intent);
        });

        adapter.setOnPlaylistMenuClickListener((playlist, anchorView) -> {
            PlaylistMenuPopup popup = new PlaylistMenuPopup(requireContext(), (menuItem, selectedPlaylist) -> {
                switch (menuItem) {
                    case PLAY:
                        playPlaylist(selectedPlaylist);
                        break;
                    case RENAME:
                        showRenameDialog(selectedPlaylist);
                        break;
                    case DELETE:
                        showDeleteDialog(selectedPlaylist);
                        break;
                }
            });
            popup.show(anchorView, playlist);
        });

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists() {
        PlaylistManager.getInstance().loadFromPrefs(requireContext());
        adapter.updatePlaylists(PlaylistManager.getInstance().getAllPlaylists());
    }

    private void playPlaylist(Playlist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(requireContext(), "В плейлисте нет треков", Toast.LENGTH_SHORT).show();
            return;
        }
        AudioTrack firstTrack = tracks.get(0);
        PlaybackManager.getInstance().playTrackFromPlaylist(firstTrack, playlist.getId(), tracks);
        Toast.makeText(requireContext(), "Воспроизведение: " + playlist.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showRenameDialog(Playlist playlist) {
        EditText input = new EditText(requireContext());
        input.setText(playlist.getName());
        input.setSelection(playlist.getName().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new AlertDialog.Builder(requireContext())
                .setTitle("Переименовать")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    playlist.setName(name);
                    PlaylistManager.getInstance().saveToPrefs(requireContext());
                    loadPlaylists();
                    Toast.makeText(requireContext(), "Переименовано", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDeleteDialog(Playlist playlist) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить плейлист")
                .setMessage("Удалить \"" + playlist.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    PlaylistManager.getInstance().deletePlaylist(playlist.getId());
                    PlaylistManager.getInstance().saveToPrefs(requireContext());
                    loadPlaylists();
                    Toast.makeText(requireContext(), "Плейлист удалён", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Название плейлиста");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Новый плейлист")
                .setView(input)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PlaylistManager.getInstance().createPlaylist(name);
                    PlaylistManager.getInstance().saveToPrefs(requireContext());
                    loadPlaylists();
                    Toast.makeText(requireContext(), "Плейлист создан", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_white_rounded);
        }
        dialog.show();

        input.post(() -> {
            input.setTextColor(Color.BLACK);
            input.setHintTextColor(Color.parseColor("#888888"));
        });
    }
}
