package com.example.audioplayer.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audioplayer.R;
import com.example.audioplayer.TracksActivity;
import com.example.audioplayer.services.AudioPlayerService;
import com.example.audioplayer.utils.Mp3Scanner;
import com.example.audioplayer.utils.PlaybackManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
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
            itemHidden.setOnClickListener(v -> showHiddenTracksDialog());
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
    private void showHiddenTracksDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(requireContext());
        title.setText("Скрытая музыка");
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#212121"));
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);

        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView empty = new TextView(requireContext());
        empty.setText("Нет скрытых треков");
        empty.setTextColor(Color.parseColor("#757575"));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(24), dp(16), dp(24));
        empty.setVisibility(View.GONE);

        Set<String> hiddenPaths = Mp3Scanner.getHiddenTracks(requireContext());

        if (hiddenPaths.isEmpty()) {
            rv.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
            rv.setAdapter(new HiddenTracksAdapter(
                    requireContext(),
                    new ArrayList<>(hiddenPaths),
                    path -> {
                        Mp3Scanner.unhideTrack(requireContext(), path);
                        Toast.makeText(requireContext(),
                                "Теперь трек будет виден",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        showHiddenTracksDialog();
                    }
            ));
        }

        root.addView(rv);
        root.addView(empty);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
    private static class HiddenTracksAdapter extends RecyclerView.Adapter<HiddenTracksAdapter.ViewHolder> {
        private final Context ctx;
        private final List<String> paths;
        private final java.util.function.Consumer<String> onRemove;

        HiddenTracksAdapter(Context ctx, List<String> paths, java.util.function.Consumer<String> onRemove) {
            this.ctx = ctx;
            this.paths = paths;
            this.onRemove = onRemove;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout item = new LinearLayout(ctx);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setMinimumHeight(dp(52));

            LinearLayout textContainer = new LinearLayout(ctx);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            textContainer.setPadding(0, 0, dp(8), 0);

            TextView title = new TextView(ctx);
            title.setId(View.generateViewId());
            title.setTextSize(16);
            title.setTextColor(Color.parseColor("#212121"));
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView path = new TextView(ctx);
            path.setId(View.generateViewId());
            path.setTextSize(12);
            path.setTextColor(Color.parseColor("#757575"));
            path.setMaxLines(1);
            path.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);

            textContainer.addView(title);
            textContainer.addView(path);

            ImageButton remove = new ImageButton(ctx);
            remove.setId(View.generateViewId());
            remove.setImageResource(android.R.drawable.ic_menu_delete);
            remove.setBackgroundColor(Color.TRANSPARENT);
            remove.setScaleType(ImageView.ScaleType.CENTER);
            remove.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
            remove.setContentDescription("Удалить из скрытых");

            item.addView(textContainer);
            item.addView(remove);

            return new ViewHolder(item, title, path, remove);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = paths.get(position);
            File f = new File(path);
            String fileName = f.getName();
            String title = fileName.replaceFirst("\\.[^.]+$", ""); // убрать расширение

            holder.title.setText(title);
            holder.path.setText(path);
            holder.remove.setOnClickListener(v -> onRemove.accept(path));
        }

        @Override
        public int getItemCount() { return paths.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, path;
            ImageButton remove;
            ViewHolder(View itemView, TextView title, TextView path, ImageButton remove) {
                super(itemView);
                this.title = title;
                this.path = path;
                this.remove = remove;
            }
        }

        private static int dp(int dp) {
            return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density + 0.5f);
        }
    }
}