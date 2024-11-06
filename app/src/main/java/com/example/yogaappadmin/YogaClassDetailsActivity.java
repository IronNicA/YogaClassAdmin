package com.example.yogaappadmin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class YogaClassDetailsActivity extends AppCompatActivity implements YogaClassListener{

    private TextView classIdValue, classTypeValue, dayValue, timeValue, capacityValue, durationValue, pricePerClassValue, descriptionValue;

    private DbHelper dbHelper;

    private YogaClass selectedClass;

    ClassInstanceAdapter instanceAdapter;
    RecyclerView instanceRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yoga_class_details);

        dbHelper = new DbHelper(this);

        classIdValue = findViewById(R.id.classIdValue);
        classTypeValue = findViewById(R.id.classTypeValue);
        dayValue = findViewById(R.id.dayValue);
        timeValue = findViewById(R.id.timeValue);
        capacityValue = findViewById(R.id.capacityValue);
        durationValue = findViewById(R.id.durationValue);
        pricePerClassValue = findViewById(R.id.pricePerClassValue);
        descriptionValue = findViewById(R.id.descriptionValue);

        int classId = getIntent().getIntExtra("classId", -1);

        selectedClass = dbHelper.getYogaClassById(classId);
        if (selectedClass != null) {
            setYogaClassDetails(selectedClass);
        } else {
            Toast.makeText(this, "Yoga class not found", Toast.LENGTH_SHORT).show();
        }

        Button buttonEditClassType = findViewById(R.id.buttonEditClassType);
        Button buttonEditDay = findViewById(R.id.buttonEditDay);
        Button buttonEditTime = findViewById(R.id.buttonEditTime);
        Button buttonEditCapacity = findViewById(R.id.buttonEditCapacity);
        Button buttonEditDuration = findViewById(R.id.buttonEditDuration);
        Button buttonEditPricePerClass = findViewById(R.id.buttonEditPricePerClass);
        Button buttonEditDescription = findViewById(R.id.buttonEditDescription);

        buttonEditClassType.setOnClickListener(v -> showEditDialog("Class Type", selectedClass.getClassType()));
        buttonEditDay.setOnClickListener(v -> showEditDialog("Day of Week", selectedClass.getDayOfWeek()));
        buttonEditTime.setOnClickListener(v -> showEditDialog("Time", selectedClass.getTime()));
        buttonEditCapacity.setOnClickListener(v -> showEditDialog("Capacity", String.valueOf(selectedClass.getCapacity())));
        buttonEditDuration.setOnClickListener(v -> showEditDialog("Duration", String.valueOf(selectedClass.getDuration())));
        buttonEditPricePerClass.setOnClickListener(v -> showEditDialog("Price Per Class", String.valueOf(selectedClass.getPricePerClass())));
        buttonEditDescription.setOnClickListener(v -> showEditDialog("Description", selectedClass.getDescription()));

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        Button buttonDeleteClass = findViewById(R.id.buttonDeleteClass);
        buttonDeleteClass.setOnClickListener(v -> {
            if (selectedClass != null) {
                deleteYogaClass(selectedClass.getClassId());
            } else {
                Toast.makeText(YogaClassDetailsActivity.this, "No class selected to delete", Toast.LENGTH_SHORT).show();
            }
        });

        loadInstances ();

        Button createInstanceButton = findViewById(R.id.createInstanceButton);
        createInstanceButton.setOnClickListener(v -> showCreateInstanceDialog());
    }
    
    private void loadInstances () {
        instanceRecyclerView = findViewById(R.id.instanceRecyclerView);
        instanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ClassInstance> instances = dbHelper.getClassInstancesByClassId(selectedClass.getClassId());
        instanceAdapter = new ClassInstanceAdapter(instances, this);
        instanceRecyclerView.setAdapter(instanceAdapter);

        Button buttonDeleteCloud = findViewById(R.id.buttonDeleteCloud);
        buttonDeleteCloud.setVisibility(View.GONE);

        TextView textViewCloudStatus = findViewById(R.id.cloudStatusTextView);
        textViewCloudStatus.setText("Cloud Status: NONE");

        dbHelper.checkIfExists("yogaClasses", selectedClass.getClassId(), exists -> {
            if (exists) {
                textViewCloudStatus.setText("Cloud Status: UPLOADED");
                buttonDeleteCloud.setVisibility(View.VISIBLE);
            }
        });

        buttonDeleteCloud.setOnClickListener(v -> {
            if (selectedClass != null) {
                deleteYogaCloud(selectedClass.getClassId());
            } else {
                Toast.makeText(YogaClassDetailsActivity.this, "No class selected to delete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setYogaClassDetails(YogaClass yogaClass) {
        classIdValue.setText(String.valueOf(yogaClass.getClassId()));
        classTypeValue.setText(yogaClass.getClassType());
        dayValue.setText(yogaClass.getDayOfWeek());
        timeValue.setText(yogaClass.getTime());
        capacityValue.setText(String.valueOf(yogaClass.getCapacity()));
        durationValue.setText(String.valueOf(yogaClass.getDuration()) + " minutes");
        pricePerClassValue.setText("$" + String.valueOf(yogaClass.getPricePerClass()));
        descriptionValue.setText(yogaClass.getDescription());
    }

    @SuppressLint("SetTextI18n")
    private void showEditDialog(String field, String currentValue) {
        Dialog dialog = new Dialog(YogaClassDetailsActivity.this);
        dialog.setContentView(R.layout.dialog_edit_field);

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText("Editing " + field);

        EditText editTextValue = dialog.findViewById(R.id.editTextValue);
        Spinner spinnerDayOfWeek = dialog.findViewById(R.id.spinnerDayOfWeek);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        TextView editTextTime = dialog.findViewById(R.id.editTextTime);

        editTextValue.setVisibility(View.GONE);
        spinnerDayOfWeek.setVisibility(View.GONE);
        editTextTime.setVisibility(View.GONE);

        switch (field) {
            case "Day of Week":
                spinnerDayOfWeek.setVisibility(View.VISIBLE);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        this, R.array.week_days, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDayOfWeek.setAdapter(adapter);

                int spinnerPosition = adapter.getPosition(currentValue);
                spinnerDayOfWeek.setSelection(spinnerPosition);
                break;

            case "Time":
                editTextTime.setVisibility(View.VISIBLE);
                editTextTime.setText(currentValue);
                editTextTime.setOnClickListener(v -> {
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(dialog.getContext(), (view, selectedHour, selectedMinute) -> {
                        String amPm = selectedHour >= 12 ? "PM" : "AM";
                        int displayHour = selectedHour % 12 == 0 ? 12 : selectedHour % 12;
                        String formattedTime = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm);
                        editTextTime.setText(formattedTime);
                    }, hour, minute, false);

                    timePickerDialog.show();
                });
                break;

            case "Capacity":
                editTextValue.setVisibility(View.VISIBLE);
                editTextValue.setInputType(InputType.TYPE_CLASS_NUMBER);
                editTextValue.setText(currentValue);
                break;

            case "Duration":
                editTextValue.setVisibility(View.VISIBLE);
                editTextValue.setInputType(InputType.TYPE_CLASS_NUMBER);
                editTextValue.setText(currentValue);
                break;

            case "Price Per Class":
                editTextValue.setVisibility(View.VISIBLE);
                editTextValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editTextValue.setText(currentValue);
                break;

            default:
                editTextValue.setVisibility(View.VISIBLE);
                editTextValue.setText(currentValue);
                break;
        }

        buttonSave.setOnClickListener(v -> {
            String updatedValue;
            if (field.equals("Day of Week")) {
                updatedValue = spinnerDayOfWeek.getSelectedItem().toString();
            } else if (field.equals("Time")) {
                updatedValue = editTextTime.getText().toString();
            } else {
                updatedValue = editTextValue.getText().toString();
            }

            updateField(field, updatedValue);
            dialog.dismiss();
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    @SuppressLint("SetTextI18n")
    private void updateField(String field, String updatedValue) {
        switch (field) {
            case "Class Type":
                selectedClass.setClassType(updatedValue);
                classTypeValue.setText(updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "classType", updatedValue); 
                break;
            case "Day of Week":
                selectedClass.setDayOfWeek(updatedValue);
                dayValue.setText(updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "dayOfWeek", updatedValue); 
                break;
            case "Time":
                selectedClass.setTime(updatedValue);
                timeValue.setText(updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "time", updatedValue); 
                break;
            case "Capacity":
                selectedClass.setCapacity(Integer.parseInt(updatedValue));
                capacityValue.setText(updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "capacity", updatedValue); 
                break;
            case "Duration":
                selectedClass.setDuration(Integer.parseInt(updatedValue));
                durationValue.setText(updatedValue + " minutes");
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "duration", updatedValue); 
                break;
            case "Price Per Class":
                selectedClass.setPricePerClass(Double.parseDouble(updatedValue));
                pricePerClassValue.setText("$" + updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "pricePerClass", updatedValue); 
                break;
            case "Description":
                selectedClass.setDescription(updatedValue);
                descriptionValue.setText(updatedValue);
                dbHelper.updateYogaClassField(selectedClass.getClassId(), "description", updatedValue); 
                break;
        }
    }

    private void deleteYogaClass(int classId) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete this course? The course data and all of it's class will be deleted from the local data")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                boolean isDeleted = dbHelper.deleteYogaClass(classId);
                if (isDeleted) {
                    Toast.makeText(YogaClassDetailsActivity.this, "Class deleted successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(YogaClassDetailsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(YogaClassDetailsActivity.this, "Failed to delete the class", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.no, null)
            .show();
    }

    private void deleteYogaCloud(int classId) {
        if (isInternetAvailable()) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Course From Cloud")
                .setMessage("Are you sure you want to delete this course? The course data and all of its classes will be deleted from the cloud.")
                .setPositiveButton(android.R.string.yes, (dialogInterface, which) -> {
                    boolean isDeleted = dbHelper.deleteYogaCloud(classId);
                    loadInstances();
                    if (!isDeleted) {
                        Toast.makeText(YogaClassDetailsActivity.this, "Failed to delete the class", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(YogaClassDetailsActivity.this, "Course deleted from the cloud successfully", Toast.LENGTH_SHORT).show();
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

    @SuppressLint("SimpleDateFormat")
    private void showCreateInstanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Class");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_instance, null);
        builder.setView(dialogView);

        EditText dateInput = dialogView.findViewById(R.id.dateInput);
        EditText teacherInput = dialogView.findViewById(R.id.teacherInput);
        EditText commentsInput = dialogView.findViewById(R.id.commentsInput);
        Button saveInstanceButton = dialogView.findViewById(R.id.saveInstanceButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

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
                        ClassInstance newInstance = new ClassInstance(0,
                            selectedClass.getClassId(), date, teacher, comments);

                        dbHelper.addClassInstance(selectedClass.getClassId(), newInstance);

                        List<ClassInstance> instances = dbHelper.getClassInstancesByClassId(selectedClass.getClassId());
                        instanceAdapter = new ClassInstanceAdapter(instances,this);
                        instanceRecyclerView.setAdapter(instanceAdapter);

                        Toast.makeText(YogaClassDetailsActivity.this, "Instance created successfully ", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    } else {
                        Toast.makeText(YogaClassDetailsActivity.this, "Selected date must be on " + selectedClass.getDayOfWeek() , Toast.LENGTH_SHORT).show();
                    }
                } catch (ParseException e) {
                    Toast.makeText(YogaClassDetailsActivity.this, "Invalid date format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(YogaClassDetailsActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadInstances();
    }
}
