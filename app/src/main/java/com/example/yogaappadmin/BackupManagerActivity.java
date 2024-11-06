package com.example.yogaappadmin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.List;

public class BackupManagerActivity extends AppCompatActivity {

    private DbHelper dbHelper;
    private BackupFileAdapter adapter;
    private ListView backupListView;
    private Button restoreButton;
    private Button deleteButton;
    private List<String> backupFiles;
    private String selectedBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_manager);

        dbHelper = new DbHelper(this);

        backupListView = findViewById(R.id.backupListView);
        Button backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);
        deleteButton = findViewById(R.id.deleteButton);

        loadBackupFiles();

        backupListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedBackup = backupFiles.get(position);
            adapter.setSelectedBackup(selectedBackup);
            restoreButton.setEnabled(true);
            restoreButton.setBackgroundColor(ContextCompat.getColor(this, R.color.restore_data_green));
            deleteButton.setEnabled(true);
            deleteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.delete_button_red));
        });

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        backupButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    dbHelper.backupDatabase();
                    loadBackupFiles();
                    Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Manage External Storage permission is required to create a backup", Toast.LENGTH_SHORT).show();
                    requestManageExternalStoragePermission();
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    dbHelper.backupDatabase();
                    loadBackupFiles();
                    Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Write permission is required to create a backup", Toast.LENGTH_SHORT).show();
                }
            }
        });

        restoreButton.setOnClickListener(v -> {
            if (selectedBackup != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Restore Backup")
                        .setMessage("Are you sure you want to restore the database from " + selectedBackup + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dbHelper.restoreDatabase(selectedBackup);
                            Toast.makeText(this, "Database restored from " + selectedBackup, Toast.LENGTH_SHORT).show();
                            restoreButton.setEnabled(false);
                            deleteButton.setEnabled(false);
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                Toast.makeText(this, "Please select a backup to restore", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(v -> {
            if (selectedBackup != null) {
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Delete Backup")
                        .setMessage("Are you sure you want to delete the backup: " + selectedBackup )
                        .setPositiveButton(android.R.string.yes, null)
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                dialog.show();

                final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);

                new CountDownTimer(7000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        positiveButton.setText("Confirm (" + millisUntilFinished / 1000 + ")");
                    }

                    public void onFinish() {
                        positiveButton.setEnabled(true);
                        positiveButton.setText("Confirm");
                    }
                }.start();

                positiveButton.setOnClickListener(view -> {
                    dbHelper.deleteBackup(selectedBackup);
                    loadBackupFiles();
                    Toast.makeText(this, "Backup deleted: " + selectedBackup, Toast.LENGTH_SHORT).show();
                    restoreButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    dialog.dismiss();
                });
            } else {
                Toast.makeText(this, "Please select a backup to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d("BackupManagerActivity", "External storage manager permission already granted");
                Toast.makeText(this, "External storage manager permission already granted", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private String extractTimestamp(String filename) {
        String datePart = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));
        String formattedDate = datePart.replace("_", "");
        return formattedDate;
    }

    private void loadBackupFiles() {
        backupFiles = dbHelper.getBackupFiles();
        backupFiles.sort((file1, file2) -> {
            String timestamp1 = extractTimestamp(file1);
            String timestamp2 = extractTimestamp(file2);
            return timestamp2.compareTo(timestamp1);
        });

        adapter = new BackupFileAdapter(this, backupFiles);
        backupListView.setAdapter(adapter);

        if (backupFiles.isEmpty()) {
            backupListView.setVisibility(View.GONE);
            findViewById(R.id.noBackupTextView).setVisibility(View.VISIBLE);
        } else {
            backupListView.setVisibility(View.VISIBLE);
            findViewById(R.id.noBackupTextView).setVisibility(View.GONE);
        }
        selectedBackup = null;
        restoreButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }
}
