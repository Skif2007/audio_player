package com.example.audioplayer;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.audioplayer.fragments.CustomizationFragment;
import com.example.audioplayer.fragments.PlaylistsFragment;
import com.example.audioplayer.fragments.SettingsFragment;
import com.example.audioplayer.fragments.TracksFragment;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.MiniPlayerPanel;
import com.example.audioplayer.utils.PlaybackManager;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class TracksActivity extends AppCompatActivity {

    private ImageView btnMenu;
    private TextView tabSongs, tabPlaylists;
    private View tabIndicator;
    private LinearLayout headerPanel;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private boolean isSongsTabActive = true;
    private TracksFragment currentTracksFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        btnMenu = findViewById(R.id.btnMenu);
        tabSongs = findViewById(R.id.tabSongs);
        tabPlaylists = findViewById(R.id.tabPlaylists);
        tabIndicator = findViewById(R.id.tabIndicator);
        headerPanel = findViewById(R.id.headerPanel);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        PlaybackManager.getInstance().init(this);
        PlaybackManager.getInstance().bindService();

        setupMiniPlayer();
        setupDrawer();
        setupTabs();
        initTabIndicator();

        if (savedInstanceState == null) {
            loadTracksFragment();
            if (currentTracksFragment != null && getIntent().getBooleanExtra("EXTRA_TRIGGER_SCAN", false)) {
                currentTracksFragment.triggerRescan();
            }
            setHeaderVisible(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("EXTRA_TRIGGER_SCAN", false)) {
            if (currentTracksFragment != null) {
                currentTracksFragment.triggerRescan();
            }
        }
    }

    private void loadTracksFragment() {
        currentTracksFragment = new TracksFragment();
        openFragment(currentTracksFragment, false);
        isSongsTabActive = true;
        updateTabStyles(true);
        initTabIndicator();
    }

    private void openFragment(Fragment fragment, boolean animate) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (animate) {
            transaction.setCustomAnimations(
                    R.anim.fragment_slide_in,
                    R.anim.fragment_slide_out,
                    R.anim.fragment_slide_in,
                    R.anim.fragment_slide_out
            );
        }
        transaction.replace(R.id.fragmentContainer, fragment).commit();

        if (fragment instanceof TracksFragment) {
            currentTracksFragment = (TracksFragment) fragment;
            setHeaderVisible(true);
        } else if (fragment instanceof PlaylistsFragment) {
            setHeaderVisible(true);
        } else {
            setHeaderVisible(false);
        }
    }

    private void setHeaderVisible(boolean visible) {
        if (headerPanel != null) {
            headerPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (tabIndicator != null) {
            tabIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setupDrawer() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_library) {
                loadTracksFragment();
                setHeaderVisible(true);
            } else if (id == R.id.nav_settings) {
                openFragment(new SettingsFragment(), true);
            } else if (id == R.id.nav_customization) {
                openFragment(new CustomizationFragment(), true);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupTabs() {
        tabSongs.setOnClickListener(v -> {
            if (!isSongsTabActive) {
                loadTracksFragment();
            }
        });
        tabPlaylists.setOnClickListener(v -> {
            if (isSongsTabActive) {
                openFragment(new PlaylistsFragment(), true);
                isSongsTabActive = false;
                animateIndicator(false);
                updateTabStyles(false);
            }
        });
    }

    private void updateTabStyles(boolean songsSelected) {
        TextView activeTab = songsSelected ? tabSongs : tabPlaylists;
        TextView inactiveTab = songsSelected ? tabPlaylists : tabSongs;

        activeTab.animate().cancel();
        inactiveTab.animate().cancel();

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
            if (headerPanel == null) return;
            int[] tabPos = new int[2];
            int[] headerPos = new int[2];

            targetTab.getLocationOnScreen(tabPos);
            headerPanel.getLocationOnScreen(headerPos);

            float tabLeftRelativeToHeader = tabPos[0] - headerPos[0];
            float textWidth = targetTab.getPaint().measureText(targetTab.getText().toString());
            float textStartX = tabLeftRelativeToHeader + targetTab.getPaddingLeft();

            tabIndicator.animate()
                    .x(textStartX)
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

    private void initTabIndicator() {
        if (tabSongs == null || headerPanel == null) return;
        tabSongs.post(() -> {
            int[] tabPos = new int[2];
            int[] headerPos = new int[2];

            tabSongs.getLocationOnScreen(tabPos);
            headerPanel.getLocationOnScreen(headerPos);

            float tabLeftRelativeToHeader = tabPos[0] - headerPos[0];
            float textWidth = tabSongs.getPaint().measureText(tabSongs.getText().toString());
            float textStartX = tabLeftRelativeToHeader + tabSongs.getPaddingLeft();

            tabIndicator.getLayoutParams().width = (int) textWidth;
            tabIndicator.setX(textStartX);
            tabIndicator.requestLayout();
        });
    }

    private void setupMiniPlayer() {
        MiniPlayerPanel miniPlayer = findViewById(R.id.miniPlayerPanel);

        miniPlayer.setOnMiniPlayerClickListener(track ->
                Toast.makeText(this, "Открываем плеер: " + track.getTitle(), Toast.LENGTH_SHORT).show()
        );

        miniPlayer.setOnMiniPlayerListener(new MiniPlayerPanel.OnMiniPlayerListener() {
            @Override
            public void onTrackClicked(AudioTrack track) {
            }

            @Override
            public void onTrackChanged(AudioTrack track) {
                if (currentTracksFragment != null && currentTracksFragment.getAdapter() != null) {
                    currentTracksFragment.getAdapter().setPlayingTrack(track);
                }
            }

            @Override
            public void onTrackCompleted() {
                PlaybackManager pm = PlaybackManager.getInstance();
                if (pm.wasPlayingFromNextQueue()) {
                    pm.resetNextQueueFlag();
                    Toast.makeText(TracksActivity.this, "Очередь пуста", Toast.LENGTH_SHORT).show();
                    return;
                }
                AudioTrack nextPlaylistTrack = PlaybackManager.getInstance().getNextTrackInPlaylist();
                if (nextPlaylistTrack != null) {
                    PlaybackManager.getInstance().playTrack(nextPlaylistTrack);
                    return;
                }

                if (currentTracksFragment == null || currentTracksFragment.getAdapter() == null) return;

                AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
                List<AudioTrack> tracks = currentTracksFragment.getAdapter().getCurrentTracks();

                if (current != null && tracks != null) {
                    for (int i = 0; i < tracks.size(); i++) {
                        if (tracks.get(i).getFilePath().equals(current.getFilePath())) {
                            if (i < tracks.size() - 1) {
                                PlaybackManager.getInstance().playTrack(tracks.get(i + 1));
                            }
                            break;
                        }
                    }
                }
            }
        });

        if (PlaybackManager.getInstance().isReady()) {
            AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
            if (current != null) {
                miniPlayer.updateTrackInfo(current);
                miniPlayer.showPanel();
                if (currentTracksFragment != null && currentTracksFragment.getAdapter() != null) {
                    currentTracksFragment.getAdapter().setPlayingTrack(current);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlaybackManager.getInstance().unbindService();
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }
}