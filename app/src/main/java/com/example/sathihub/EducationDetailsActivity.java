package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EducationDetailsActivity extends AppCompatActivity {

    Spinner spQualification, spOccupation, spSector, spIncome, spLanguage;
    EditText etOtherOccupation, etOrganization, etCity, etState;
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education_details);

        spQualification = findViewById(R.id.spQualification);
        spOccupation = findViewById(R.id.spOccupation);
        spSector = findViewById(R.id.spSector);
        spIncome = findViewById(R.id.spIncome);
        spLanguage = findViewById(R.id.spLanguage);

        etOtherOccupation = findViewById(R.id.etOtherOccupation);
        etOrganization = findViewById(R.id.etOrganization);
        etCity = findViewById(R.id.etCity);
        etState = findViewById(R.id.etState);

        btnNext = findViewById(R.id.btnNext);

        loadQualification();
        loadOccupation();
        loadSector();
        loadIncome();
        loadLanguage();

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

        btnNext.setOnClickListener(v -> validateAndNext());
    }

    private void loadQualification() {
        String[] data = {
                "Select",
                "10th",
                "12th",
                "Diploma",
                "ITI",
                "Graduate",
                "Post Graduate",
                "MBA",
                "M.Tech",
                "PhD",
                "Doctor",
                "Engineer",
                "CA",
                "CS",
                "ICWA",
                "Other"
        };
        spQualification.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadOccupation() {
        String[] data = {
                "Select",
                "Software Engineer",
                "Doctor",
                "Teacher",
                "Business",
                "Government Job",
                "Private Job",
                "Farmer",
                "Shop Owner",
                "Self Employed",
                "Lawyer",
                "Chartered Accountant",
                "Banker",
                "Defence",
                "Police",
                "Professor",
                "Entrepreneur",
                "Housewife",
                "Student",
                "Unemployed",
                "Other"
        };
        spOccupation.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadSector() {
        String[] data = {
                "Select",
                "Private Sector",
                "Government Sector",
                "Public Sector",
                "Self Employed",
                "Business",
                "Not Working"
        };
        spSector.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadIncome() {
        String[] data = {
                "Select",
                "No Income",
                "Below 1 Lakh",
                "1 - 2 Lakh",
                "2 - 3 Lakh",
                "3 - 5 Lakh",
                "5 - 7 Lakh",
                "7 - 10 Lakh",
                "10 - 15 Lakh",
                "15 - 20 Lakh",
                "20 - 30 Lakh",
                "Above 30 Lakh"
        };
        spIncome.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadLanguage() {
        String[] data = {
                "Select",
                "Hindi",
                "English",
                "Gujarati",
                "Marathi",
                "Punjabi",
                "Urdu",
                "Bengali",
                "Tamil",
                "Telugu",
                "Kannada",
                "Malayalam"
        };
        spLanguage.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void validateAndNext() {
        if (spQualification.getSelectedItemPosition() == 0 ||
                spOccupation.getSelectedItemPosition() == 0 ||
                spSector.getSelectedItemPosition() == 0 ||
                spIncome.getSelectedItemPosition() == 0 ||
                spLanguage.getSelectedItemPosition() == 0 ||
                etCity.getText().toString().trim().isEmpty() ||
                etState.getText().toString().trim().isEmpty()) {

            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Education Details Saved", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(EducationDetailsActivity.this, FamilyDetailsActivity.class);
        startActivity(intent);
    }
}
