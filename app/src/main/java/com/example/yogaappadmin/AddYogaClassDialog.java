    package com.example.yogaappadmin;

    import android.app.Dialog;
    import android.app.TimePickerDialog;
    import android.content.Context;
    import android.text.format.DateFormat;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.Spinner;
    import android.widget.TextView;
    import android.widget.Toast;

    import java.util.Calendar;

    public class AddYogaClassDialog {

        private final Dialog dialog;
        private final DbHelper dbHelper;
        private final YogaClassListener listener;

        public AddYogaClassDialog(Context context, YogaClassListener listener) {
            this.listener = listener;
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_add_yoga_class);
            dbHelper = new DbHelper(context);

            setupViews();
        }

        private void setupViews() {
            Spinner spinnerDayOfWeek = dialog.findViewById(R.id.spinnerDayOfWeek);
            TextView editTextTime = dialog.findViewById(R.id.editTextTime);
            EditText editTextCapacity = dialog.findViewById(R.id.editTextCapacity);
            EditText editTextDuration = dialog.findViewById(R.id.editTextDuration);
            EditText editTextPricePerClass = dialog.findViewById(R.id.editTextPricePerClass);
            EditText editTextClassType = dialog.findViewById(R.id.editTextClassType);
            EditText editTextDescription = dialog.findViewById(R.id.editTextDescription);
            Button buttonSave = dialog.findViewById(R.id.saveButton);

            Button cancelButton = dialog.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(v -> dialog.dismiss());

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

            buttonSave.setOnClickListener(v -> {
                String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
                String time = editTextTime.getText().toString();
                String capacityStr = editTextCapacity.getText().toString();
                String durationStr = editTextDuration.getText().toString();
                String pricePerClassStr = editTextPricePerClass.getText().toString();
                String classType = editTextClassType.getText().toString();
                String description = editTextDescription.getText().toString();

                if (classType.isEmpty()) {
                    editTextClassType.setError("Please enter a class type");
                    Toast.makeText(dialog.getContext(), "Please enter a class type", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dayOfWeek.isEmpty()) {
                    Toast.makeText(dialog.getContext(), "Please select a day of the week", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (time.isEmpty()) {
                    editTextTime.setError("Please enter a time");
                    Toast.makeText(dialog.getContext(), "Please enter a time", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (capacityStr.isEmpty()) {
                    editTextCapacity.setError("Please enter a capacity");
                    Toast.makeText(dialog.getContext(), "Please enter a capacity", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Integer.parseInt(capacityStr) < 10) {
                    editTextCapacity.setError("Capacity must be more than than or equal to 10 people");
                    Toast.makeText(dialog.getContext(), "Capacity must be more than or equal to 10 people", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (durationStr.isEmpty()) {
                    editTextDuration.setError("Please enter a duration");
                    Toast.makeText(dialog.getContext(), "Please enter a duration", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Integer.parseInt(durationStr) < 45) {
                    editTextDuration.setError("Duration must be at least 45 minutes");
                    Toast.makeText(dialog.getContext(), "Duration must be at least 45 minutes", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pricePerClassStr.isEmpty()) {
                    editTextPricePerClass.setError("Please enter the price per class");
                    Toast.makeText(dialog.getContext(), "Please enter the price per class", Toast.LENGTH_SHORT).show();
                    return;
                }

                double pricePerClass = Double.parseDouble(pricePerClassStr);
                int capacity = Integer.parseInt(capacityStr);
                int duration = Integer.parseInt(durationStr);

                YogaClass yogaClass = new YogaClass(0, dayOfWeek, time, capacity, duration, pricePerClass, classType, description);

                showConfirmationDialog(yogaClass);
            });


        }

        private void showConfirmationDialog(YogaClass yogaClass) {
            Dialog confirmationDialog = new Dialog(dialog.getContext());
            confirmationDialog.setContentView(R.layout.dialog_confirmation);

            TextView confirmationText = confirmationDialog.findViewById(R.id.confirmationText);
            Button buttonConfirm = confirmationDialog.findViewById(R.id.buttonConfirm);
            Button buttonEdit = confirmationDialog.findViewById(R.id.buttonEdit);

            String confirmationMessage = "Day of Week: " + yogaClass.getDayOfWeek() + "\n" + "\n" +
                    "Time: " + yogaClass.getTime() + "\n" + "\n" +
                    "Capacity: " + yogaClass.getCapacity() + "\n" + "\n" +
                    "Duration: " + yogaClass.getDuration() + " minutes\n" + "\n" +
                    "Price: $" + yogaClass.getPricePerClass() + "\n" + "\n" +
                    "Course Type: " + yogaClass.getClassType() + "\n" + "\n" +
                    "Description: " + yogaClass.getDescription();

            confirmationText.setText(confirmationMessage);

            buttonConfirm.setOnClickListener(v -> {
                dbHelper.addYogaClass(yogaClass);
                listener.onYogaClassAdded(yogaClass);
                confirmationDialog.dismiss();
                dialog.dismiss();
                Toast.makeText(dialog.getContext(), "Yoga class added successfully!", Toast.LENGTH_SHORT).show();
            });

            buttonEdit.setOnClickListener(v -> {
                confirmationDialog.dismiss();
            });

            confirmationDialog.show();
            confirmationDialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }

        public void show() {
            dialog.show();
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }
