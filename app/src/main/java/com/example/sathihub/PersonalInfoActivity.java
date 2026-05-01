package com.example.sathihub;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.sathihub.model.PersonalModel;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class PersonalInfoActivity extends AppCompatActivity {

    EditText etName, etDob;
    Spinner spMaritalStatus, spHeight, spWeight, spBloodGroup;
    RadioGroup rgGender, rgDisability;
    Button btnContinue;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        etName = findViewById(R.id.etName);
        etDob = findViewById(R.id.etDob);

        spMaritalStatus = findViewById(R.id.spMaritalStatus);
        spHeight = findViewById(R.id.spHeight);
        spWeight = findViewById(R.id.spWeight);
        spBloodGroup = findViewById(R.id.spBloodGroup);

        rgGender = findViewById(R.id.rgGender);
        rgDisability = findViewById(R.id.rgDisability);
        btnContinue = findViewById(R.id.btnContinue);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etDob.setOnClickListener(v -> showDatePicker());

        loadMaritalStatus();
        loadHeight();
        loadWeight();
        loadBloodGroup();

        btnContinue.setOnClickListener(v -> validateAndSave());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> etDob.setText(d + "/" + (m + 1) + "/" + y),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void loadMaritalStatus() {
        String[] status = {"Select", "Unmarried", "Divorced", "Widowed", "Separated"};
        spMaritalStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, status));
    }

    private void loadHeight() {
        ArrayList<String> heights = new ArrayList<>();
        heights.add("Select");
        for (int ft = 4; ft <= 7; ft++) {
            for (int inch = 0; inch < 12; inch++) {
                heights.add(ft + " ft " + inch + " in");
            }
        }
        spHeight.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, heights));
    }

    private void loadWeight() {
        ArrayList<String> weights = new ArrayList<>();
        weights.add("Select");
        for (int i = 30; i <= 150; i++) {
            weights.add(i + " kg");
        }
        spWeight.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, weights));
    }

    private void loadBloodGroup() {
        String[] blood = {"Select", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        spBloodGroup.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, blood));
    }

    private void validateAndSave() {

        String name = etName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        int selectedDisabilityId = rgDisability.getCheckedRadioButtonId();

        if (name.isEmpty() || dob.isEmpty()
                || selectedGenderId == -1
                || selectedDisabilityId == -1
                || spMaritalStatus.getSelectedItemPosition() == 0
                || spHeight.getSelectedItemPosition() == 0
                || spBloodGroup.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGender = findViewById(selectedGenderId);
        RadioButton rbDisability = findViewById(selectedDisabilityId);

        String gender = rbGender.getText().toString();
        String disability = rbDisability.getText().toString();
        String maritalStatus = spMaritalStatus.getSelectedItem().toString();
        String height = spHeight.getSelectedItem().toString();
        String weight = spWeight.getSelectedItem().toString();
        String bloodGroup = spBloodGroup.getSelectedItem().toString();

        int age = calculateAge(dob);
        String uid = auth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("PersonalInfo")
                .child(uid)
                .setValue(new PersonalModel(
                        name, gender, dob, String.valueOf(age),
                        height, weight, bloodGroup, maritalStatus, disability
                ))
                .addOnSuccessListener(unused -> {
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(uid)
                            .child("name").setValue(name)
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Personal Info Saved", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, ReligiousActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int calculateAge(String dob) {
        String[] parts = dob.split("/");
        Calendar dobCal = Calendar.getInstance();
        dobCal.set(Integer.parseInt(parts[2]),
                Integer.parseInt(parts[1]) - 1,
                Integer.parseInt(parts[0]));

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) age--;
        return age;
    }
}
