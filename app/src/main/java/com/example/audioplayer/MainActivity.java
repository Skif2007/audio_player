package com.example.audioplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    //**************
    //Уникальные коды для идентификации запросов
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int FOLDER_PICKER_CODE = 200;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 300;
    //**************

    // Ключ для сохранения URI выбранной папки в SharedPreferences
    private static final String PREF_FOLDERS_SET = "selected_folder_uri";

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
            List<SelectedFolder> currentList = getSelectedFoldersList();
            if (position >= 0 && position < currentList.size()) {
                currentList.remove(position);
                saveFoldersList(currentList);
                updateFoldersDisplay();
            }
        });

        btnSelectFolders.setOnClickListener(v -> {
            if (!hasManageStoragePermission()) {
                new AlertDialog.Builder(this)
                        .setTitle("Доступ ко всем файлам")
                        .setMessage("Для выбора папок, включая Download, приложению нужен полный доступ к хранилищу. Нажмите \"Разрешить\" и включите доступ в настройках.")
                        .setPositiveButton("Разрешить", (dialog, which) -> requestManageStoragePermission())
                        .setNegativeButton("Отмена", null)
                        .show();
            } else if (hasPermissions()) {
                openFolderPicker();
            } else {
                requestPermissions();
            }
        });

        btnStartScan.setOnClickListener(v -> {
            if (getSelectedFoldersSet().isEmpty()) {
                Toast.makeText(this, "Сначала выберите хотя бы одну папку", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Начинаем сканирование " + getSelectedFoldersSet().size() + " папок", Toast.LENGTH_SHORT).show();
                // TODO: Сканирование
            }
        });
        updateFoldersDisplay();
    }

    private boolean hasManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            // Для Android 10 и ниже достаточно старых разрешений
            return true;
        }
    }

    // Запрашиваем полный доступ
    private void requestManageStoragePermission() {
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

    private boolean hasPermissions() {
        /*Проверяет, получило ли приложение необходимые разрешения*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        /*Запрашивает у пользователя необходимые разрешения с учётом версии*/
        String permission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        }
        else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                PERMISSION_REQUEST_CODE);
    }

    private void openFolderPicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, FOLDER_PICKER_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
            if (hasManageStoragePermission()) {
                Toast.makeText(this, "Полный доступ получен. Теперь можно выбирать любые папки.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Полный доступ не предоставлен. Выбор некоторых папок может быть недоступен.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == FOLDER_PICKER_CODE && resultCode == RESULT_OK && data != null) {
            Uri folderUri = data.getData();
            if (folderUri != null) {
                String displayName = getHumanReadableFolderName(folderUri);
                SelectedFolder newFolder = new SelectedFolder(folderUri.toString(), displayName);
                List<SelectedFolder> currentFolders = getSelectedFoldersList();
                currentFolders.add(newFolder);
                saveFoldersList(currentFolders);
                updateFoldersDisplay();
                Toast.makeText(this, "Папка \"" + displayName + "\" добавлена.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private List<SelectedFolder> getSelectedFoldersList() {
        Set<String> uriSet = sharedPreferences.getStringSet(PREF_FOLDERS_SET, new HashSet<>());
        List<SelectedFolder> folders = new ArrayList<>();
        for (String uriString : uriSet) {
            Uri uri = Uri.parse(uriString);
            String displayName = getHumanReadableFolderName(uri);
            folders.add(new SelectedFolder(uriString, displayName));
        }
        return folders;
    }

    private Set<String> getSelectedFoldersSet() {
        return sharedPreferences.getStringSet(PREF_FOLDERS_SET, new HashSet<>());
    }

    private void saveFoldersList(List<SelectedFolder> folders) {
        Set<String> uriSet = new HashSet<>();
        for (SelectedFolder folder : folders) {
            uriSet.add(folder.getUriString());
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(PREF_FOLDERS_SET, uriSet);
        editor.apply();
    }

    private void updateFoldersDisplay() {
        List<SelectedFolder> folders = getSelectedFoldersList();
        foldersAdapter.setFolders(folders);
        btnStartScan.setEnabled(!folders.isEmpty());
    }

    @Override
    /*Автоматически вызывается и обрабатывает ответ пользователя на запрос разрешений*/
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Разрешение получено! Теперь можно выбирать папки.",
                        Toast.LENGTH_SHORT).show();
                openFolderPicker();
            } else {
                Toast.makeText(this,
                        "Без разрешения на чтение файлов приложение не сможет работать.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getHumanReadableFolderName(Uri treeUri) {
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] parts = docId.split(":");
        if (parts.length >= 2) {
            String type = parts[0];
            String path = parts[1];
            if ("primary".equals(type)) {
                String lastSegment = path;
                int slashIndex = path.lastIndexOf('/');
                if (slashIndex != -1) {
                    lastSegment = path.substring(slashIndex + 1);
                }
                if (lastSegment.isEmpty()) {
                    return "Внутренняя память";
                }
                return lastSegment;
            } else {
                return type + (path.isEmpty() ? "" : "/" + path);
            }
        }
        return "Папка";
    }

}