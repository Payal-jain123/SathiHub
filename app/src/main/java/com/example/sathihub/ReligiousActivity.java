package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

public class ReligiousActivity extends AppCompatActivity {

    Spinner spReligion, spCaste, spSubCaste, spMotherTongue, spManglik;
    EditText etGotra;
    Button btnContinue;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_religious);

        spReligion = findViewById(R.id.spReligion);
        spCaste = findViewById(R.id.spCaste);
        spSubCaste = findViewById(R.id.spSubCaste);
        spMotherTongue = findViewById(R.id.spMotherTongue);
        spManglik = findViewById(R.id.spManglik);
        etGotra = findViewById(R.id.etGotra);
        btnContinue = findViewById(R.id.btnContinue);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        loadReligion();
        loadMotherTongue();
        loadManglik();

        spReligion.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                String religion = spReligion.getSelectedItem().toString();
                loadCaste(religion);
            }
        });

        spCaste.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                String caste = spCaste.getSelectedItem().toString();
                loadSubCaste(caste);
            }
        });

        btnContinue.setOnClickListener(v -> validateAndSave());
    }

    // ================= LOAD SPINNERS =================

    private void loadReligion() {
        String[] religion = {
                "Select", "Hindu", "Muslim", "Jain", "Sikh",
                "Christian", "Buddhist", "Parsi", "Jewish", "Other"
        };

        spReligion.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, religion));
    }

    private void loadCaste(String religion) {
        ArrayList<String> caste = new ArrayList<>();
        caste.add("Select");

        if (religion.equals("Jain")) {
            caste.add("Digambar");
            caste.add("Shwetambar Murtipujak");
            caste.add("Shwetambar Sthanakvasi");
            caste.add("Shwetambar Terapanthi");
        }
        else if (religion.equals("Hindu")) {
            caste.add("Brahmin"); caste.add("Rajput"); caste.add("Jat");
            caste.add("Yadav"); caste.add("Maratha"); caste.add("Baniya");
        }
        else if (religion.equals("Muslim")) {
            caste.add("Sunni"); caste.add("Shia"); caste.add("Bohra");
        }
        else if (religion.equals("Sikh")) {
            caste.add("Jat Sikh"); caste.add("Khatri");
        }
        else if (religion.equals("Christian")) {
            caste.add("Roman Catholic"); caste.add("Protestant");
        }
        else {
            caste.add("Other");
        }

        spCaste.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, caste));
    }

    private void loadSubCaste(String caste) {
        ArrayList<String> sub = new ArrayList<>();
        sub.add("Select");

        // ================= JAIN LOGIC =================
        if (caste.equals("Digambar")) {
            sub.add("Agarwal");
            sub.add("Khandelwal");
            sub.add("Parwar");
            sub.add("Kasliwal");
            sub.add("Golapurab");
            sub.add("Humad");
        }
        else if (caste.equals("Shwetambar Murtipujak")) {
            sub.add("Oswal");
            sub.add("Porwal");
            sub.add("Shrimal");
            sub.add("Modh");
            sub.add("Visha Oswal");
            sub.add("Visha Porwal");
        }
        else if (caste.equals("Shwetambar Sthanakvasi")) {
            sub.add("Oswal");
            sub.add("Humad");
            sub.add("Charnagri");
            sub.add("Golapurab");
        }
        else if (caste.equals("Shwetambar Terapanthi")) {
            sub.add("Daga");
            sub.add("Bothra");
            sub.add("Dugar");
            sub.add("Surana");
            sub.add("Kothari");
            sub.add("Malu");
            sub.add("Rathi");
        }

        // ================= OTHER RELIGIONS =================
        else if (caste.equals("Brahmin")) {
            sub.add("Gaur"); sub.add("Maithil"); sub.add("Iyer");
        }
        else if (caste.equals("Rajput")) {
            sub.add("Sisodia"); sub.add("Rathore");
        }
        else if (caste.equals("Sunni")) {
            sub.add("Hanafi"); sub.add("Shafi");
        }
        else {
            sub.add("Not Applicable");
        }

        spSubCaste.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sub));
    }

    private void loadMotherTongue() {
        String[] lang = {
                "Select", "Hindi", "English", "Gujarati", "Marathi",
                "Punjabi", "Urdu", "Bengali", "Tamil", "Telugu"
        };

        spMotherTongue.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, lang));
    }

    private void loadManglik() {
        String[] manglik = {"Select", "Yes", "No"};
        spManglik.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, manglik));
    }

    // ================= SAVE DATA =================

    private void validateAndSave() {

        if (spReligion.getSelectedItemPosition() == 0 ||
                spCaste.getSelectedItemPosition() == 0 ||
                spSubCaste.getSelectedItemPosition() == 0 ||
                spMotherTongue.getSelectedItemPosition() == 0 ||
                spManglik.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference userReligiousRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("religious");
        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);

        String religion = spReligion.getSelectedItem().toString();
        String caste = spCaste.getSelectedItem().toString();
        String subCaste = spSubCaste.getSelectedItem().toString();
        String motherTongue = spMotherTongue.getSelectedItem().toString();
        String manglik = spManglik.getSelectedItem().toString();
        String gotra = etGotra.getText().toString().trim();

        Tasks.whenAllSuccess(
            userReligiousRef.child("religion").setValue(religion),
            userReligiousRef.child("caste").setValue(caste),
            userReligiousRef.child("subCaste").setValue(subCaste),
            userReligiousRef.child("motherTongue").setValue(motherTongue),
            userReligiousRef.child("manglik").setValue(manglik),
            userReligiousRef.child("gotra").setValue(gotra),
            personalRef.child("religion").setValue(religion),
            personalRef.child("caste").setValue(caste)
        ).addOnSuccessListener(results -> {
            Toast.makeText(this, "Religious Details Saved", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, EducationDetailsActivity.class));
            finish();
        }).addOnFailureListener(e ->
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
