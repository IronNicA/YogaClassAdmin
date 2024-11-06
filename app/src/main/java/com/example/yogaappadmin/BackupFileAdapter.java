package com.example.yogaappadmin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class BackupFileAdapter extends ArrayAdapter<String> {
    private final List<String> backupFiles;
    private String selectedBackup;

    public BackupFileAdapter(Context context, List<String> backupFiles) {
        super(context, android.R.layout.simple_list_item_1, backupFiles);
        this.backupFiles = backupFiles;
    }

    public void setSelectedBackup(String selectedBackup) {
        this.selectedBackup = selectedBackup;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        TextView textView = view.findViewById(android.R.id.text1);
        String fileName = backupFiles.get(position);
        textView.setText(fileName);

        if (fileName.equals(selectedBackup)) {
            view.setBackgroundColor(0xFFCCCCCC);
        } else {
            view.setBackgroundColor(0x00000000);
        }

        return view;
    }
}
