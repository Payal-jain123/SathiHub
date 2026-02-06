package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ReligiousActivity extends AppCompatActivity {

    Spinner spReligion, spCaste, spSubCaste, spMotherTongue, spManglik;
    EditText etGotra;
    Button btnContinue;

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

        btnContinue.setOnClickListener(v -> validateAndNext());
    }

    private void loadReligion() {
        String[] religion = {
                "Select",
                "Hindu",
                "Muslim",
                "Jain",
                "Sikh",
                "Christian",
                "Buddhist",
                "Parsi",
                "Jewish",
                "Other"
        };

        spReligion.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, religion));
    }

    private void loadCaste(String religion) {
        ArrayList<String> caste = new ArrayList<>();
        caste.add("Select");

        if (religion.equals("Hindu")) {
            caste.add("Brahmin");
            caste.add("Rajput");
            caste.add("Kshatriya");
            caste.add("Vaishya");
            caste.add("Kayastha");
            caste.add("Agarwal");
            caste.add("Jat");
            caste.add("Yadav");
            caste.add("Kurmi");
            caste.add("Maratha");
            caste.add("Baniya");
            caste.add("SC");
            caste.add("ST");
            caste.add("OBC");
        }
        else if (religion.equals("Muslim")) {
            caste.add("Sunni");
            caste.add("Shia");
            caste.add("Bohra");
            caste.add("Khoja");
            caste.add("Pathan");
            caste.add("Sheikh");
            caste.add("Syed");
        }
        else if (religion.equals("Jain")) {
            caste.add("Digambar");
            caste.add("Shwetambar");
            caste.add("Sthanakvasi");
            caste.add("Terapanthi");
        }
        else if (religion.equals("Sikh")) {
            caste.add("Jat Sikh");
            caste.add("Khatri");
            caste.add("Arora");
            caste.add("Ramgarhia");
            caste.add("Mazhabhi");
        }
        else if (religion.equals("Christian")) {
            caste.add("Roman Catholic");
            caste.add("Protestant");
            caste.add("Syrian Catholic");
            caste.add("Syrian Orthodox");
            caste.add("Born Again");
        }
        else if (religion.equals("Buddhist")) {
            caste.add("Mahayana");
            caste.add("Theravada");
            caste.add("Navayana");
        }
        else if (religion.equals("Parsi")) {
            caste.add("Irani");
            caste.add("Parsi Zoroastrian");
        }
        else if (religion.equals("Jewish")) {
            caste.add("Ashkenazi");
            caste.add("Sephardi");
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

        if (caste.equals("Brahmin")) {
            sub.add("Gaur");
            sub.add("Saraswat");
            sub.add("Maithil");
            sub.add("Iyer");
            sub.add("Iyengar");
            sub.add("Kanyakubj");
            sub.add("Bhumihar");
        }
        else if (caste.equals("Rajput")) {
            sub.add("Sisodia");
            sub.add("Rathore");
            sub.add("Chauhan");
            sub.add("Solanki");
            sub.add("Tomar");
        }
        else if (caste.equals("Jat")) {
            sub.add("Jat Hindu");
            sub.add("Jat Sikh");
        }
        else if (caste.equals("Yadav")) {
            sub.add("Ahir");
            sub.add("Gwala");
        }
        else if (caste.equals("Maratha")) {
            sub.add("96 Kuli");
            sub.add("Kunbi");
        }
        else if (caste.equals("Sunni")) {
            sub.add("Hanafi");
            sub.add("Shafi");
        }
        else if (caste.equals("Shia")) {
            sub.add("Ithna Ashari");
            sub.add("Ismaili");
        }
        else if (caste.equals("Digambar")) {
            sub.add("Agarwal Jain");
            sub.add("Khandelwal Jain");
        }
        else if (caste.equals("Shwetambar")) {
            sub.add("Murtipujak");
            sub.add("Sthanakvasi");
        }
        else if (caste.equals("Roman Catholic")) {
            sub.add("Goan Catholic");
            sub.add("East Indian Catholic");
        }
        else if (caste.equals("Syrian Catholic")) {
            sub.add("Syro Malabar");
            sub.add("Syro Malankara");
        }
        else {
            sub.add("Not Applicable");
        }

        spSubCaste.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sub));
    }

    private void loadMotherTongue() {
        String[] lang = {
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

        spMotherTongue.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, lang));
    }

    private void loadManglik() {
        String[] manglik = {"Select", "Yes", "No"};
        spManglik.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, manglik));
    }

    private void validateAndNext() {
        if (spReligion.getSelectedItemPosition() == 0 ||
                spCaste.getSelectedItemPosition() == 0 ||
                spSubCaste.getSelectedItemPosition() == 0 ||
                spMotherTongue.getSelectedItemPosition() == 0 ||
                spManglik.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ReligiousActivity.this, EducationDetailsActivity.class);
        startActivity(intent);
    }
}
