package com.example.sathihub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    Spinner spGender, spReligion, spEducation, spMaritalStatus;
    EditText edtCity;
    Button btnSearch, btnClear;
    RecyclerView recyclerProfiles;
    ProgressBar progressBar;
    TextView tvEmpty, tvResultCount;

    FirebaseAuth auth;
    DatabaseReference usersRef, personalInfoRef, connectionRequestsRef, profileImageRef;

    List<SearchProfile> allProfiles = new ArrayList<>();
    List<SearchProfile> filteredList = new ArrayList<>();
    SearchAdapter adapter;

    String currentUid;
    DataSnapshot myPreferencesSnapshot = null;
    Set<String> acceptedUids = new HashSet<>();

    // ML Weights for search ranking
    private static final double W_AGE = 0.25;
    private static final double W_RELIGION = 0.20;
    private static final double W_CASTE = 0.15;
    private static final double W_EDUCATION = 0.15;
    private static final double W_LOCATION = 0.10;
    private static final double W_HEIGHT = 0.10;
    private static final double W_OCCUPATION = 0.05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        spGender = findViewById(R.id.spGender);
        spReligion = findViewById(R.id.spReligion);
        spEducation = findViewById(R.id.spEducation);
        spMaritalStatus = findViewById(R.id.spMaritalStatus);
        edtCity = findViewById(R.id.edtCity);
        btnSearch = findViewById(R.id.btnSearch);
        btnClear = findViewById(R.id.btnClear);
        recyclerProfiles = findViewById(R.id.recyclerProfiles);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvResultCount = findViewById(R.id.tvResultCount);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = auth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        personalInfoRef = FirebaseDatabase.getInstance().getReference("PersonalInfo");
        connectionRequestsRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests");
        profileImageRef = FirebaseDatabase.getInstance().getReference("ProfileImage");

        setupSpinners();
        setupRecyclerView();

        btnSearch.setOnClickListener(v -> applyFilters());
        btnClear.setOnClickListener(v -> clearFilters());

        // Auto-apply city filter as user types
        edtCity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Auto-load all profiles when opened
        loadConnectionsThenProfiles();
    }

    private void setupSpinners() {
        String[] genders = {"All Genders", "Male", "Female"};
        String[] religions = {"All Religions", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Other"};
        String[] educations = {"All Education", "10th", "12th", "Diploma", "ITI", "Graduate", "Post Graduate", "MBA", "M.Tech", "PhD", "Doctor", "Engineer", "CA", "CS", "Other"};
        String[] marital = {"All Status", "Unmarried", "Divorced", "Widowed", "Separated"};

        spGender.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, genders));
        spReligion.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, religions));
        spEducation.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, educations));
        spMaritalStatus.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, marital));

        // Auto-apply filter when any spinner changes
        android.widget.AdapterView.OnItemSelectedListener filterListener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };

        spGender.setOnItemSelectedListener(filterListener);
        spReligion.setOnItemSelectedListener(filterListener);
        spEducation.setOnItemSelectedListener(filterListener);
        spMaritalStatus.setOnItemSelectedListener(filterListener);
    }

    private void setupRecyclerView() {
        recyclerProfiles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(filteredList);
        recyclerProfiles.setAdapter(adapter);
    }

    // ===================== LOAD REAL PROFILES FROM FIREBASE =====================

    private void loadConnectionsThenProfiles() {
        showLoading(true);
        acceptedUids.clear();

        connectionRequestsRef.child(currentUid).get().addOnSuccessListener(connSnap -> {
            for (DataSnapshot child : connSnap.getChildren()) {
                if ("accepted".equals(child.getValue(String.class))) {
                    String otherUid = child.getKey();
                    if (otherUid != null) acceptedUids.add(otherUid);
                }
            }
            loadAllProfiles();
        }).addOnFailureListener(e -> loadAllProfiles());
    }

    private void loadAllProfiles() {
        usersRef.child(currentUid).child("partnerPreference").get()
                .addOnSuccessListener(prefSnap -> {
                    myPreferencesSnapshot = prefSnap;
                    fetchProfilesFromFirebase();
                })
                .addOnFailureListener(e -> fetchProfilesFromFirebase());
    }

    private void fetchProfilesFromFirebase() {
        usersRef.get().addOnSuccessListener(usersSnapshot -> {
            personalInfoRef.get().addOnSuccessListener(personalSnapshot -> {
                profileImageRef.get().addOnSuccessListener(imageSnapshot -> {
                    allProfiles.clear();

                    for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                        String otherUid = userSnap.getKey();
                        if (otherUid == null || otherUid.equals(currentUid)) continue;

                        // Skip accepted connections - they only show in Chats
                        if (acceptedUids.contains(otherUid)) continue;

                        Boolean completed = userSnap.child("profileCompleted").getValue(Boolean.class);
                        if (completed == null || !completed) continue;

                        DataSnapshot otherPersonal = personalSnapshot.child(otherUid);
                        if (!otherPersonal.exists()) continue;

                        String name = safeString(otherPersonal.child("name").getValue(String.class));
                        if (name.isEmpty()) name = safeString(userSnap.child("name").getValue(String.class));

                        SearchProfile profile = new SearchProfile();
                        profile.uid = otherUid;
                        profile.name = name;
                        profile.occupation = safeString(otherPersonal.child("occupation").getValue(String.class));
                        profile.education = safeString(otherPersonal.child("education").getValue(String.class));
                        profile.religion = safeString(otherPersonal.child("religion").getValue(String.class));
                        profile.height = safeString(otherPersonal.child("height").getValue(String.class));
                        profile.maritalStatus = safeString(otherPersonal.child("maritalStatus").getValue(String.class));
                        profile.gender = safeString(otherPersonal.child("gender").getValue(String.class));
                        profile.age = safeString(otherPersonal.child("age").getValue(String.class));
                        profile.city = safeString(otherPersonal.child("city").getValue(String.class));
                        profile.photoUrl = safeString(otherPersonal.child("imageUrl").getValue(String.class));
                        if (profile.photoUrl.isEmpty()) {
                            profile.photoUrl = safeString(imageSnapshot.child(otherUid).child("imageUrl").getValue(String.class));
                        }

                        // Get income from education node if available
                        profile.income = safeString(userSnap.child("education").child("income").getValue(String.class));
                        if (profile.income.isEmpty()) {
                            profile.income = safeString(userSnap.child("family").child("familyIncome").getValue(String.class));
                        }

                        // Calculate trust score (fake profile detection)
                        profile.trustScore = calculateTrustScore(otherPersonal, imageSnapshot.child(otherUid), userSnap);
                        profile.verificationStatus = safeString(userSnap.child("verificationStatus").getValue(String.class));
                        if (profile.verificationStatus.isEmpty()) {
                            profile.verificationStatus = safeString(otherPersonal.child("verificationStatus").getValue(String.class));
                        }

                        // Calculate ML relevance score for ranking
                        profile.relevanceScore = calculateRelevanceScore(otherPersonal);

                        allProfiles.add(profile);
                    }

                    // Sort by relevance score (highest first)
                    Collections.sort(allProfiles, (a, b) -> Double.compare(b.relevanceScore, a.relevanceScore));

                    showLoading(false);
                    applyFilters();

                }).addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
        });
    }

    // ===================== ML RELEVANCE SCORING =====================

    private double calculateRelevanceScore(DataSnapshot otherPersonal) {
        if (myPreferencesSnapshot == null || !myPreferencesSnapshot.exists()) return 0;

        double total = 0;

        total += scoreAge(
                safeString(otherPersonal.child("age").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("ageRange").getValue(String.class))) * W_AGE;

        total += scoreExact(
                safeString(otherPersonal.child("religion").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("religion").getValue(String.class))) * W_RELIGION;

        total += scoreExact(
                safeString(otherPersonal.child("caste").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("caste").getValue(String.class))) * W_CASTE;

        total += scoreFuzzy(
                safeString(otherPersonal.child("education").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("education").getValue(String.class))) * W_EDUCATION;

        total += scoreLocation(
                safeString(otherPersonal.child("city").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("location").getValue(String.class))) * W_LOCATION;

        total += scoreHeight(
                safeString(otherPersonal.child("height").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("heightRange").getValue(String.class))) * W_HEIGHT;

        total += scoreFuzzy(
                safeString(otherPersonal.child("occupation").getValue(String.class)),
                safeString(myPreferencesSnapshot.child("occupation").getValue(String.class))) * W_OCCUPATION;

        return total;
    }

    // ===================== SCORING HELPERS =====================

    private double scoreAge(String ageStr, String prefRange) {
        if (ageStr.isEmpty() || prefRange.isEmpty() || prefRange.equals("Select")) return 0;
        try {
            int age = Integer.parseInt(ageStr.replaceAll("[^0-9]", ""));
            int[] range = parseIntRange(prefRange);
            if (range == null) return 0;
            if (age >= range[0] && age <= range[1]) return 1.0;
            int dist = Math.min(Math.abs(age - range[0]), Math.abs(age - range[1]));
            if (dist <= 3) return 0.5;
            return 0;
        } catch (Exception e) { return 0; }
    }

    private double scoreHeight(String hStr, String prefRange) {
        if (hStr.isEmpty() || prefRange.isEmpty() || prefRange.equals("Select")) return 0;
        try {
            double hInch = heightToInches(hStr);
            double[] range = parseHeightRange(prefRange);
            if (range == null) return 0;
            if (hInch >= range[0] && hInch <= range[1]) return 1.0;
            double dist = Math.min(Math.abs(hInch - range[0]), Math.abs(hInch - range[1]));
            if (dist <= 2) return 0.5;
            return 0;
        } catch (Exception e) { return 0; }
    }

    private double scoreExact(String val, String pref) {
        if (val.isEmpty() || pref.isEmpty() || pref.equals("Select")) return 0;
        return val.equalsIgnoreCase(pref) ? 1.0 : 0;
    }

    private double scoreFuzzy(String val, String pref) {
        if (val.isEmpty() || pref.isEmpty() || pref.equals("Select")) return 0;
        if (val.equalsIgnoreCase(pref)) return 1.0;
        String v = val.toLowerCase(), p = pref.toLowerCase();
        if (v.contains(p) || p.contains(v)) return 0.5;
        return 0;
    }

    private double scoreLocation(String city, String prefLoc) {
        if (city.isEmpty() || prefLoc.isEmpty()) return 0;
        String c = city.toLowerCase(), p = prefLoc.toLowerCase();
        if (c.equals(p)) return 1.0;
        if (c.contains(p) || p.contains(c)) return 0.5;
        return 0;
    }

    private int[] parseIntRange(String rangeStr) {
        try {
            if (rangeStr.contains("+")) {
                int min = Integer.parseInt(rangeStr.replaceAll("[^0-9]", ""));
                return new int[]{min, 99};
            }
            String[] p = rangeStr.replaceAll("[^0-9\\-]", "").split("-");
            if (p.length == 2) return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
        } catch (Exception ignored) {}
        return null;
    }

    private double heightToInches(String h) {
        try {
            h = h.toLowerCase().replace("ft", "'").replace("in", "").trim();
            String[] p = h.split("'");
            if (p.length >= 2) {
                int ft = Integer.parseInt(p[0].trim());
                int inch = Integer.parseInt(p[1].trim().replaceAll("[^0-9]", ""));
                return ft * 12.0 + inch;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private double[] parseHeightRange(String rangeStr) {
        try {
            if (rangeStr.contains("+")) {
                double min = heightToInches(rangeStr.replace("+", "").trim());
                return new double[]{min, 99 * 12};
            }
            String[] p = rangeStr.split("-");
            if (p.length == 2) {
                return new double[]{heightToInches(p[0].trim()), heightToInches(p[1].trim())};
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ===================== FILTERING =====================

    private void applyFilters() {
        filteredList.clear();

        String genderFilter = spGender.getSelectedItem().toString();
        String religionFilter = spReligion.getSelectedItem().toString();
        String educationFilter = spEducation.getSelectedItem().toString();
        String maritalFilter = spMaritalStatus.getSelectedItem().toString();
        String cityFilter = edtCity.getText().toString().trim().toLowerCase();

        for (SearchProfile profile : allProfiles) {
            if (!genderFilter.equals("All Genders") && !genderFilter.equalsIgnoreCase(profile.gender)) continue;
            if (!religionFilter.equals("All Religions") && !religionFilter.equalsIgnoreCase(profile.religion)) continue;
            if (!educationFilter.equals("All Education") && !educationFilter.equalsIgnoreCase(profile.education)) continue;
            if (!maritalFilter.equals("All Status") && !maritalFilter.equalsIgnoreCase(profile.maritalStatus)) continue;
            if (!cityFilter.isEmpty()) {
                if (profile.city.isEmpty() || !profile.city.toLowerCase().contains(cityFilter)) continue;
            }
            filteredList.add(profile);
        }

        adapter.notifyDataSetChanged();
        tvResultCount.setText(filteredList.size() + " profiles found");

        if (filteredList.isEmpty()) {
            recyclerProfiles.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerProfiles.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void clearFilters() {
        spGender.setSelection(0);
        spReligion.setSelection(0);
        spEducation.setSelection(0);
        spMaritalStatus.setSelection(0);
        edtCity.setText("");
        applyFilters();
    }

    // ===================== UTILS =====================

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerProfiles.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @NonNull
    private String safeString(String s) {
        return s != null ? s : "";
    }

    // ===================== INNER MODEL CLASS =====================

    static class SearchProfile {
        String uid;
        String name;
        String occupation;
        String education;
        String religion;
        String height;
        String income;
        String maritalStatus;
        String gender;
        String age;
        String city;
        String photoUrl;
        double relevanceScore;
        int trustScore;
        String verificationStatus;
    }

    // ===================== INNER ADAPTER (Programmatic Views) =====================

    static class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

        List<SearchProfile> list;

        SearchAdapter(List<SearchProfile> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Build white card row programmatically
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.card_bg);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(16, 12, 16, 12);
            card.setLayoutParams(cardParams);
            card.setPadding(20, 20, 20, 20);

            // Profile Image
            ImageView img = new ImageView(parent.getContext());
            img.setLayoutParams(new LinearLayout.LayoutParams(90, 90));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(R.drawable.ic_profile);
            card.addView(img);

            // Info column
            LinearLayout infoCol = new LinearLayout(parent.getContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoParams.setMargins(20, 0, 12, 0);
            infoCol.setLayoutParams(infoParams);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(17);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF000000);
            infoCol.addView(tvName);

            TextView tvVerifiedBadge = new TextView(parent.getContext());
            tvVerifiedBadge.setTextSize(10);
            tvVerifiedBadge.setTextColor(0xFFFFFFFF);
            tvVerifiedBadge.setPadding(6, 2, 6, 2);
            tvVerifiedBadge.setVisibility(View.GONE);
            infoCol.addView(tvVerifiedBadge);

            TextView tvDetails = new TextView(parent.getContext());
            tvDetails.setTextSize(13);
            tvDetails.setTextColor(0xFF555555);
            tvDetails.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            infoCol.addView(tvDetails);

            TextView tvTrustWarning = new TextView(parent.getContext());
            tvTrustWarning.setTextSize(11);
            tvTrustWarning.setTypeface(null, Typeface.BOLD);
            tvTrustWarning.setPadding(0, 4, 0, 0);
            tvTrustWarning.setVisibility(View.GONE);
            infoCol.addView(tvTrustWarning);

            card.addView(infoCol);

            // Relevance score badge
            TextView tvScore = new TextView(parent.getContext());
            tvScore.setTextSize(13);
            tvScore.setTypeface(null, Typeface.BOLD);
            tvScore.setPadding(8, 4, 8, 4);
            LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scoreParams.setMargins(0, 0, 0, 0);
            tvScore.setLayoutParams(scoreParams);
            card.addView(tvScore);

            return new ViewHolder(card, img, tvName, tvVerifiedBadge, tvDetails, tvTrustWarning, tvScore);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            SearchProfile p = list.get(position);

            h.tvName.setText((p.name.isEmpty() ? "Unknown" : p.name));

            // Verification badge
            if ("verified".equals(p.verificationStatus)) {
                h.tvVerifiedBadge.setVisibility(View.VISIBLE);
                h.tvVerifiedBadge.setText("Verified");
                h.tvVerifiedBadge.setBackgroundColor(0xFF4CAF50);
            } else if ("pending".equals(p.verificationStatus)) {
                h.tvVerifiedBadge.setVisibility(View.VISIBLE);
                h.tvVerifiedBadge.setText("Pending");
                h.tvVerifiedBadge.setBackgroundColor(0xFFFF9800);
            } else {
                h.tvVerifiedBadge.setVisibility(View.GONE);
            }

            // Trust warning (fake profile detection)
            if (p.trustScore < 40) {
                h.tvTrustWarning.setVisibility(View.VISIBLE);
                h.tvTrustWarning.setText("Low Trust Profile");
                h.tvTrustWarning.setTextColor(0xFFF44336);
            } else if (p.trustScore < 60) {
                h.tvTrustWarning.setVisibility(View.VISIBLE);
                h.tvTrustWarning.setText("New Profile");
                h.tvTrustWarning.setTextColor(0xFFFF9800);
            } else {
                h.tvTrustWarning.setVisibility(View.GONE);
            }

            StringBuilder details = new StringBuilder();
            if (!p.age.isEmpty()) details.append(p.age).append(" yrs");
            if (!p.city.isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append(p.city);
            }
            if (!p.occupation.isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append(p.occupation);
            }
            if (!p.education.isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append(p.education);
            }
            if (!p.religion.isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append(p.religion);
            }
            h.tvDetails.setText(details.toString());

            // Score color and text
            double score = p.relevanceScore * 100;
            int color;
            if (score >= 70) color = 0xFF2E7D32;
            else if (score >= 40) color = 0xFFF57F17;
            else color = 0xFF888888;
            h.tvScore.setTextColor(color);
            h.tvScore.setText(String.format("%.0f%%", score));

            // Photo
            if (p.photoUrl != null && !p.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext())
                        .load(p.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(h.itemView.getContext(), ProfileActivity.class);
                i.putExtra("targetUid", p.uid);
                h.itemView.getContext().startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName, tvVerifiedBadge, tvDetails, tvTrustWarning, tvScore;

            ViewHolder(View itemView, ImageView img, TextView tvName, TextView tvVerifiedBadge,
                       TextView tvDetails, TextView tvTrustWarning, TextView tvScore) {
                super(itemView);
                this.img = img;
                this.tvName = tvName;
                this.tvVerifiedBadge = tvVerifiedBadge;
                this.tvDetails = tvDetails;
                this.tvTrustWarning = tvTrustWarning;
                this.tvScore = tvScore;
            }
        }
    }
}
