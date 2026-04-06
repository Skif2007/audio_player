package com.example.audioplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

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
            // Попробуем получить через Directory
            try {
                File dir = volume.getDirectory();
                if (dir != null) return dir.getAbsolutePath();
            } catch (Exception ignored) {}
            return null;
        }
    }

    public void show() {
        if (availableRoots.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Ошибка")
                    .setMessage("Не удалось найти хранилища")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Spinner storageSpinner = new Spinner(context, Spinner.MODE_DROPDOWN);
        ArrayAdapter<StorageRoot> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, availableRoots);
        storageSpinner.setAdapter(adapter);
        storageSpinner.setPadding(32, 16, 32, 0);

        // 2. Создаём контейнер для списка папок
        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 8, 32, 16);
        scrollView.addView(layout);

        // 3. Функция для обновления списка папок при смене хранилища
        Runnable updateFolderList = () -> {
            layout.removeAllViews();
            StorageRoot currentRoot = (StorageRoot) storageSpinner.getSelectedItem();
            if (currentRoot == null) return;

            File currentDir = new File(currentRoot.path);
            File[] folders = getFoldersInDirectory(currentDir);
            List<CheckBox> checkBoxes = new ArrayList<>();

            if (folders != null && folders.length > 0) {
                for (File folder : folders) {
                    CheckBox checkBox = new CheckBox(context);
                    checkBox.setText(folder.getName());
                    checkBox.setTextSize(15);
                    final String currentFolderPath = folder.getAbsolutePath();
                    checkBox.setTag(currentFolderPath);
                    if (selectedPathsSet.contains(currentFolderPath)) {
                        checkBox.setChecked(true);
                    }

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

                    layout.addView(checkBox);
                }
            } else {
                TextView empty = new TextView(context);
                empty.setText("Папки не найдены или нет доступа");
                empty.setPadding(0, 20, 0, 20);
                layout.addView(empty);
            }
        };

        // Слушатель переключения хранилища
        storageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFolderList.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Собираем верхнюю панель (заголовок + спиннер)
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        TextView titleText = new TextView(context);
        titleText.setText("Выберите хранилище:");
        titleText.setPadding(32, 16, 32, 4);
        titleText.setTextSize(14);
        header.addView(titleText);
        header.addView(storageSpinner);

        // 6. Финальная сборка диалога
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(header);
        mainLayout.addView(scrollView);

        new AlertDialog.Builder(context)
                .setTitle("📁 Выбор папок")
                .setView(mainLayout)
                .setPositiveButton("ГОТОВО", (dialog, which) -> {
                    //List<String> selected = new ArrayList<>();
                    // Проходимся по всем чекбоксам в текущем списке
//                    for (int i = 0; i < layout.getChildCount(); i++) {
//                        View child = layout.getChildAt(i);
//                        if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
//                            selected.add((String) child.getTag());
//                        }
//                    }
                    if (listener != null) {
                        listener.onFoldersSelected(new ArrayList<>(selectedPathsSet));
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();

        // Инициализируем список для первого хранилища
        storageSpinner.setSelection(0);
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