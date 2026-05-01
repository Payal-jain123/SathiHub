package com.example.sathihub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class MatchesActivity extends AppCompatActivity {

    RecyclerView recyclerMatches;
    ProgressBar progressBar;
    TextView tvEmpty, tvResultCount;
    Spinner spFilterReligion, spFilterMinScore;

    FirebaseAuth auth;
    DatabaseReference usersRef, personalInfoRef, connectionRequestsRef, profileImageRef;

    MatchAdapter matchAdapter;
    List<MatchResult> allMatches = new ArrayList<>();
    List<MatchResult> filteredMatches = new ArrayList<>();

    String currentUid;
    String currentUserGender = "";
    DataSnapshot myPreferencesSnapshot = null;

    Set<String> acceptedUids = new HashSet<>();
    Set<String> pendingUids = new HashSet<>();

    // ========== ML FEATURE WEIGHTS (Content-Based Recommendation) ==========
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
        setContentView(R.layout.activity_matches);

        recyclerMatches = findViewById(R.id.recyclerMatches);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvResultCount = findViewById(R.id.tvResultCount);
        spFilterReligion = findViewById(R.id.spFilterReligion);
        spFilterMinScore = findViewById(R.id.spFilterMinScore);

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

        setupRecyclerView();
        setupFilters();
        loadConnectionsThenMatches();
    }

    private void setupRecyclerView() {
        recyclerMatches.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter(filteredMatches, pendingUids);
        recyclerMatches.setAdapter(matchAdapter);
    }

    private void setupFilters() {
        String[] religions = {"All Religions", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Other"};
        String[] minScores = {"Any Match %", "90%+", "80%+", "70%+", "60%+", "50%+"};

        spFilterReligion.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, religions));
        spFilterMinScore.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item_black, minScores));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spFilterReligion.setOnItemSelectedListener(listener);
        spFilterMinScore.setOnItemSelectedListener(listener);
    }

    // ===================== LOAD DATA FROM FIREBASE =====================

    private void loadConnectionsThenMatches() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerMatches.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        acceptedUids.clear();
        pendingUids.clear();

        connectionRequestsRef.child(currentUid).get().addOnSuccessListener(connSnap -> {
            for (DataSnapshot child : connSnap.getChildren()) {
                String status = child.getValue(String.class);
                String otherUid = child.getKey();
                if (otherUid == null) continue;
                if ("accepted".equals(status)) {
                    acceptedUids.add(otherUid);
                } else if ("pending".equals(status)) {
                    pendingUids.add(otherUid);
                }
            }
            loadMatches();
        }).addOnFailureListener(e -> loadMatches());
    }

    private void loadMatches() {
        personalInfoRef.child(currentUid).get().addOnSuccessListener(myPersonal -> {
            if (myPersonal.exists()) {
                currentUserGender = safeString(myPersonal.child("gender").getValue(String.class));
            }
            usersRef.child(currentUid).child("partnerPreference").get()
                    .addOnSuccessListener(prefSnap -> {
                        myPreferencesSnapshot = prefSnap;
                        fetchAllUsers();
                    })
                    .addOnFailureListener(e -> showError("Failed to load preferences"));
        }).addOnFailureListener(e -> showError("Failed to load profile"));
    }

    private void fetchAllUsers() {
        usersRef.get().addOnSuccessListener(usersSnapshot -> {
            personalInfoRef.get().addOnSuccessListener(personalSnapshot -> {
                profileImageRef.get().addOnSuccessListener(imageSnapshot -> {
                    allMatches.clear();

                    for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                        String otherUid = userSnap.getKey();
                        if (otherUid == null || otherUid.equals(currentUid)) continue;

                        // Skip accepted connections - they only show in Chats
                        if (acceptedUids.contains(otherUid)) continue;

                        Boolean completed = userSnap.child("profileCompleted").getValue(Boolean.class);
                        if (completed == null || !completed) continue;

                        DataSnapshot otherPersonal = personalSnapshot.child(otherUid);
                        if (!otherPersonal.exists()) continue;

                        String otherGender = safeString(otherPersonal.child("gender").getValue(String.class));
                        if (!isOppositeGender(currentUserGender, otherGender)) continue;

                        double score = mlCompatibilityScore(otherPersonal, userSnap);
                        if (score < 20) continue;

                        String photoUrl = safeString(otherPersonal.child("imageUrl").getValue(String.class));
                        if (photoUrl.isEmpty()) {
                            photoUrl = safeString(imageSnapshot.child(otherUid).child("imageUrl").getValue(String.class));
                        }

                        // Calculate trust score for fake profile detection
                        int trustScore = calculateTrustScore(otherPersonal, imageSnapshot.child(otherUid), userSnap);
                        String verStatus = safeString(userSnap.child("verificationStatus").getValue(String.class));
                        if (verStatus.isEmpty()) {
                            verStatus = safeString(otherPersonal.child("verificationStatus").getValue(String.class));
                        }

                        MatchResult match = new MatchResult(
                                otherUid,
                                safeString(otherPersonal.child("name").getValue(String.class)),
                                safeString(otherPersonal.child("age").getValue(String.class)),
                                safeString(otherPersonal.child("height").getValue(String.class)),
                                safeString(otherPersonal.child("religion").getValue(String.class)),
                                safeString(otherPersonal.child("caste").getValue(String.class)),
                                safeString(otherPersonal.child("education").getValue(String.class)),
                                safeString(otherPersonal.child("occupation").getValue(String.class)),
                                safeString(otherPersonal.child("city").getValue(String.class)),
                                safeString(otherPersonal.child("gender").getValue(String.class)),
                                photoUrl,
                                score,
                                trustScore,
                                verStatus
                        );
                        allMatches.add(match);
                    }

                    Collections.sort(allMatches, (a, b) -> Double.compare(b.score, a.score));
                    progressBar.setVisibility(View.GONE);
                    applyFilters();

                }).addOnFailureListener(e -> showError("Failed to load images"));
            }).addOnFailureListener(e -> showError("Failed to load profiles"));
        }).addOnFailureListener(e -> showError("Failed to load users"));
    }

    // ===================== ML COMPATIBILITY SCORER =====================

    private double mlCompatibilityScore(DataSnapshot otherPersonal, DataSnapshot otherUser) {
        if (myPreferencesSnapshot == null || !myPreferencesSnapshot.exists()) return 50.0;

        double total = 0.0;

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

        return Math.min(total * 100.0, 100.0);
    }

    // ===================== ML SCORING HELPERS =====================

    private double scoreAge(String ageStr, String prefRange) {
        if (ageStr.isEmpty() || prefRange.isEmpty() || prefRange.equals("Select")) return 0.5;
        try {
            int age = Integer.parseInt(ageStr.replaceAll("[^0-9]", ""));
            int[] range = parseIntRange(prefRange);
            if (range == null) return 0.5;
            if (age >= range[0] && age <= range[1]) return 1.0;
            int dist = Math.min(Math.abs(age - range[0]), Math.abs(age - range[1]));
            if (dist <= 3) return 0.7;
            if (dist <= 5) return 0.4;
            return 0.1;
        } catch (Exception e) { return 0.5; }
    }

    private double scoreHeight(String heightStr, String prefRange) {
        if (heightStr.isEmpty() || prefRange.isEmpty() || prefRange.equals("Select")) return 0.5;
        try {
            double hInch = heightToInches(heightStr);
            double[] range = parseHeightRange(prefRange);
            if (range == null) return 0.5;
            if (hInch >= range[0] && hInch <= range[1]) return 1.0;
            double dist = Math.min(Math.abs(hInch - range[0]), Math.abs(hInch - range[1]));
            if (dist <= 2) return 0.7;
            if (dist <= 4) return 0.4;
            return 0.1;
        } catch (Exception e) { return 0.5; }
    }

    private double scoreExact(String val, String pref) {
        if (val.isEmpty() || pref.isEmpty() || pref.equals("Select")) return 0.5;
        return val.equalsIgnoreCase(pref) ? 1.0 : 0.0;
    }

    private double scoreFuzzy(String val, String pref) {
        if (val.isEmpty() || pref.isEmpty() || pref.equals("Select")) return 0.5;
        if (val.equalsIgnoreCase(pref)) return 1.0;
        String v = val.toLowerCase(), p = pref.toLowerCase();
        if (v.contains(p) || p.contains(v)) return 0.7;
        return 0.0;
    }

    private double scoreLocation(String city, String prefLoc) {
        if (city.isEmpty() || prefLoc.isEmpty()) return 0.5;
        String c = city.toLowerCase(), p = prefLoc.toLowerCase();
        if (c.equals(p)) return 1.0;
        if (c.contains(p) || p.contains(c)) return 0.7;
        return 0.0;
    }

    // ===================== PARSING UTILS =====================

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

    // ===================== TRUST SCORE (Fake Profile Detection) =====================

    private int calculateTrustScore(DataSnapshot personalSnap, DataSnapshot imgSnap, DataSnapshot userSnap) {
        int score = 0;

        // Photo present (20 points)
        String photoUrl = safeString(personalSnap.child("imageUrl").getValue(String.class));
        if (photoUrl.isEmpty()) photoUrl = safeString(imgSnap.child("imageUrl").getValue(String.class));
        if (!photoUrl.isEmpty()) score += 20;

        // Basic info complete (20 points)
        int basicFields = 0;
        if (hasValue(personalSnap, "name")) basicFields++;
        if (hasValue(personalSnap, "age")) basicFields++;
        if (hasValue(personalSnap, "gender")) basicFields++;
        if (hasValue(personalSnap, "city")) basicFields++;
        score += (basicFields * 5);

        // Religious info (10 points)
        if (hasValue(personalSnap, "religion")) score += 5;
        if (hasValue(personalSnap, "caste")) score += 5;

        // Education & Occupation (15 points)
        if (hasValue(personalSnap, "education")) score += 8;
        if (hasValue(personalSnap, "occupation")) score += 7;

        // About me present (10 points)
        if (hasValue(personalSnap, "about")) score += 10;

        // Resume present (10 points)
        String resumeUrl = safeString(imgSnap.child("resumeUrl").getValue(String.class));
        if (!resumeUrl.isEmpty()) score += 10;

        // Family details present (5 points)
        DataSnapshot familySnap = userSnap.child("family");
        if (familySnap.exists()) score += 5;

        // Partner preference set (5 points)
        DataSnapshot prefSnap = userSnap.child("partnerPreference");
        if (prefSnap.exists()) score += 5;

        // Verification bonus (up to 15 points)
        String vstatus = safeString(userSnap.child("verificationStatus").getValue(String.class));
        if (vstatus.isEmpty()) {
            vstatus = safeString(personalSnap.child("verificationStatus").getValue(String.class));
        }
        if ("verified".equals(vstatus)) score += 15;
        else if ("pending".equals(vstatus)) score += 8;

        return Math.min(score, 100);
    }

    private boolean hasValue(DataSnapshot snap, String key) {
        String val = snap.child(key).getValue(String.class);
        return val != null && !val.isEmpty();
    }

    // ===================== FILTERING =====================

    private void applyFilters() {
        filteredMatches.clear();
        String religionFilter = spFilterReligion.getSelectedItem().toString();
        String minScoreStr = spFilterMinScore.getSelectedItem().toString();

        int minScore = 0;
        if (minScoreStr.contains("90")) minScore = 90;
        else if (minScoreStr.contains("80")) minScore = 80;
        else if (minScoreStr.contains("70")) minScore = 70;
        else if (minScoreStr.contains("60")) minScore = 60;
        else if (minScoreStr.contains("50")) minScore = 50;

        for (MatchResult m : allMatches) {
            if (!religionFilter.equals("All Religions") && !religionFilter.equalsIgnoreCase(m.religion)) continue;
            if (m.score < minScore) continue;
            filteredMatches.add(m);
        }

        matchAdapter.notifyDataSetChanged();
        tvResultCount.setText(filteredMatches.size() + " matches found");

        if (filteredMatches.isEmpty()) {
            recyclerMatches.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerMatches.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    // ===================== UTILS =====================

    private boolean isOppositeGender(String myGender, String otherGender) {
        if (myGender.isEmpty() || otherGender.isEmpty()) return true;
        String m = myGender.toLowerCase(), o = otherGender.toLowerCase();
        if (m.contains("male") && !m.contains("fe")) return o.contains("fe");
        if (m.contains("fe")) return o.contains("male") && !o.contains("fe");
        return true;
    }

    @NonNull
    private String safeString(String s) { return s != null ? s : ""; }

    private void showError(String msg) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ===================== INNER MODEL =====================

    static class MatchResult {
        String uid, name, age, height, religion, caste, education, occupation, city, gender, photoUrl;
        double score;
        int trustScore;
        String verificationStatus;

        MatchResult(String uid, String name, String age, String height, String religion,
                    String caste, String education, String occupation, String city,
                    String gender, String photoUrl, double score, int trustScore, String verificationStatus) {
            this.uid = uid; this.name = name; this.age = age; this.height = height;
            this.religion = religion; this.caste = caste; this.education = education;
            this.occupation = occupation; this.city = city; this.gender = gender;
            this.photoUrl = photoUrl; this.score = score;
            this.trustScore = trustScore;
            this.verificationStatus = verificationStatus;
        }

        String getPercent() { return String.format("%.0f%%", score); }
    }

    // ===================== INNER ADAPTER (Programmatic Views) =====================

    static class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder> {

        List<MatchResult> list;
        Set<String> pendingUids;

        MatchAdapter(List<MatchResult> list, Set<String> pendingUids) {
            this.list = list;
            this.pendingUids = pendingUids;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Build white card row programmatically to match app style
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
            img.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageResource(R.drawable.ic_profile);
            card.addView(img);

            // Left info column
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
            tvName.setId(View.generateViewId());
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
            tvDetails.setId(View.generateViewId());
            infoCol.addView(tvDetails);

            TextView tvExtra = new TextView(parent.getContext());
            tvExtra.setTextSize(12);
            tvExtra.setTextColor(0xFF777777);
            tvExtra.setId(View.generateViewId());
            infoCol.addView(tvExtra);

            TextView tvTrustWarning = new TextView(parent.getContext());
            tvTrustWarning.setTextSize(11);
            tvTrustWarning.setTypeface(null, Typeface.BOLD);
            tvTrustWarning.setPadding(0, 4, 0, 0);
            tvTrustWarning.setVisibility(View.GONE);
            infoCol.addView(tvTrustWarning);

            card.addView(infoCol);

            // Right score + button column
            LinearLayout actionCol = new LinearLayout(parent.getContext());
            actionCol.setOrientation(LinearLayout.VERTICAL);
            actionCol.setGravity(android.view.Gravity.CENTER);
            actionCol.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvScore = new TextView(parent.getContext());
            tvScore.setTextSize(14);
            tvScore.setTypeface(null, Typeface.BOLD);
            tvScore.setPadding(10, 6, 10, 6);
            tvScore.setId(View.generateViewId());
            actionCol.addView(tvScore);

            Button btnConnect = new Button(parent.getContext());
            btnConnect.setText("Connect");
            btnConnect.setTextSize(12);
            btnConnect.setTextColor(0xFFFFFFFF);
            btnConnect.setBackgroundColor(0xFFC2185B);
            btnConnect.setMinHeight(0);
            btnConnect.setMinimumHeight(64);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 8, 0, 0);
            btnConnect.setLayoutParams(btnParams);
            btnConnect.setId(View.generateViewId());
            actionCol.addView(btnConnect);

            card.addView(actionCol);

            return new ViewHolder(card, img, tvName, tvVerifiedBadge, tvDetails, tvExtra, tvTrustWarning, tvScore, btnConnect);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            MatchResult m = list.get(position);

            // Name with verified badge
            String nameText = m.name.isEmpty() ? "Unknown" : m.name;
            if ("verified".equals(m.verificationStatus)) {
                nameText += " ";
            }
            h.tvName.setText(nameText + "  |  " + m.getPercent() + " Match");

            h.tvDetails.setText((m.age.isEmpty() ? "-" : m.age + " yrs") + "  |  " + (m.city.isEmpty() ? "-" : m.city));
            h.tvExtra.setText((m.religion.isEmpty() ? "-" : m.religion) + "  |  " + (m.education.isEmpty() ? "-" : m.education));

            // Score color
            int color;
            if (m.score >= 80) color = 0xFF2E7D32;
            else if (m.score >= 50) color = 0xFFF57F17;
            else color = 0xFFC62828;
            h.tvScore.setTextColor(color);
            h.tvScore.setText(m.getPercent());

            // Verification badge
            if ("verified".equals(m.verificationStatus)) {
                h.tvVerifiedBadge.setVisibility(View.VISIBLE);
                h.tvVerifiedBadge.setText("Verified");
                h.tvVerifiedBadge.setBackgroundColor(0xFF4CAF50);
            } else if ("pending".equals(m.verificationStatus)) {
                h.tvVerifiedBadge.setVisibility(View.VISIBLE);
                h.tvVerifiedBadge.setText("Pending");
                h.tvVerifiedBadge.setBackgroundColor(0xFFFF9800);
            } else {
                h.tvVerifiedBadge.setVisibility(View.GONE);
            }

            // Trust score indicator (low trust = potential fake)
            if (m.trustScore < 40) {
                h.tvTrustWarning.setVisibility(View.VISIBLE);
                h.tvTrustWarning.setText("Low Trust");
                h.tvTrustWarning.setTextColor(0xFFF44336);
            } else if (m.trustScore < 60) {
                h.tvTrustWarning.setVisibility(View.VISIBLE);
                h.tvTrustWarning.setText("New Profile");
                h.tvTrustWarning.setTextColor(0xFFFF9800);
            } else {
                h.tvTrustWarning.setVisibility(View.GONE);
            }

            // Photo
            if (m.photoUrl != null && !m.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(m.photoUrl).placeholder(R.drawable.ic_profile).circleCrop().into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }

            // Click to view profile
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(h.itemView.getContext(), ProfileActivity.class);
                i.putExtra("targetUid", m.uid);
                h.itemView.getContext().startActivity(i);
            });

            // Handle connection button state
            if (pendingUids.contains(m.uid)) {
                h.btnConnect.setText("Sent");
                h.btnConnect.setEnabled(false);
                h.btnConnect.setBackgroundColor(0xFF9E9E9E);
            } else {
                h.btnConnect.setText("Connect");
                h.btnConnect.setEnabled(true);
                h.btnConnect.setBackgroundColor(0xFFC2185B);
                h.btnConnect.setOnClickListener(v -> {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseDatabase.getInstance().getReference("ConnectionRequests")
                            .child(m.uid).child(uid).setValue("pending")
                            .addOnSuccessListener(unused -> {
                                pendingUids.add(m.uid);
                                h.btnConnect.setText("Sent");
                                h.btnConnect.setEnabled(false);
                                h.btnConnect.setBackgroundColor(0xFF9E9E9E);
                                Toast.makeText(h.itemView.getContext(), "Request sent to " + m.name, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(h.itemView.getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName, tvVerifiedBadge, tvDetails, tvExtra, tvTrustWarning, tvScore;
            Button btnConnect;

            ViewHolder(View itemView, ImageView img, TextView tvName, TextView tvVerifiedBadge,
                       TextView tvDetails, TextView tvExtra, TextView tvTrustWarning,
                       TextView tvScore, Button btnConnect) {
                super(itemView);
                this.img = img; this.tvName = tvName; this.tvVerifiedBadge = tvVerifiedBadge;
                this.tvDetails = tvDetails; this.tvExtra = tvExtra; this.tvTrustWarning = tvTrustWarning;
                this.tvScore = tvScore; this.btnConnect = btnConnect;
            }
        }
    }
}