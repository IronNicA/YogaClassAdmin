package com.example.yogaappadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchResultsActivity extends AppCompatActivity implements YogaClassListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        loadResults();

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadResults () {
        RecyclerView searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        TextView noResultsTextView = findViewById(R.id.noResultsTextView);

        DbHelper dbHelper = new DbHelper(this);

        String searchQuery = getIntent().getStringExtra("searchQuery");

        List<ClassInstance> searchResults = dbHelper.searchClassInstancesByTeacher(searchQuery);

        ClassInstanceAdapter adapter;
        if (searchResults.isEmpty()) {
            noResultsTextView.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            noResultsTextView.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            adapter = new ClassInstanceAdapter(searchResults, this);
            searchResultsRecyclerView.setAdapter(adapter);
        }

        adapter = new ClassInstanceAdapter(searchResults, this);
        searchResultsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadResults();
    }

    @Override
    public void onYogaClassAdded(YogaClass yogaClass) {

    }

    @Override
    public void onViewDetails(YogaClass yogaClass) {

    }

    @Override
    public void onViewInstanceDetails(ClassInstance instance) {
        Intent intent = new Intent(this, ClassInstanceDetailsActivity.class);
        intent.putExtra("instanceId", instance.getInstanceId());
        startActivity(intent);
    }
}

