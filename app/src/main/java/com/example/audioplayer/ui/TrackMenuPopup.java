package com.example.audioplayer.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioplayer.models.AudioTrack;
import com.example.audioplayer.utils.PlaybackManager; // ← обычный импорт
import com.google.android.material.card.MaterialCardView;

import java.io.File;

public class TrackMenuPopup {

    public enum MenuItem {
        LOOP, PLAY_NEXT, DELETE, ADD_TO_PLAYLIST, HIDE, SHARE, ABOUT
    }

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(MenuItem item, AudioTrack track);
    }

    private final Context context;
    private final OnMenuItemSelectedListener listener;

    public TrackMenuPopup(Context context, OnMenuItemSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show(View anchor, AudioTrack track) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(createPopupCard(track, dialog));
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.5f);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private MaterialCardView createPopupCard(AudioTrack track, Dialog dialog) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(12));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, dp(4));

        // ← прямой вызов вместо рефлексии
        boolean isLooping = PlaybackManager.getInstance().isLooping(track);

        String[] labels = {
                isLooping ? "Прервать цикл" : "Зациклить",
                "Воспроизвести следующим", "Удалить с устройства",
                "Добавить в плейлист", "Скрыть трек", "Поделиться", "О треке"
        };
        MenuItem[] items = MenuItem.values();

        Drawable selectableBackground = getSelectableItemBackground();

        for (int i = 0; i < labels.length; i++) {
            final String label = labels[i];
            final MenuItem menuItem = items[i];

            TextView item = createMenuItem(label, selectableBackground);
            setupItemAnimation(item);

            item.setOnClickListener(v -> {
                if (listener != null) {
                    if (menuItem == MenuItem.ABOUT) {
                        showTrackInfoDialog(track, dialog);
                        return;
                    } else if (menuItem == MenuItem.SHARE) {
                        shareTrack(track);
                        dialog.dismiss();
                        return;
                    }
                    listener.onMenuItemSelected(menuItem, track);
                }
                //   Когда все пункты реализую уберу эти тосты
                Toast.makeText(context, "Нажат пункт: " + label, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });


            layout.addView(item);
        }

        card.addView(layout);
        return card;
    }

    private TextView createMenuItem(String text, Drawable background) {
        TextView item = new TextView(context);
        item.setText(text);
        item.setTextColor(Color.BLACK);
        item.setTextSize(16);
        item.setPadding(dp(18), dp(14), dp(18), dp(14));
        item.setBackground(background);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setMinHeight(dp(52));
        return item;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupItemAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }

    @SuppressLint("ResourceType")
    private Drawable getSelectableItemBackground() {
        int[] attrs = {android.R.attr.selectableItemBackground};
        android.content.res.TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs);
        Drawable background = typedArray.getDrawable(0);
        typedArray.recycle();
        return background != null ? background : new ColorDrawable(Color.TRANSPARENT);
    }

    private void showTrackInfoDialog(AudioTrack track, Dialog parentDialog) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(track.getFilePath());

            String title = track.getTitle();
            String artist = track.getArtist();
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            String durationStr = formatDuration(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String format = track.getFileFormat();
            String size = formatFileSize(track.getFileSize());
            String path = track.getFilePath();
            String fileName = path != null ? new File(path).getName() : "Неизвестно";
            String dateAdded = track.getFile() != null
                    ? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(track.getFile().lastModified()))
                    : "Неизвестно";

            com.google.android.material.card.MaterialCardView rootCard =
                    new com.google.android.material.card.MaterialCardView(context);
            rootCard.setCardBackgroundColor(Color.WHITE);
            rootCard.setRadius(dp(16));
            rootCard.setCardElevation(dp(4));

            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(Color.WHITE);
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(20), dp(16), dp(20), dp(24));
            content.setBackgroundColor(Color.WHITE);

            int keyColor = Color.parseColor("#757575");
            int valueColor = Color.parseColor("#212121");
            int textSizeKey = 13;
            int textSizeValue = 14;

            String[][] rows = {
                    {"Название", title}, {"Исполнитель", artist}, {"Альбом", album},
                    {"Жанр", genre}, {"Год", year}, {"Длительность", durationStr},
                    {"Битрейт", bitrate != null ? bitrate + " б/с" : "Неизвестно"},
                    {"Формат", format}, {"Размер", size}, {"Имя файла", fileName},
                    {"Путь", path}, {"Дата добавления", dateAdded}
            };

            for (String[] row : rows) {
                if (row[1] == null || row[1].isEmpty()) continue;

                // Ключ
                TextView key = new TextView(context);
                key.setText(row[0]);
                key.setTextColor(keyColor);
                key.setTextSize(textSizeKey);
                key.setPadding(0, dp(8), 0, dp(4));

                TextView val = new TextView(context);
                val.setText(row[1]);
                val.setTextColor(valueColor);
                val.setTextSize(textSizeValue);
                val.setPadding(0, 0, 0, dp(4));
                val.setSingleLine(false);
                val.setMaxLines(Integer.MAX_VALUE);
                val.setEllipsize(null);
                val.setHorizontallyScrolling(false);
                val.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);

                content.addView(key);
                content.addView(val);
            }

            scroll.addView(content);
            rootCard.addView(scroll);

            android.app.AlertDialog infoDialog = new android.app.AlertDialog.Builder(context)
                    .setView(rootCard)
                    .create();

            if (infoDialog.getWindow() != null) {
                infoDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }

            infoDialog.show();
            parentDialog.dismiss();

        } catch (Exception e) {
            android.util.Log.e("TrackMenuPopup", "Error showing track info", e);
            Toast.makeText(context, "Не удалось загрузить информацию", Toast.LENGTH_SHORT).show();
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private void shareTrack(AudioTrack track) {
        File file = track.getFile();
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("audio/*");

            android.net.Uri uri;
            try {
                uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        file
                );
            } catch (IllegalArgumentException | NullPointerException e) {
                android.content.Intent textShare = new android.content.Intent(android.content.Intent.ACTION_SEND);
                textShare.setType("text/plain");
                textShare.putExtra(android.content.Intent.EXTRA_TEXT,
                        "🎵 " + track.getTitle() + "\n👤 " + track.getArtist() +
                                "\n\nФайл: " + file.getAbsolutePath());
                context.startActivity(android.content.Intent.createChooser(textShare, "Поделиться треком"));
                return;
            }

            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться треком"));

        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(context, "Нет приложений для отправки", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("TrackMenuPopup", "Share error", e);
            Toast.makeText(context, "Ошибка при отправке", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDuration(String durationMs) {
        if (durationMs == null) return "Неизвестно";
        try {
            int totalSec = Integer.parseInt(durationMs) / 1000;
            int min = totalSec / 60;
            int sec = totalSec % 60;
            return String.format(java.util.Locale.getDefault(), "%d:%02d", min, sec);
        } catch (Exception e) { return "Неизвестно"; }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 0) return "Неизвестно";
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f КБ", bytes / 1024f);
        if (bytes < 1024 * 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f МБ", bytes / (1024f * 1024));
        return String.format(java.util.Locale.getDefault(), "%.1f ГБ", bytes / (1024f * 1024 * 1024));
    }

    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

}