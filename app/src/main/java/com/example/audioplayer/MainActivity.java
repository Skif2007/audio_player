package com.example.audioplayer;
// TODO: добавить возможность заходить внутрь папок, улучшить визуал, добавить анимации
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.adapters.FoldersAdapter;
import com.example.audioplayer.models.SelectedFolder;
import com.example.audioplayer.utils.SimpleFolderPickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /* коды для ответов */
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 300;

    private static final String PREF_FOLDERS_SET = "selected_folder_paths";

    private Button btnSelectFolders, btnStartScan;
    private RecyclerView rvSelectedFolders;
    private FoldersAdapter foldersAdapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectFolders = findViewById(R.id.btn_select_folders);
        btnStartScan = findViewById(R.id.btn_start_scan);
        rvSelectedFolders = findViewById(R.id.rv_selected_folders);

        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);

        foldersAdapter = new FoldersAdapter();
        rvSelectedFolders.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedFolders.setAdapter(foldersAdapter);

        foldersAdapter.setOnFolderDeleteListener(position -> {
            /*Обработчик нажатия на иконку для удаления папки
            из списка*/
            List<SelectedFolder> currentList = getSelectedFoldersList();
            if (position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                saveFoldersList(currentList);
                updateFoldersDisplay();
            }
        });

        btnSelectFolders.setOnClickListener(v -> {
            /*Обработчик кнопки выбрать папки
            * для проверки на то, что необходимые права выданы*/
            if (!hasManageStoragePermission()) {
                new AlertDialog.Builder(this)
                        .setTitle("Доступ ко всем файлам")
                        .setMessage("Для выбора папок, включая Download и Android, приложению нужен полный доступ к хранилищу. Нажмите \"Разрешить\" и включите доступ в настройках.")
                        .setPositiveButton("Разрешить", (dialog, which) -> requestManageStoragePermission())
                        .setNegativeButton("Отмена", null)
                        .show();
            } else if (hasMediaPermissions()) {
                openFolderPicker();
            } else {
                requestMediaPermissions();
            }
        });

        btnStartScan.setOnClickListener(v -> {
            /*Обработчик заглушка для начала сканирования*/
            if (getSelectedFoldersSet().isEmpty()) {
                Toast.makeText(this, "Сначала выберите хотя бы одну папку", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Начинаем сканирование " + getSelectedFoldersSet().size() + " папок", Toast.LENGTH_SHORT).show();
                // TODO: Логика сканирования
            }
        });

        updateFoldersDisplay();
    }

    private boolean hasManageStoragePermission() {
        /*Проверка необходимости разрешения и его наличия*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void requestManageStoragePermission() {
        /*Вызываем стандартное окно для пользователя где
         он может выдать разрешение */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
            }
        }
    }

    private boolean hasMediaPermissions() {
        /*Проверяет наличие разрешения*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestMediaPermissions() {
        /*Вызывает системный диалог для получения разрешения*/
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
    }


    private void openFolderPicker() {
        /*Открывает мой кастыльный диалог для выбора необходимых директорий*/
        new SimpleFolderPickerDialog(
                this,
                selectedPaths -> {
                    if (selectedPaths != null && !selectedPaths.isEmpty()) {
                        List<SelectedFolder> currentFolders = getSelectedFoldersList();
                        int addedCount = 0;

                        for (String path : selectedPaths) {
                            File folder = new File(path);
                            if (folder.exists() && folder.isDirectory()) {
                                // Проверяем на дубликаты
                                boolean exists = false;
                                for (SelectedFolder f : currentFolders) {
                                    if (f.getUriString().equals(path)) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    String displayName = getHumanReadableFolderName(path);
                                    currentFolders.add(new SelectedFolder(path, displayName));
                                    addedCount++;
                                }
                            }
                        }

                        if (addedCount > 0) {
                            saveFoldersList(currentFolders);
                            updateFoldersDisplay();
                            Toast.makeText(MainActivity.this, "Добавлено папок: " + addedCount, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Выбранные папки уже в списке", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        ).show();
    }

    private List<SelectedFolder> getSelectedFoldersList() {
        /*Загружает список папок из настроек с читаемыми именами*/
        Set<String> pathSet = sharedPreferences.getStringSet(PREF_FOLDERS_SET, new HashSet<>());
        List<SelectedFolder> folders = new ArrayList<>();
        for (String path : pathSet) {
            String displayName = getHumanReadableFolderName(path);
            folders.add(new SelectedFolder(path, displayName));
        }
        return folders;
    }

    private Set<String> getSelectedFoldersSet() {
        return sharedPreferences.getStringSet(PREF_FOLDERS_SET, new HashSet<>());
    }

    private void saveFoldersList(List<SelectedFolder> folders) {
        //Сохраняем список папок в настройки
        Set<String> pathSet = new HashSet<>();
        for (SelectedFolder folder : folders) {
            pathSet.add(folder.getUriString());
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(PREF_FOLDERS_SET, pathSet);
        editor.apply();
    }

    private void updateFoldersDisplay() {
        //Обновляет отображение списка папок на экране
        List<SelectedFolder> folders = getSelectedFoldersList();
        foldersAdapter.setFolders(folders);
        btnStartScan.setEnabled(!folders.isEmpty());
    }


    //Обработка ответов на запросы прав
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show();
                openFolderPicker();
            } else {
                Toast.makeText(this, "Без разрешения доступ к файлам невозможен", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
            if (hasManageStoragePermission()) {
                Toast.makeText(this, "Полный доступ получен!", Toast.LENGTH_LONG).show();
                if (hasMediaPermissions()) {
                    openFolderPicker();
                } else {
                    requestMediaPermissions();
                }
            } else {
                Toast.makeText(this, "Доступ не предоставлен. Выбор некоторых папок может быть недоступен.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private String getHumanReadableFolderName(String path) {
        //Извлекает из пути просто называние папки
        if (path == null) return "Папка";
        File folder = new File(path);
        String name = folder.getName();
        if (name.isEmpty() || "/".equals(path)) return "Внутренняя память";
        if ("/storage/emulated/0/Download".equals(path)) return "Download";
        if ("/storage/emulated/0/Android".equals(path)) return "Android";
        if ("/storage/emulated/0/Music".equals(path)) return "Music";
        return name;
    }
}