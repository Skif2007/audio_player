package com.example.audioplayer.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.adapters.TracksAdapter;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.Mp3Scanner;
import com.example.audioplayer.utils.PlaybackManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TracksFragment extends Fragment {

    private RecyclerView rvTracks;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TracksAdapter adapter;
    private Mp3Scanner scanner;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_FOLDERS = "selected_folder_paths";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tracks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTracks = view.findViewById(R.id.rvTracks);
        progressBar = view.findViewById(R.id.progressBar);
        tvStatus = view.findViewById(R.id.tvStatus);

        prefs = requireActivity().getSharedPreferences(PREFS_NAME, requireActivity().MODE_PRIVATE);

        rvTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TracksAdapter(requireContext(), new ArrayList<>(), (track, anchorView) ->
                showToast("Меню: " + track.getTitle())
        );
        rvTracks.setAdapter(adapter);

        // Восстанавливаем подсветку текущего трека при создании фрагмента
        if (PlaybackManager.getInstance().isReady()) {
            AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
            if (current != null && adapter != null) {
                adapter.setPlayingTrack(current);
            }
        }

        loadOrScanTracks();
    }

    private void loadOrScanTracks() {
        List<AudioTrack> savedTracks = Mp3Scanner.loadTracksFromPrefs(requireContext());
        if (!savedTracks.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvStatus.setVisibility(View.GONE);
            rvTracks.setVisibility(View.VISIBLE);
            adapter.updateTracks(savedTracks);

            // После загрузки треков снова применяем подсветку
            if (PlaybackManager.getInstance().isReady()) {
                AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
                if (current != null) {
                    adapter.setPlayingTrack(current);
                }
            }
            return;
        }
        startScanning();
    }

    private void startScanning() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Сканирование папок...");
        rvTracks.setVisibility(View.GONE);

        Set<String> foldersSet = prefs.getStringSet(KEY_FOLDERS, new HashSet<>());
        List<String> folders = new ArrayList<>(foldersSet);

        if (folders.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Нет выбранных папок");
            return;
        }

        scanner = new Mp3Scanner(requireContext(), tracks -> {
            if (!isAdded()) return;
            progressBar.setVisibility(View.GONE);
            tvStatus.setVisibility(View.GONE);
            rvTracks.setVisibility(View.VISIBLE);
            adapter.updateTracks(tracks);
            Mp3Scanner.saveTracksToPrefs(requireContext(), tracks);
            Toast.makeText(requireContext(), "Найдено: " + tracks.size() + " треков", Toast.LENGTH_SHORT).show();

            if (PlaybackManager.getInstance().isReady()) {
                AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
                if (current != null) {
                    adapter.setPlayingTrack(current);
                }
            }
        });

        scanner.startScanning(folders);
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scanner = null;
    }

    public TracksAdapter getAdapter() {
        return adapter;
    }
}