package com.example.audioplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FolderViewHolder> {

    private List<SelectedFolder> folders = new ArrayList<>();
    private OnFolderDeleteListener deleteListener;

    public interface OnFolderDeleteListener {
        void onDelete(int position);
    }

    public void setOnFolderDeleteListener(OnFolderDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        SelectedFolder folder = folders.get(position);
        holder.tvFolderName.setText(folder.getDisplayName());
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void setFolders(List<SelectedFolder> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView tvFolderName;
        ImageButton btnDelete;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_folder);
        }
    }
}