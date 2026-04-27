package com.example.audioplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.adapters.PlaylistTracksAdapter;
import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.models.Playlist;
import com.example.audioplayer.services.AudioPlayerService;
import com.example.audioplayer.ui.AddToPlaylistDialog;
import com.example.audioplayer.ui.TrackMenuPopup;
import com.example.audioplayer.utils.MiniPlayerPanel;
import com.example.audioplayer.utils.PlaybackManager;
import com.example.audioplayer.utils.PlaylistManager;

import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private RecyclerView rvPlaylistTracks;
    private TextView tvPlaylistTitle, tvEmpty;
    private ImageButton btnBack;
    private PlaylistTracksAdapter adapter;
    private MiniPlayerPanel miniPlayer;

    private String playlistId;
    private Playlist playlist;

    private final AudioPlayerService.OnPlaybackListener playbackListener = new AudioPlayerService.OnPlaybackListener() {
        @Override
        public void onPlaybackStateChanged(boolean isPlaying) {}

        @Override
        public void onProgressUpdated(int currentPosition, int duration) {}

        @Override
        public void onTrackChanged(AudioTrack track) {
            if (adapter != null && track != null) {
                adapter.setPlayingTrack(track);
            }
            if (miniPlayer != null && track != null) {
                miniPlayer.updateTrackInfo(track);
                miniPlayer.showPanel();
            }
        }

        @Override
        public void onTrackCompleted() {
            // Обычная логика плейлиста (очередь "Play Next" уже обработана в PlaybackManager)
            PlaybackManager pm = PlaybackManager.getInstance();
            String currentPlaylist = pm.getCurrentPlaylistId();
            if (playlistId != null && playlistId.equals(currentPlaylist)) {
                AudioTrack next = pm.getNextTrackInPlaylist();
                if (next != null) {
                    pm.playTrack(next);
                } else {
                    pm.clearPlaylistContext();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getStringExtra(EXTRA_PLAYLIST_ID);
        if (playlistId == null) {
            Toast.makeText(this, "Ошибка: плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvPlaylistTracks = findViewById(R.id.rvPlaylistTracks);
        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);
        miniPlayer = findViewById(R.id.miniPlayerPanel);

        btnBack.setOnClickListener(v -> finish());

        rvPlaylistTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaylistTracksAdapter(this, null, (track, anchorView) -> {
            TrackMenuPopup popup = new TrackMenuPopup(this, (menuItem, selectedTrack) -> {
                if (menuItem == TrackMenuPopup.MenuItem.ADD_TO_PLAYLIST) {
                    AddToPlaylistDialog dialog = new AddToPlaylistDialog();
                    dialog.setTrackToAdd(selectedTrack);
                    dialog.show(getSupportFragmentManager(), "add_to_playlist");
                } else if (menuItem == TrackMenuPopup.MenuItem.LOOP) {
                    PlaybackManager.getInstance().toggleLoop(selectedTrack);
                    boolean isLooping = PlaybackManager.getInstance().isLooping(selectedTrack);
                    Toast.makeText(this,
                            isLooping ? "Зациклено: " + selectedTrack.getTitle() : "Цикл прерван",
                            Toast.LENGTH_SHORT).show();
                } else if (menuItem == TrackMenuPopup.MenuItem.PLAY_NEXT) {
                    PlaybackManager pm = PlaybackManager.getInstance();
                    if (pm.isPlaying()) {
                        pm.addToNextQueue(selectedTrack);
                        Toast.makeText(this, "В очереди: " + selectedTrack.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        pm.markPlayingFromNextQueue();
                        pm.playTrack(selectedTrack);
                        Toast.makeText(this, "Воспроизведение: " + selectedTrack.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            popup.show(anchorView, track);
        });

        adapter.setOnTrackClickListener(track -> {
            PlaybackManager.getInstance().interruptNextQueue();
            if (playlist != null) {
                PlaybackManager.getInstance().playTrackFromPlaylist(track, playlistId, playlist.getTracks());
            }
        });

        rvPlaylistTracks.setAdapter(adapter);
        setupDragAndDrop();
        setupMiniPlayer();
        loadPlaylist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlaybackManager.getInstance().addUiListener(playbackListener);
        if (playlist != null) {
            updatePlayingTrack();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackManager.getInstance().removeUiListener(playbackListener);
    }

    private void loadPlaylist() {
        PlaylistManager.getInstance().loadFromPrefs(this);
        playlist = PlaylistManager.getInstance().getPlaylist(playlistId);
        if (playlist == null) {
            Toast.makeText(this, "Плейлист не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvPlaylistTitle.setText(playlist.getName());
        updateTrackList();
    }

    private void updateTrackList() {
        List<AudioTrack> tracks = playlist.getTracks();
        adapter.updateTracks(tracks);
        if (tracks.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPlaylistTracks.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPlaylistTracks.setVisibility(View.VISIBLE);
        }
        updatePlayingTrack();
    }

    private void updatePlayingTrack() {
        if (PlaybackManager.getInstance().isReady()) {
            AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
            if (current != null) {
                adapter.setPlayingTrack(current);
            }
        }
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }
                adapter.onItemMove(fromPosition, toPosition);
                PlaylistManager.getInstance().moveTrackInPlaylist(playlistId, fromPosition, toPosition);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                PlaylistManager.getInstance().saveToPrefs(PlaylistDetailActivity.this);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvPlaylistTracks);
    }

    private void setupMiniPlayer() {
        miniPlayer.setOnMiniPlayerClickListener(track ->
                Toast.makeText(this, track.getTitle(), Toast.LENGTH_SHORT).show()
        );
        miniPlayer.setOnMiniPlayerListener(new MiniPlayerPanel.OnMiniPlayerListener() {
            @Override public void onTrackClicked(AudioTrack track) {}
            @Override public void onTrackChanged(AudioTrack track) {
                if (adapter != null) {
                    adapter.setPlayingTrack(track);
                }
            }
            @Override public void onTrackCompleted() {}
        });
        if (PlaybackManager.getInstance().isReady()) {
            AudioTrack current = PlaybackManager.getInstance().getCurrentTrack();
            if (current != null) {
                miniPlayer.updateTrackInfo(current);
                miniPlayer.showPanel();
                if (adapter != null) {
                    adapter.setPlayingTrack(current);
                }
            }
        }
    }
}