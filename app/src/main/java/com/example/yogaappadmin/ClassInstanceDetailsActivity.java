package com.example.yogaappadmin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;

import java.text.ParseException;

public class ClassInstanceDetailsActivity extends AppCompatActivity {

    private TextView instanceIdValue, teacherValue, dateValue, commentsValue, instanceStatusValue;
    private YogaClass selectedClass;
    ClassInstance selectedInstance;
    private DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_instance_details);

        dbHelper = new DbHelper(this);

        instanceIdValue = findViewById(R.id.instanceIdValue);
        teacherValue = findViewById(R.id.teacherValue);
        dateValue = findViewById(R.id.dateValue);
        commentsValue = findViewById(R.id.commentsValue);

        int instanceId = getIntent().getIntExtra("instanceId", -1);
        selectedInstance = dbHelper.getClassInstanceById(instanceId);

        selectedClass = dbHelper.getYogaClassById(selectedInstance.getClassId());

        if (selectedInstance != null) {
            setClassInstanceDetails(selectedInstance);
        } else {
            Toast.makeText(this, "Class instance not found", Toast.LENGTH_SHORT).show();
        }

        loadCheckCloud();

        Button buttonEditInstance = findViewById(R.id.buttonEditInstance);
        buttonEditInstance.setOnClickListener(v -> showEditDialog(selectedInstance));

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonDeleteInstance = findViewById(R.id.buttonDeleteClass);
        buttonDeleteInstance.setOnClickListener(v -> {
            if (selectedInstance != null) {
                deleteClassInstance(selectedInstance.getInstanceId());
            } else {
                Toast.makeText(ClassInstanceDetailsActivity.this, "No class to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCheckCloud (){
        Button buttonDeleteCloud = findViewById(R.id.buttonDeleteCloud);
        buttonDeleteCloud.setVisibility(View.GONE);

        TextView textViewCloudStatus = findViewById(R.id.cloudStatusTextView);
        textViewCloudStatus.setText("Cloud Status: NONE");

        dbHelper.checkIfExists("classInstances", selectedInstance.getInstanceId(), exists -> {
            if (exists) {
                textViewCloudStatus.setText("Cloud Status: UPLOADED");
                buttonDeleteCloud.setVisibility(View.VISIBLE);
            }
        });

        buttonDeleteCloud.setOnClickListener(v -> {
            if (selectedClass != null) {
                deleteClassCloud(selectedInstance.getInstanceId());
            } else {
                Toast.makeText(ClassInstanceDetailsActivity.this, "No class selected to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setClassInstanceDetails(ClassInstance instance) {
        instanceIdValue.setText(String.valueOf(instance.getInstanceId()));
        teacherValue.setText(instance.getTeacher());
        dateValue.setText(instance.getDate());
        commentsValue.setText(instance.getComments());
    }

    @SuppressLint("SimpleDateFormat")
    private void showEditDialog(ClassInstance classInstance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Class");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_instance, null);
        builder.setView(dialogView);

        EditText dateInput = dialogView.findViewById(R.id.dateInput);
        EditText teacherInput = dialogView.findViewById(R.id.teacherInput);
        EditText commentsInput = dialogView.findViewById(R.id.commentsInput);
        Button saveInstanceButton = dialogView.findViewById(R.id.saveInstanceButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        dateInput.setText(classInstance.getDate());
        teacherInput.setText(classInstance.getTeacher());
        commentsInput.setText(classInstance.getComments());

        dateInput.setFocusable(false);
        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        dateInput.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setCancelable(true);
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveInstanceButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString();
            String teacher = teacherInput.getText().toString();
            String comments = commentsInput.getText().toString();

            if (!date.isEmpty() && !teacher.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    Date selectedDate = dateFormat.parse(date);
                    Calendar selectedCalendar = Calendar.getInstance();
                    if (selectedDate != null) {
                        selectedCalendar.setTime(selectedDate);
                    }

                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                    Calendar classCalendar = Calendar.getInstance();
                    classCalendar.setTime(dayFormat.parse(selectedClass.getDayOfWeek()));

                    if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == classCalendar.get(Calendar.DAY_OF_WEEK)) {
                        classInstance.setDate(date);
                        classInstance.setTeacher(teacher);
                        classInstance.setComments(comments);

                        dbHelper.updateClassInstance(classInstance);

                        setClassInstanceDetails(classInstance);

                        Toast.makeText(ClassInstanceDetailsActivity.this, "Edited instance successfully", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    } else {
                        Toast.makeText(ClassInstanceDetailsActivity.this, "Selected date must be on " + selectedClass.getDayOfWeek(), Toast.LENGTH_SHORT).show();
                    }
                } catch (ParseException e) {
                    Toast.makeText(ClassInstanceDetailsActivity.this, "Invalid date format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ClassInstanceDetailsActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void deleteClassInstance(int instanceId) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage("Are you sure you want to delete this class from the local data?")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                boolean isDeleted = dbHelper.deleteClassInstance(instanceId);
                if (isDeleted) {
                    Toast.makeText(ClassInstanceDetailsActivity.this, "Instance deleted successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ClassInstanceDetailsActivity.this, YogaClassDetailsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("classId", selectedClass.getClassId());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ClassInstanceDetailsActivity.this, "Failed to delete the instance", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.no, null)
            .show();
    }

    private void deleteClassCloud(int instanceId) {
        if (isInternetAvailable()) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Delete Class")
                    .setMessage("Are you sure you want to delete this class from the cloud data?")
                    .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                        boolean isDeleted = dbHelper.deleteInstanceCloud(instanceId);
                        loadCheckCloud();
                        if (!isDeleted) {
                            Toast.makeText(ClassInstanceDetailsActivity.this, "Failed to delete the instance", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ClassInstanceDetailsActivity.this, "Class deleted from the cloud successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
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
        } else {
            Toast.makeText(this, "No internet connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
        }
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
}