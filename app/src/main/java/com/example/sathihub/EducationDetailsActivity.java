package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EducationDetailsActivity extends AppCompatActivity {

    Spinner spQualification, spOccupation, spSector, spIncome;
    MultiAutoCompleteTextView etLanguages;   // 🔥 changed
    EditText etOtherOccupation, etOrganization, etCity, etState;
    Button btnNext;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_details);

        spQualification = findViewById(R.id.spQualification);
        spOccupation = findViewById(R.id.spOccupation);
        spSector = findViewById(R.id.spSector);
        spIncome = findViewById(R.id.spIncome);

        etLanguages = findViewById(R.id.etLanguages); // 🔥 new id

        etOtherOccupation = findViewById(R.id.etOtherOccupation);
        etOrganization = findViewById(R.id.etOrganization);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);

        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadQualification();
        loadOccupation();
        loadSector();
        loadIncome();
        loadLanguages();   // 🔥 changed

        spOccupation.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                String occ = spOccupation.getSelectedItem().toString();
                if (occ.equals("Other")) {
                    etOtherOccupation.setVisibility(View.VISIBLE);
                } else {
                    etOtherOccupation.setVisibility(View.GONE);
                }
            }
        });

        loadExistingData(); // ✅ Edit Profile ke liye

        btnNext.setOnClickListener(v -> validateAndSave());
    }

    // ================= LOAD EXISTING DATA =================

    private void loadExistingData() {
        String uid = auth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("education")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {

                        setSpinner(spQualification, snapshot.child("qualification").getValue(String.class));
                        setSpinner(spOccupation, snapshot.child("occupation").getValue(String.class));
                        setSpinner(spSector, snapshot.child("sector").getValue(String.class));
                        setSpinner(spIncome, snapshot.child("income").getValue(String.class));

                        etLanguages.setText(snapshot.child("language").getValue(String.class));

                        etOtherOccupation.setText(snapshot.child("otherOccupation").getValue(String.class));
                        etOrganization.setText(snapshot.child("organization").getValue(String.class));
                        etCity.setText(snapshot.child("city").getValue(String.class));
                        etState.setText(snapshot.child("state").getValue(String.class));
                    }
                });
    }

    private void setSpinner(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int pos = adapter.getPosition(value);
        if (pos >= 0) spinner.setSelection(pos);
    }

    // ================= LOAD SPINNERS =================

    private void loadQualification() {
        String[] data = {
                "Select", "10th", "12th", "Diploma", "ITI",
                "Graduate", "Post Graduate", "MBA", "M.Tech",
                "PhD", "Doctor", "Engineer", "CA", "CS",
                "ICWA", "Other"
        };
        spQualification.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadOccupation() {
        String[] data = {
                "Select", "Software Engineer", "Doctor", "Teacher",
                "Business", "Government Job", "Private Job",
                "Farmer", "Shop Owner", "Self Employed",
                "Lawyer", "Chartered Accountant", "Banker",
                "Defence", "Police", "Professor", "Entrepreneur",
                "Housewife", "Student", "Unemployed", "Other"
        };
        spOccupation.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadSector() {
        String[] data = {
                "Select", "Private Sector", "Government Sector",
                "Public Sector", "Self Employed",
                "Business", "Not Working"
        };
        spSector.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadIncome() {
        String[] data = {
                "Select", "No Income", "Below 1 Lakh", "1 - 2 Lakh",
                "2 - 3 Lakh", "3 - 5 Lakh", "5 - 7 Lakh",
                "7 - 10 Lakh", "10 - 15 Lakh", "15 - 20 Lakh",
                "20 - 30 Lakh", "Above 30 Lakh"
        };
        spIncome.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    // 🔥 MULTI LANGUAGE LOGIC
    private void loadLanguages() {
        String[] data = {
                "Hindi", "English", "Gujarati", "Marathi",
                "Punjabi", "Urdu", "Bengali", "Tamil",
                "Telugu", "Kannada", "Malayalam"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, data);

        etLanguages.setAdapter(adapter);
        etLanguages.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    }

    // ================= SAVE DATA =================

    private void validateAndSave() {

        if (spQualification.getSelectedItemPosition() == 0 ||
                spOccupation.getSelectedItemPosition() == 0 ||
                spSector.getSelectedItemPosition() == 0 ||
                spIncome.getSelectedItemPosition() == 0 ||
                etLanguages.getText().toString().trim().isEmpty() ||
                etCity.getText().toString().trim().isEmpty() ||
                etState.getText().toString().trim().isEmpty()) {

            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference eduRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("education");
        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);

        String qualification = spQualification.getSelectedItem().toString();
        String occupation = spOccupation.getSelectedItem().toString();
        String sector = spSector.getSelectedItem().toString();
        String income = spIncome.getSelectedItem().toString();
        String languages = etLanguages.getText().toString().trim();
        String otherOccupation = etOtherOccupation.getText().toString().trim();
        String organization = etOrganization.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();

        Tasks.whenAllSuccess(
            eduRef.child("qualification").setValue(qualification),
            eduRef.child("occupation").setValue(occupation),
            eduRef.child("sector").setValue(sector),
            eduRef.child("income").setValue(income),
            eduRef.child("language").setValue(languages),
            eduRef.child("otherOccupation").setValue(otherOccupation),
            eduRef.child("organization").setValue(organization),
            eduRef.child("city").setValue(city),
            eduRef.child("state").setValue(state),
            personalRef.child("education").setValue(qualification),
            personalRef.child("occupation").setValue(occupation),
            personalRef.child("city").setValue(city)
        ).addOnSuccessListener(results -> {
            Toast.makeText(this, "Education Details Saved", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, FamilyDetailsActivity.class));
            finish();
        }).addOnFailureListener(e ->
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
