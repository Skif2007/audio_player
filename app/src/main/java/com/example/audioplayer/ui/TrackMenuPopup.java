package com.example.audioplayer.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioplayer.models.AudioTrack;
import com.google.android.material.card.MaterialCardView;

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

        String[] labels = {
                "Зациклить", "Воспроизвести следующим", "Удалить с устройства",
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
                    listener.onMenuItemSelected(menuItem, track);
                }
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
        item.setTextSize(16); // ↑ было 14
        item.setPadding(dp(18), dp(14), dp(18), dp(14)); // ↑ было 16/12
        item.setBackground(background);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setMinHeight(dp(52)); // ↑ было 48, пропорционально тексту
        return item;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupItemAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start(); // ↑ было 0.96
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

    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}