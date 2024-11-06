package com.example.yogaappadmin;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements YogaClassListener {
    private RecyclerView recyclerViewYogaClasses;
    private YogaClassAdapter yogaClassAdapter;
    private DbHelper dbHelper;
    private List<YogaClass> yogaClasses;
    private boolean isSyncNeeded;
    private LinearLayout syncLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewYogaClasses = findViewById(R.id.recyclerViewYogaClasses);
        Button addClassButton = findViewById(R.id.addClassButton);

        EditText searchInput = findViewById(R.id.searchInput);
        Button searchButton = findViewById(R.id.searchButton);

        syncLayout = findViewById(R.id.syncLayout);
        syncLayout.setVisibility(View.INVISIBLE);

        Button btnUpload = findViewById(R.id.btnUpload);
        Button btnSync = findViewById(R.id.btnSync);
        Button btnSyncManual = findViewById(R.id.btnSyncManual);

        dbHelper = new DbHelper(this);

        isSyncNeeded = dbHelper.isSyncNeeded();
        updateSyncLayoutVisibility();

        loadClasses();

        addClassButton.setOnClickListener(v -> {
            AddYogaClassDialog dialog = new AddYogaClassDialog(MainActivity.this, MainActivity.this);
            dialog.show();
        });

        searchButton.setOnClickListener(v -> {
            String teacherName = searchInput.getText().toString().trim();
            if (!teacherName.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, SearchResultsActivity.class);
                intent.putExtra("searchQuery", teacherName);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a teacher's name", Toast.LENGTH_SHORT).show();
            }
        });

        btnUpload.setOnClickListener(v -> uploadLocalData());
        btnSync.setOnClickListener(v -> syncLocalDataWithFirebase());
        btnSyncManual.setOnClickListener(v -> syncLocalDataWithFirebase());

        Button openBackupManagerButton = findViewById(R.id.openBackupManagerButton);
        openBackupManagerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BackupManagerActivity.class);
            startActivity(intent);
        });
    }

    private void updateSyncLayoutVisibility() {
        if (isSyncNeeded) {
            syncLayout.setVisibility(View.VISIBLE);
        } else {
            syncLayout.setVisibility(View.GONE);
        }
    }

    private void uploadLocalData() {
        if (isInternetAvailable()) {
            Toast.makeText(this, "Uploading local data...", Toast.LENGTH_SHORT).show();
            isSyncNeeded = false;
            updateSyncLayoutVisibility();
            dbHelper.syncYogaClassesAndInstances();
        } else {
            Toast.makeText(this, "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncLocalDataWithFirebase() {
        if (isInternetAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Sync")
                    .setMessage("This will delete any un-uploaded local data. Do you want to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Toast.makeText(this, "Syncing with cloud...", Toast.LENGTH_SHORT).show();
                        isSyncNeeded = false;
                        updateSyncLayoutVisibility();
                        dbHelper.syncLocalDataWithFirebase();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Toast.makeText(this, "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadClasses() {
        yogaClasses = dbHelper.getAllYogaClasses();
        yogaClassAdapter = new YogaClassAdapter(yogaClasses, this,this);

        recyclerViewYogaClasses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewYogaClasses.setAdapter(yogaClassAdapter);

        isSyncNeeded = dbHelper.isSyncNeeded();
        updateSyncLayoutVisibility();

        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) {
                    yogaClassAdapter.updateList(dbHelper.getAllYogaClasses());
                } else {
                    filterByClassType(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}

        });
    }

    private void filterByClassType(String query) {
        List<YogaClass> filteredList = new ArrayList<>();

        if (query.trim().isEmpty()) {
            filteredList.addAll(yogaClasses);
        } else {
            for (YogaClass yogaClass : yogaClasses) {
                if (yogaClass.getClassType() != null &&
                        yogaClass.getClassType().toLowerCase().contains(query.toLowerCase().trim())) {
                    filteredList.add(yogaClass);
                }
            }
        }
        yogaClassAdapter.updateList(filteredList);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClasses();
    }

    @Override
    public void onYogaClassAdded(YogaClass yogaClass) {
        yogaClasses.add(yogaClass);
        yogaClassAdapter.notifyItemInserted(yogaClasses.size() - 1);
        loadClasses();
    }

    @Override
    public void onViewDetails(YogaClass yogaClass) {
        Intent intent = new Intent(this, YogaClassDetailsActivity.class);
        intent.putExtra("classId", yogaClass.getClassId());
        startActivity(intent);
    }

    @Override
    public void onViewInstanceDetails (ClassInstance classInstance){}

}
