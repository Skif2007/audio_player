package com.example.audioplayer;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.adapters.TracksAdapter;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.Mp3Scanner;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TracksActivity extends AppCompatActivity {
    private ImageView btnMenu;
    private TextView tabSongs, tabPlaylists;
    private View tabIndicator;
    private boolean isSongsTabActive = true;
    private LinearLayout headerPanel;
        private RecyclerView rvTracks;
        private DrawerLayout drawerLayout;
        private NavigationView navigationView;
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

            drawerLayout = findViewById(R.id.drawerLayout);
            navigationView = findViewById(R.id.navigationView);
            headerPanel = findViewById(R.id.headerPanel);

            navigationView.setNavigationItemSelectedListener(item -> {
                // Обработчик кликов по пунктам меню
                int id = item.getItemId();
                if (id == R.id.menu_item_1) {
                    Toast.makeText(this, "Нажат пункт 1", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.menu_item_2) {
                    Toast.makeText(this, "Нажат пункт 2", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.menu_item_3) {
                    Toast.makeText(this, "Нажат пункт 3", Toast.LENGTH_SHORT).show();
                }
                drawerLayout.closeDrawers();
                return true;
            });

            btnMenu = findViewById(R.id.btnMenu);
            tabSongs = findViewById(R.id.tabSongs);
            tabPlaylists = findViewById(R.id.tabPlaylists);
            tabIndicator = findViewById(R.id.tabIndicator);

            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

            tabSongs.setOnClickListener(v -> switchTab(true));
            tabPlaylists.setOnClickListener(v -> switchTab(false));

            updateTabStyles(isSongsTabActive);
            // Инициализация позиции индикатора
            tabSongs.post(() -> {
                int[] tabPos = new int[2];
                int[] headerPos = new int[2];

                tabSongs.getLocationOnScreen(tabPos);
                headerPanel.getLocationOnScreen(headerPos);

                float tabLeftRelativeToHeader = tabPos[0] - headerPos[0];
                float textWidth = tabSongs.getPaint().measureText(tabSongs.getText().toString());
                float textStartX = tabLeftRelativeToHeader + tabSongs.getPaddingLeft();

                float indicatorStartX = textStartX;

                tabIndicator.getLayoutParams().width = (int) textWidth;
                tabIndicator.setX(indicatorStartX);
                tabIndicator.requestLayout();
            });

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
    private void switchTab(boolean songsSelected) {
        if (isSongsTabActive == songsSelected) return;
        isSongsTabActive = songsSelected;
        animateIndicator(songsSelected);

        updateTabStyles(songsSelected);

        // TODO: здесь позже добавить логику переключения контента
    }

    private void updateTabStyles(boolean songsSelected) {
        TextView activeTab = songsSelected ? tabSongs : tabPlaylists;
        TextView inactiveTab = songsSelected ? tabPlaylists : tabSongs;

        activeTab.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .start();
        inactiveTab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start();

        activeTab.setTextColor(ContextCompat.getColor(this, R.color.tab_text_selected));
        activeTab.setTypeface(null, Typeface.BOLD);
        inactiveTab.setTextColor(ContextCompat.getColor(this, R.color.tab_text_unselected));
        inactiveTab.setTypeface(null, Typeface.NORMAL);
    }

    private void animateIndicator(boolean toSongsTab) {
        TextView targetTab = toSongsTab ? tabSongs : tabPlaylists;

        targetTab.post(() -> {
            int[] tabPos = new int[2];
            int[] headerPos = new int[2];

            targetTab.getLocationOnScreen(tabPos);
            headerPanel.getLocationOnScreen(headerPos);

            float tabLeftRelativeToHeader = tabPos[0] - headerPos[0];
            float textWidth = targetTab.getPaint().measureText(targetTab.getText().toString());
            float textStartX = tabLeftRelativeToHeader + targetTab.getPaddingLeft();
            float indicatorStartX = textStartX;

            // Анимация позиции
            tabIndicator.animate()
                    .x(indicatorStartX)
                    .setDuration(200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            ValueAnimator widthAnimator = ValueAnimator.ofInt(
                    tabIndicator.getWidth(),
                    (int) textWidth
            );
            widthAnimator.addUpdateListener(animation -> {
                int value = (Integer) animation.getAnimatedValue();
                tabIndicator.getLayoutParams().width = value;
                tabIndicator.requestLayout();
            });
            widthAnimator.setDuration(200);
            widthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            widthAnimator.start();
        });
    }
}


