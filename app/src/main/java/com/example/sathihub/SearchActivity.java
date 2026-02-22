package com.example.sathihub;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sathihub.adapter.ProfileAdapter;
import com.example.sathihub.model.ProfileModel;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {

    Spinner spFilterType, spGender;
    EditText edtFilterValue;
    Button btnSearch;
    RecyclerView recyclerProfiles;

    ArrayList<ProfileModel> fullList = new ArrayList<>();
    ArrayList<ProfileModel> filteredList = new ArrayList<>();
    ProfileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        spFilterType = findViewById(R.id.spFilterType);
        edtFilterValue = findViewById(R.id.edtFilterValue);
        spGender = findViewById(R.id.spGender);
        btnSearch = findViewById(R.id.btnSearch);
        recyclerProfiles = findViewById(R.id.recyclerProfiles);

        String[] filterTypes = {
                "Profession", "Education", "Religion",
                "Height", "Income", "Marital Status"
        };

        String[] genders = {"All", "Male", "Female"};

        spFilterType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filterTypes));

        spGender.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, genders));

        recyclerProfiles.setLayoutManager(new LinearLayoutManager(this));

        // Dummy Data
        fullList.add(new ProfileModel("Rahul","Engineer","B.Tech","Hindu","170","5LPA","Single","Male"));
        fullList.add(new ProfileModel("Anjali","Teacher","M.A","Hindu","160","4LPA","Single","Female"));
        fullList.add(new ProfileModel("Amit","Doctor","MBBS","Muslim","175","8LPA","Divorced","Male"));
        fullList.add(new ProfileModel("Priya","Designer","B.Des","Christian","158","6LPA","Single","Female"));

        adapter = new ProfileAdapter(filteredList, model -> {});
        recyclerProfiles.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> applyFilter());
    }

    private void applyFilter() {
        String filterType = spFilterType.getSelectedItem().toString();
        String filterValue = edtFilterValue.getText().toString().trim().toLowerCase();
        String gender = spGender.getSelectedItem().toString();

        filteredList.clear();

        for (ProfileModel model : fullList) {

            boolean matchGender = gender.equals("All") ||
                    model.getGender().equalsIgnoreCase(gender);

            boolean matchFilter = false;

            switch (filterType) {
                case "Profession":
                    matchFilter = model.getProfession().toLowerCase().contains(filterValue);
                    break;
                case "Education":
                    matchFilter = model.getEducation().toLowerCase().contains(filterValue);
                    break;
                case "Religion":
                    matchFilter = model.getReligion().toLowerCase().contains(filterValue);
                    break;
                case "Height":
                    matchFilter = model.getHeight().equals(filterValue);
                    break;
                case "Income":
                    matchFilter = model.getIncome().toLowerCase().contains(filterValue);
                    break;
                case "Marital Status":
                    matchFilter = model.getMaritalStatus().toLowerCase().contains(filterValue);
                    break;
            }

            if (matchGender && matchFilter) {
                filteredList.add(model);
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No match found", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged();
    }
}
