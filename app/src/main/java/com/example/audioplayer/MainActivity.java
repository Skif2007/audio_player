package com.example.audioplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    //**************
    //Уникальные коды для идентификации запросов
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int FOLDER_PICKED_CODE = 200;
    //**************

    // Ключ для сохранения URI выбранной папки в SharedPreferences
    private static final String PREF_SELECTED_FOLDER_URI = "selected_folder_uri";

    private Button btnSelectFolders;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectFolders = findViewById(R.id.btn_select_folders);

        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE);

        btnSelectFolders.setOnClickListener(v -> {
            /*Обработчик кнопки первичного выбора директорий*/
            if (hasPermissions()) {
                openFolderPicker();
            }
            else requestPermissions();
        });
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

    private void openFolderPicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, FOLDER_PICKED_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == FOLDER_PICKED_CODE && resultCode == RESULT_OK && data != null){
            Uri folderUri = data.getData();
            if(folderUri != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_SELECTED_FOLDER_URI, folderUri.toString());
                editor.apply();
                Toast.makeText(this, "Выбрана папка: " +
                        folderUri.toString(),Toast.LENGTH_LONG).show();
                //Здесь будет реализовано сканирование файлов
            }
            else {
                Toast.makeText(this, "Не удалось получить URI папки",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}