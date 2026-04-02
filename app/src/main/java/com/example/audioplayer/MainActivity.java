package com.example.audioplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
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
            /*Обработчик кнопки первичного выбора директорий*/
            if (hasPermissions()) {
                openFolderPicker();
            }
            else requestPermissions();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLDER_PICKER_CODE && resultCode == RESULT_OK && data != null) {
            Uri folderUri = data.getData();
            if (folderUri != null) {
                String displayName = getDisplayNameFromUri(folderUri);
                if (displayName == null || displayName.isEmpty()) {
                    displayName = folderUri.getLastPathSegment();
                }
                SelectedFolder newFolder = new SelectedFolder(folderUri.toString(), displayName);
                List<SelectedFolder> currentFolders = getSelectedFoldersList();
                currentFolders.add(newFolder);
                saveFoldersList(currentFolders);
                updateFoldersDisplay();
                Toast.makeText(this, "Папка \"" + displayName + "\" добавлена.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getDisplayNameFromUri(Uri uri) {
        String displayName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return displayName;
    }
    private List<SelectedFolder> getSelectedFoldersList() {
        Set<String> uriSet = sharedPreferences.getStringSet(PREF_FOLDERS_SET, new HashSet<>());
        List<SelectedFolder> folders = new ArrayList<>();
        for (String uriString : uriSet) {
            folders.add(new SelectedFolder(uriString, uriString));
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

}