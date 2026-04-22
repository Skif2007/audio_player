package com.example.audioplayer.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.audioplayer.models.Playlist;
import com.google.android.material.card.MaterialCardView;

public class PlaylistMenuPopup {

    public enum MenuItem {
        PLAY, RENAME, DELETE
    }

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(MenuItem item, Playlist playlist);
    }

    private final Context context;
    private final OnMenuItemSelectedListener listener;

    public PlaylistMenuPopup(Context context, OnMenuItemSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show(View anchor, Playlist playlist) {
        final PopupWindow popup = new PopupWindow(context);
        popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        MaterialCardView card = createPopupCard(playlist, popup);
        popup.setContentView(card);

        popup.showAsDropDown(anchor, -dp(12), -dp(4), Gravity.END);
    }

    private MaterialCardView createPopupCard(Playlist playlist, PopupWindow popup) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(12));
        card.setCardElevation(dp(6));
        card.setUseCompatPadding(true);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, dp(4));

        String[] labels = {"Воспроизвести", "Переименовать", "Удалить плейлист"};
        MenuItem[] items = MenuItem.values();

        Drawable selectableBackground = getSelectableItemBackground();

        for (int i = 0; i < labels.length; i++) {
            final String label = labels[i];
            final MenuItem menuItem = items[i];

            TextView item = createMenuItem(label, selectableBackground);
            setupItemAnimation(item);

            item.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMenuItemSelected(menuItem, playlist);
                }
                popup.dismiss();
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
        item.setTextSize(14);
        item.setPadding(dp(16), dp(12), dp(16), dp(12));
        item.setBackground(background);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setMinHeight(dp(48));
        return item;
    }

    private void setupItemAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }

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
