package com.example.audioplayer.utils;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.audioplayer.R;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimpleFolderPickerDialog {

    public interface OnFoldersSelectedListener {
        void onFoldersSelected(List<String> selectedPaths);
    }

    private final Context context;
    private final OnFoldersSelectedListener listener;
    private final List<StorageRoot> availableRoots = new ArrayList<>();
    private final Set<String> selectedPathsSet = new HashSet<>();

    // Внутренний класс для хранения информации о корне хранилища
    private static class StorageRoot {
        final String path;
        final String displayName;
        final boolean isPrimary;

        StorageRoot(String path, String displayName, boolean isPrimary) {
            this.path = path;
            this.displayName = displayName;
            this.isPrimary = isPrimary;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public SimpleFolderPickerDialog(Context context, OnFoldersSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        detectAllStorageVolumes();
    }


    private void detectAllStorageVolumes() {
        // 1. Всегда добавляем основное хранилище (внутренняя память)
        File primaryDir = android.os.Environment.getExternalStorageDirectory();
        availableRoots.add(new StorageRoot(
                primaryDir.getAbsolutePath(),
                "📱 Внутренняя память",
                true
        ));

        // 2. Пытаемся найти внешние SD-карты через StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                if (storageManager != null) {
                    List<StorageVolume> volumes = storageManager.getStorageVolumes();
                    for (StorageVolume volume : volumes) {
                        if (volume.isPrimary()) continue;

                        String path = getVolumePath(volume);
                        String desc = volume.getDescription(context);

                        if (path != null && new File(path).exists()) {
                            availableRoots.add(new StorageRoot(path, "💾 " + desc, false));
                        }
                    }
                }
            } catch (Exception e) {
                detectSdCardFallback();
            }
        } else {
            detectSdCardFallback();
        }
    }

    private void detectSdCardFallback() {
        String[] possiblePaths = {
                "/storage/sdcard1",
                "/storage/extSdCard",
                "/storage/external_sd",
                "/mnt/external_sd",
                "/mnt/sdcard/external_sd"
        };
        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                // Проверяем, не добавили ли мы уже этот путь
                boolean exists = false;
                for (StorageRoot root : availableRoots) {
                    if (root.path.equals(path)) exists = true;
                }
                if (!exists) {
                    availableRoots.add(new StorageRoot(path, "💾 SD Card (" + path + ")", false));
                }
            }
        }
    }

    private String getVolumePath(StorageVolume volume) {
        try {
            Method getPath = StorageVolume.class.getMethod("getPath");
            return (String) getPath.invoke(volume);
        } catch (Exception e) {
            try {
                File dir = volume.getDirectory();
                if (dir != null) return dir.getAbsolutePath();
            } catch (Exception ignored) {}
            return null;
        }
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_folder_picker, null);
        Spinner storageSpinner = dialogView.findViewById(R.id.spinner_storage);
        LinearLayout layoutFoldersList = dialogView.findViewById(R.id.layout_folders_list);
        ArrayAdapter<StorageRoot> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, availableRoots);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storageSpinner.setAdapter(adapter);

        Runnable updateFolderList = () -> {
            layoutFoldersList.removeAllViews();

            if (storageSpinner.getSelectedItem() == null) return;

            StorageRoot currentRoot = (StorageRoot) storageSpinner.getSelectedItem();
            File currentDir = new File(currentRoot.path);
            File[] folders = getFoldersInDirectory(currentDir);

            if (folders != null && folders.length > 0) {
                for (File folder : folders) {
                    com.google.android.material.checkbox.MaterialCheckBox checkBox =
                            new com.google.android.material.checkbox.MaterialCheckBox(context);

                    final String currentFolderPath = folder.getAbsolutePath();

                    checkBox.setText(folder.getName());
                    checkBox.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                    checkBox.setTextSize(16f);
                    checkBox.setPadding(16, 16, 16, 16);
                    checkBox.setTag(currentFolderPath);

                    checkBox.setChecked(selectedPathsSet.contains(currentFolderPath));

                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                selectedPathsSet.add(currentFolderPath);
                            } else {
                                selectedPathsSet.remove(currentFolderPath);
                            }
                        }
                    });

                    layoutFoldersList.addView(checkBox);
                }
            } else {
                TextView emptyText = new TextView(context);
                emptyText.setText("Папки не найдены или нет доступа");
                emptyText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(0, 40, 0, 40);
                layoutFoldersList.addView(emptyText);
            }
        };

        // Слушатель смены хранилища
        storageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFolderList.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        SpannableString orangeTitle = new SpannableString("📁 Выбор папок");
        orangeTitle.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accent_orange)),
                0,
                orangeTitle.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // 2. Создаём диалог
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(orangeTitle)
                .setView(dialogView)
                .setPositiveButton("Добавить выбранное", (dialog, which) -> {
                    if (listener != null) {
                        listener.onFoldersSelected(new ArrayList<>(selectedPathsSet));
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        // Инициализация
        if (availableRoots.size() > 0) {
            storageSpinner.setSelection(0);
        }
        updateFolderList.run();
    }

    private File[] getFoldersInDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.canRead()) {
            return new File[0];
        }
        // Фильтруем: только папки, не скрытые
        return dir.listFiles(file -> file != null && file.isDirectory() && !file.isHidden());
    }
}