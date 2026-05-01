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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FamilyDetailsActivity extends AppCompatActivity {

    EditText etFatherOccupation, etMotherOccupation, etFamilyState; // 🔥 city → state
    Spinner spFamilyType, spBrothers, spSisters, spFamilyIncome, spFamilyStatus;
    Button btnNext;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_details);

        etFatherOccupation = findViewById(R.id.etFatherOccupation);
        etMotherOccupation = findViewById(R.id.etMotherOccupation);
        etFamilyState = findViewById(R.id.etFamilyCity); // XML id same hai, but logically STATE

        spFamilyType = findViewById(R.id.spFamilyType);
        spBrothers = findViewById(R.id.spBrothers);
        spSisters = findViewById(R.id.spSisters);
        spFamilyIncome = findViewById(R.id.spFamilyIncome);
        spFamilyStatus = findViewById(R.id.spFamilyStatus);

        btnNext = findViewById(R.id.btnNext);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFamilyType();
        loadBrothers();
        loadSisters();
        loadFamilyIncome();
        loadFamilyStatus();

        loadExistingData(); // ✅ for Edit Profile

        btnNext.setOnClickListener(v -> validateAndNext());
    }

    private void loadExistingData() {
        String uid = auth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("family")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        etFatherOccupation.setText(snapshot.child("fatherOccupation").getValue(String.class));
                        etMotherOccupation.setText(snapshot.child("motherOccupation").getValue(String.class));
                        etFamilyState.setText(snapshot.child("familyState").getValue(String.class)); // 🔥 state

                        setSpinner(spFamilyType, snapshot.child("familyType").getValue(String.class));
                        setSpinner(spBrothers, snapshot.child("brothers").getValue(String.class));
                        setSpinner(spSisters, snapshot.child("sisters").getValue(String.class));
                        setSpinner(spFamilyIncome, snapshot.child("familyIncome").getValue(String.class));
                        setSpinner(spFamilyStatus, snapshot.child("familyStatus").getValue(String.class));
                    }
                });
    }

    private void setSpinner(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int pos = adapter.getPosition(value);
        if (pos >= 0) spinner.setSelection(pos);
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
                etFamilyState.getText().toString().trim().isEmpty() ||
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
                etFatherOccupation.getText().toString().trim(),
                etMotherOccupation.getText().toString().trim(),
                spFamilyType.getSelectedItem().toString(),
                spBrothers.getSelectedItem().toString(),
                spSisters.getSelectedItem().toString(),
                spFamilyIncome.getSelectedItem().toString(),
                etFamilyState.getText().toString().trim(),
                spFamilyStatus.getSelectedItem().toString()
        );

        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);

        FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("family")
                .setValue(model)
                .addOnSuccessListener(unused -> {
                    Tasks.whenAllSuccess(
                        personalRef.child("fatherOccupation").setValue(etFatherOccupation.getText().toString().trim()),
                        personalRef.child("motherOccupation").setValue(etMotherOccupation.getText().toString().trim())
                    ).addOnSuccessListener(results2 -> {
                        Toast.makeText(this, "Family Details Saved", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, PartnerPreferenceActivity.class));
                        finish();
                    }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
