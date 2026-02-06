package com.example.sathihub;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class PersonalInfoActivity extends AppCompatActivity {

    EditText etName, etDob;
    Spinner spMaritalStatus, spHeight, spWeight, spBloodGroup;
    RadioGroup rgGender, rgDisability;
    Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        // Bind Views
        etName = findViewById(R.id.etName);
        etDob = findViewById(R.id.etDob);
        spMaritalStatus = findViewById(R.id.spMaritalStatus);
        spHeight = findViewById(R.id.spHeight);
        spWeight = findViewById(R.id.spWeight);
        spBloodGroup = findViewById(R.id.spBloodGroup);
        rgGender = findViewById(R.id.rgGender);
        rgDisability = findViewById(R.id.rgDisability);
        btnContinue = findViewById(R.id.btnContinue);

        // Date Picker for DOB
        etDob.setOnClickListener(v -> showDatePicker());

        // Load Spinner Data
        loadMaritalStatus();
        loadHeight();
        loadWeight();
        loadBloodGroup();

        // Continue Button
        btnContinue.setOnClickListener(v -> validateAndNext());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) ->
                        etDob.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1),
                year, month, day
        );
        dialog.show();
    }

    private void loadMaritalStatus() {
        String[] status = {"Select", "Never Married", "Divorced", "Widowed", "Separated"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, status);
        spMaritalStatus.setAdapter(adapter);
    }

    private void loadHeight() {
        ArrayList<String> heights = new ArrayList<>();
        heights.add("Select");
        for (int i = 140; i <= 210; i++) {
            heights.add(i + " cm");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, heights);
        spHeight.setAdapter(adapter);
    }

    private void loadWeight() {
        ArrayList<String> weights = new ArrayList<>();
        weights.add("Select");
        for (int i = 40; i <= 150; i++) {
            weights.add(i + " kg");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, weights);
        spWeight.setAdapter(adapter);
    }

    private void loadBloodGroup() {
        String[] blood = {"Select", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, blood);
        spBloodGroup.setAdapter(adapter);
    }

    private void validateAndNext() {
        String name = etName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        int selectedDisabilityId = rgDisability.getCheckedRadioButtonId();

        if (name.isEmpty()) {
            etName.setError("Enter Name");
            return;
        }

        if (selectedGenderId == -1) {
            Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dob.isEmpty()) {
            Toast.makeText(this, "Select DOB", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spMaritalStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select Marital Status", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spHeight.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select Height", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spBloodGroup.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select Blood Group", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDisabilityId == -1) {
            Toast.makeText(this, "Select Disability option", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGender = findViewById(selectedGenderId);
        RadioButton rbDisability = findViewById(selectedDisabilityId);

        String gender = rbGender.getText().toString();
        String disability = rbDisability.getText().toString();

        // Next Activity
        Intent intent = new Intent(PersonalInfoActivity.this, ReligiousActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("gender", gender);
        intent.putExtra("dob", dob);

        startActivity(intent);
    }
}
