package com.example.audioplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.adapters.TracksAdapter;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.Mp3Scanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TracksActivity extends AppCompatActivity {
        private RecyclerView rvTracks;
        private TracksAdapter adapter;
        private ProgressBar progressBar;
        private TextView tvStatus;
        private Mp3Scanner scanner;
        private SharedPreferences prefs;
        private static final String PREFS_NAME = "app_settings";
        private static final String KEY_FOLDERS = "selected_folder_paths";

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_tracks);

            rvTracks = findViewById(R.id.rvTracks);
            progressBar = findViewById(R.id.progressBar);
            tvStatus = findViewById(R.id.tvStatus);

            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            rvTracks.setLayoutManager(new LinearLayoutManager(this));
            List<AudioTrack> emptyList = new ArrayList<>();

            adapter = new TracksAdapter(this, emptyList, new TracksAdapter.OnTrackMenuClickListener() {
                @Override
                public void onMenuClick(AudioTrack track, View anchorView) {
                    Toast.makeText(TracksActivity.this, "Меню для: " + track.getTitle(), Toast.LENGTH_SHORT).show();
                    // TODO: Здесь потом добавим код для показа PopupMenu с действиями (играть, удалить, информация и т.д.)
                }

            });

            rvTracks.setAdapter(adapter);
            List<AudioTrack> savedTracks = Mp3Scanner.loadTracksFromPrefs(this);
            if (!savedTracks.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                rvTracks.setVisibility(View.VISIBLE);
                adapter.updateTracks(savedTracks);
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
            scanner = new Mp3Scanner(this, new Mp3Scanner.OnScanCompleteListener() {

                @Override
                public void onScanComplete(List<AudioTrack> tracks) {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.GONE);
                    rvTracks.setVisibility(View.VISIBLE);
                    adapter.updateTracks(tracks);
                    Mp3Scanner.saveTracksToPrefs(TracksActivity.this, tracks);

                    Toast.makeText(TracksActivity.this, "Найдено: " + tracks.size() + " треков", Toast.LENGTH_SHORT).show();

                }

            });

            scanner.startScanning(folders);

        }
        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

}


