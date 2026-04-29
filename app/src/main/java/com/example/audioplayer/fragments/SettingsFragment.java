package com.example.audioplayer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.audioplayer.R;
import com.example.audioplayer.TracksActivity;
import com.example.audioplayer.services.AudioPlayerService;
import com.example.audioplayer.utils.Mp3Scanner;
import com.example.audioplayer.utils.PlaybackManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LOCKSCREEN = "pref_lockscreen_playback";
    private static final String KEY_HEADPHONES = "pref_pause_on_headphone_disconnect";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    private void showScanConfirmDialog() {
        Activity activity = requireActivity();
        Context ctx = activity.getApplicationContext();

        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_scan_confirm, null);

        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_no).setOnClickListener(v -> {
            if (dialog.isShowing()) dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_yes).setOnClickListener(v -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            performRescan(activity, ctx);
        });

        dialog.setView(dialogView);
        dialog.show();
    }


    private void performRescan(Activity activity, Context ctx) {
        PlaybackManager pm = PlaybackManager.getInstance();
        if (pm.isReady() && pm.isPlaying()) {
            AudioPlayerService service = pm.getService();
            if (service != null) {
                service.pause();
            }
        }

        Intent intent = new Intent(activity, TracksActivity.class);
        intent.putExtra("EXTRA_TRIGGER_SCAN", true);
        activity.startActivity(intent);

        Toast.makeText(ctx, "Обновление библиотеки...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        View itemHidden = view.findViewById(R.id.item_hidden_music);
        if (itemHidden != null) {
            itemHidden.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Нажат пункт 'Скрытая музыка'", Toast.LENGTH_SHORT).show()
            );
        }

        View itemDirs = view.findViewById(R.id.item_scan_directories);
        if (itemDirs != null) {
            itemDirs.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Нажат пункт 'Сканируемые директории'", Toast.LENGTH_SHORT).show()
            );
        }

        SwitchMaterial switchLockscreen = view.findViewById(R.id.switch_lockscreen);
        if (switchLockscreen != null) {
            switchLockscreen.setChecked(prefs.getBoolean(KEY_LOCKSCREEN, false));
            switchLockscreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_LOCKSCREEN, isChecked).apply();
                Toast.makeText(getContext(),
                        "Экран блокировки: " + (isChecked ? "Включено" : "Выключено"),
                        Toast.LENGTH_SHORT).show();
            });
        }

        SwitchMaterial switchHeadphones = view.findViewById(R.id.switch_headphones);
        if (switchHeadphones != null) {
            switchHeadphones.setChecked(prefs.getBoolean(KEY_HEADPHONES, false));
            switchHeadphones.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_HEADPHONES, isChecked).apply();
                Toast.makeText(getContext(),
                        "Пауза наушники: " + (isChecked ? "Включено" : "Выключено"),
                        Toast.LENGTH_SHORT).show();
            });
        }

        View itemScan = view.findViewById(R.id.item_scan_music);
        if (itemScan != null) {
            itemScan.setOnClickListener(v -> showScanConfirmDialog());
        }
    }
}