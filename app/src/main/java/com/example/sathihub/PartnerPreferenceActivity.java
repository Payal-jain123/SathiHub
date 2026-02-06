package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sathihub.model.PartnerPreferenceModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PartnerPreferenceActivity extends AppCompatActivity {

    Spinner spAgeRange, spHeightRange, spReligion, spCaste, spEducation, spOccupation;
    EditText etLocation;
    Button btnSubmit;

    DatabaseReference reference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_preference);

        spAgeRange = findViewById(R.id.spAgeRange);
        spHeightRange = findViewById(R.id.spHeightRange);
        spReligion = findViewById(R.id.spReligion);
        spCaste = findViewById(R.id.spCaste);
        spEducation = findViewById(R.id.spEducation);
        spOccupation = findViewById(R.id.spOccupation);
        etLocation = findViewById(R.id.etLocation);
        btnSubmit = findViewById(R.id.btnSubmit);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("users");

        loadAgeRange();
        loadHeightRange();
        loadReligion();
        loadCaste();
        loadEducation();
        loadOccupation();

        btnSubmit.setOnClickListener(v -> validateAndSave());
    }

    private void loadAgeRange() {
        String[] data = {"Select","18 - 22","23 - 26","27 - 30","31 - 35","36 - 40","40+"};
        spAgeRange.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadHeightRange() {
        String[] data = {"Select","4'5 - 5'0","5'1 - 5'5","5'6 - 6'0","6'0+"};
        spHeightRange.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadReligion() {
        String[] data = {"Select","Hindu","Muslim","Christian","Sikh","Jain","Buddhist","Other"};
        spReligion.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadCaste() {
        String[] data = {"Select","General","OBC","SC","ST","Other"};
        spCaste.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadEducation() {
        String[] data = {"Select","10th","12th","Diploma","Graduate","Post Graduate","Doctor","Engineer","CA","MBA","PhD","Other"};
        spEducation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void loadOccupation() {
        String[] data = {"Select","Software Engineer","Doctor","Teacher","Business","Government Job","Private Job","Farmer","Self Employed","Housewife","Other"};
        spOccupation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, data));
    }

    private void validateAndSave() {

        if (spAgeRange.getSelectedItemPosition() == 0 ||
                spHeightRange.getSelectedItemPosition() == 0 ||
                spReligion.getSelectedItemPosition() == 0 ||
                spCaste.getSelectedItemPosition() == 0 ||
                spEducation.getSelectedItemPosition() == 0 ||
                spOccupation.getSelectedItemPosition() == 0 ||
                etLocation.getText().toString().trim().isEmpty()) {

            Toast.makeText(this, "Please fill all partner preference fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if(auth.getCurrentUser() == null){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        PartnerPreferenceModel model = new PartnerPreferenceModel(
                spAgeRange.getSelectedItem().toString(),
                spHeightRange.getSelectedItem().toString(),
                spReligion.getSelectedItem().toString(),
                spCaste.getSelectedItem().toString(),
                etLocation.getText().toString(),
                spEducation.getSelectedItem().toString(),
                spOccupation.getSelectedItem().toString()
        );

        reference.child(uid).child("partnerPreference").setValue(model);

        Toast.makeText(this, "Partner Preference Saved", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(PartnerPreferenceActivity.this, UploadPhotoActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
