package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sathihub.model.FamilyModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FamilyDetailsActivity extends AppCompatActivity {

    EditText etFatherOccupation, etMotherOccupation, etFamilyCity;
    Spinner spFamilyType, spBrothers, spSisters, spFamilyIncome, spFamilyStatus;
    Button btnNext;

    DatabaseReference reference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_details);

        etFatherOccupation = findViewById(R.id.etFatherOccupation);
        etMotherOccupation = findViewById(R.id.etMotherOccupation);
        etFamilyCity = findViewById(R.id.etFamilyCity);

        spFamilyType = findViewById(R.id.spFamilyType);
        spBrothers = findViewById(R.id.spBrothers);
        spSisters = findViewById(R.id.spSisters);
        spFamilyIncome = findViewById(R.id.spFamilyIncome);
        spFamilyStatus = findViewById(R.id.spFamilyStatus);

        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("users");

        loadFamilyType();
        loadBrothers();
        loadSisters();
        loadFamilyIncome();
        loadFamilyStatus();

        btnNext.setOnClickListener(v -> validateAndNext());
    }

    private void loadFamilyType() {
        String[] data = {"Select", "Joint Family", "Nuclear Family"};
        spFamilyType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadBrothers() {
        String[] data = {"Select", "0", "1", "2", "3", "4+"};
        spBrothers.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadSisters() {
        String[] data = {"Select", "0", "1", "2", "3", "4+"};
        spSisters.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadFamilyIncome() {
        String[] data = {
                "Select",
                "Below 3 Lakh",
                "3 - 5 Lakh",
                "5 - 10 Lakh",
                "10 - 20 Lakh",
                "20 - 30 Lakh",
                "Above 30 Lakh"
        };
        spFamilyIncome.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadFamilyStatus() {
        String[] data = {
                "Select",
                "Middle Class",
                "Upper Middle Class",
                "Rich",
                "Affluent"
        };
        spFamilyStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void validateAndNext() {

        if (etFatherOccupation.getText().toString().trim().isEmpty() ||
                etMotherOccupation.getText().toString().trim().isEmpty() ||
                etFamilyCity.getText().toString().trim().isEmpty() ||
                spFamilyType.getSelectedItemPosition() == 0 ||
                spBrothers.getSelectedItemPosition() == 0 ||
                spSisters.getSelectedItemPosition() == 0 ||
                spFamilyIncome.getSelectedItemPosition() == 0 ||
                spFamilyStatus.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Please fill all family details", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FamilyModel model = new FamilyModel(
                etFatherOccupation.getText().toString(),
                etMotherOccupation.getText().toString(),
                spFamilyType.getSelectedItem().toString(),
                spBrothers.getSelectedItem().toString(),
                spSisters.getSelectedItem().toString(),
                spFamilyIncome.getSelectedItem().toString(),
                etFamilyCity.getText().toString(),
                spFamilyStatus.getSelectedItem().toString()
        );

        reference.child(uid).child("family").setValue(model);

        Toast.makeText(this, "Family Details Saved", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(FamilyDetailsActivity.this, PartnerPreferenceActivity.class));
        finish();
    }
}
