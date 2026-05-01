package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgProfile;
    TextView tvName, tvInfo, tvAbout, tvDetails;
    TextView tvVerifiedBadge, tvTrustScore, tvCompatPercent, tvTrustLabel, tvCompatLabel;
    LinearLayout layoutTrustScore, layoutCompatibility;
    View trustBar;
    Button btnEditProfile, btnLogout, btnAction;
    LinearLayout layoutSettings;

    FirebaseAuth auth;
    String uid;
    String targetUid;
    boolean isOwnProfile;

    boolean isProfileCompleted = false;

    // Hold data from all nodes
    String age, height, city, religion, caste, education, occupation,
            fatherOcc, motherOcc, partnerAge, partnerReligion, about;

    // Trust & compatibility
    int trustScore = 0;
    String verificationStatus = "none";
    double compatibilityScore = 0;

    // Current user preferences for compatibility calc
    String myAge, myReligion, myCaste, myEducation, myOccupation, myCity, myHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfile = findViewById(R.id.imgProfile);
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        tvAbout = findViewById(R.id.tvAbout);
        tvDetails = findViewById(R.id.tvDetails);

        tvVerifiedBadge = findViewById(R.id.tvVerifiedBadge);
        tvTrustScore = findViewById(R.id.tvTrustScore);
        tvCompatPercent = findViewById(R.id.tvCompatPercent);
        tvTrustLabel = findViewById(R.id.tvTrustLabel);
        tvCompatLabel = findViewById(R.id.tvCompatLabel);
        layoutTrustScore = findViewById(R.id.layoutTrustScore);
        layoutCompatibility = findViewById(R.id.layoutCompatibility);
        trustBar = findViewById(R.id.trustBar);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnAction = findViewById(R.id.btnAction);
        layoutSettings = findViewById(R.id.layoutSettings);

        TextView btnAccount = findViewById(R.id.btnAccount);
        TextView btnPrivacy = findViewById(R.id.btnPrivacy);
        TextView btnHelp = findViewById(R.id.btnHelp);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        uid = auth.getCurrentUser().getUid();

        // Check if viewing another user's profile
        targetUid = getIntent().getStringExtra("targetUid");
        if (targetUid == null || targetUid.isEmpty()) {
            targetUid = uid;
        }
        isOwnProfile = targetUid.equals(uid);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid);

        if (isOwnProfile) {
            // Own profile - show all controls
            btnAction.setVisibility(View.GONE);

            // Check profile completed
            userRef.child("profileCompleted").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        isProfileCompleted = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    }
                    btnEditProfile.setText(isProfileCompleted ? "Edit Profile" : "Complete Profile");
                    if (!isProfileCompleted) {
                        Toast.makeText(ProfileActivity.this, "Please complete your profile", Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ProfileActivity.this, "Error reading profile status", Toast.LENGTH_SHORT).show();
                }
            });

            btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

            btnLogout.setOnClickListener(v -> {
                auth.signOut();
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            });

            btnAccount.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, AccountSettingsActivity.class)));
            btnPrivacy.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, PrivacySecurityActivity.class)));
            btnHelp.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, HelpSupportActivity.class)));
        } else {
            // Other user's profile - hide edit/settings/logout, show connect/chat action
            btnEditProfile.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.GONE);
            btnAction.setVisibility(View.VISIBLE);

            setupActionButton();
            loadMyPreferencesForCompatibility();
        }

        // Load name
        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                tvName.setText(name != null ? name : "No Name");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load all profile data from multiple nodes independently
        loadFullProfile();

        // Load image
        loadProfileImage(userRef);
    }

    private void setupActionButton() {
        DatabaseReference connRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests")
                .child(uid).child(targetUid);

        connRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("accepted".equals(status)) {
                    btnAction.setText("Chat");
                    btnAction.setBackgroundColor(0xFF4CAF50);
                    btnAction.setOnClickListener(v -> {
                        Intent i = new Intent(ProfileActivity.this, ChatsActivity.class);
                        startActivity(i);
                    });
                } else if ("pending".equals(status)) {
                    btnAction.setText("Request Sent");
                    btnAction.setEnabled(false);
                    btnAction.setBackgroundColor(0xFF9E9E9E);
                } else {
                    btnAction.setText("Connect");
                    btnAction.setBackgroundColor(0xFFC2185B);
                    btnAction.setOnClickListener(v -> sendConnectionRequest());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendConnectionRequest() {
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests")
                .child(targetUid).child(uid);
        reqRef.setValue("pending")
                .addOnSuccessListener(unused -> {
                    btnAction.setText("Request Sent");
                    btnAction.setEnabled(false);
                    btnAction.setBackgroundColor(0xFF9E9E9E);
                    Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ===================== TRUST SCORE & VERIFICATION =====================

    private void loadTrustAndVerification() {
        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(targetUid);
        DatabaseReference verificationRef = FirebaseDatabase.getInstance().getReference("Verification").child(targetUid);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid);
        DatabaseReference profileImageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(targetUid);

        // Check verification status
        verificationRef.child("status").get().addOnSuccessListener(vsnap -> {
            String vstatus = vsnap.getValue(String.class);
            if (vstatus == null) {
                // Fallback to Users node
                userRef.child("verificationStatus").get().addOnSuccessListener(usnap -> {
                    String uv = usnap.getValue(String.class);
                    displayVerification(uv != null ? uv : "none");
                });
            } else {
                displayVerification(vstatus);
            }
        }).addOnFailureListener(e -> displayVerification("none"));

        // Calculate trust score based on profile completeness
        personalRef.get().addOnSuccessListener(personalSnap -> {
            profileImageRef.get().addOnSuccessListener(imgSnap -> {
                userRef.get().addOnSuccessListener(userSnap -> {
                    trustScore = calculateTrustScore(personalSnap, imgSnap, userSnap);
                    displayTrustScore(trustScore);
                });
            });
        });
    }

    private int calculateTrustScore(DataSnapshot personalSnap, DataSnapshot imgSnap, DataSnapshot userSnap) {
        int score = 0;

        // Photo present (20 points)
        String photoUrl = safeStr(personalSnap.child("imageUrl").getValue(String.class));
        if (photoUrl.isEmpty()) photoUrl = safeStr(imgSnap.child("imageUrl").getValue(String.class));
        if (!photoUrl.isEmpty()) score += 20;

        // Basic info complete (20 points) - name, age, gender, city
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
        String resumeUrl = safeStr(imgSnap.child("resumeUrl").getValue(String.class));
        if (!resumeUrl.isEmpty()) score += 10;

        // Family details present (5 points)
        DataSnapshot familySnap = userSnap.child("family");
        if (familySnap.exists()) score += 5;

        // Partner preference set (5 points)
        DataSnapshot prefSnap = userSnap.child("partnerPreference");
        if (prefSnap.exists()) score += 5;

        // Verification bonus (up to 15 points)
        String vstatus = safeStr(userSnap.child("verificationStatus").getValue(String.class));
        if ("verified".equals(vstatus)) score += 15;
        else if ("pending".equals(vstatus)) score += 8;

        return Math.min(score, 100);
    }

    private boolean hasValue(DataSnapshot snap, String key) {
        String val = snap.child(key).getValue(String.class);
        return val != null && !val.isEmpty();
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private void displayVerification(String status) {
        verificationStatus = status != null ? status : "none";
        tvVerifiedBadge.setVisibility(View.VISIBLE);

        if ("verified".equals(verificationStatus)) {
            tvVerifiedBadge.setText("Verified");
            tvVerifiedBadge.setBackgroundColor(0xFF4CAF50); // Green
        } else if ("pending".equals(verificationStatus)) {
            tvVerifiedBadge.setText("Verification Pending");
            tvVerifiedBadge.setBackgroundColor(0xFFFF9800); // Orange
        } else {
            tvVerifiedBadge.setText("Not Verified");
            tvVerifiedBadge.setBackgroundColor(0xFF9E9E9E); // Grey
        }
    }

    private void displayTrustScore(int score) {
        trustScore = score;
        layoutTrustScore.setVisibility(View.VISIBLE);
        tvTrustScore.setText(score + "/100");

        // Color based on score
        int color;
        if (score >= 80) color = 0xFF4CAF50;
        else if (score >= 50) color = 0xFFFF9800;
        else color = 0xFFF44336;

        trustBar.setBackgroundColor(color);

        // Animate bar width
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) trustBar.getLayoutParams();
        params.weight = score;
        trustBar.setLayoutParams(params);
    }

    // ===================== COMPATIBILITY (for other profiles) =====================

    private void loadMyPreferencesForCompatibility() {
        if (isOwnProfile) return;

        DatabaseReference myPersonalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);
        DatabaseReference myPrefRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("partnerPreference");

        myPersonalRef.get().addOnSuccessListener(mySnap -> {
            myAge = safeStr(mySnap.child("age").getValue(String.class));
            myReligion = safeStr(mySnap.child("religion").getValue(String.class));
            myCaste = safeStr(mySnap.child("caste").getValue(String.class));
            myEducation = safeStr(mySnap.child("education").getValue(String.class));
            myOccupation = safeStr(mySnap.child("occupation").getValue(String.class));
            myCity = safeStr(mySnap.child("city").getValue(String.class));
            myHeight = safeStr(mySnap.child("height").getValue(String.class));

            myPrefRef.get().addOnSuccessListener(prefSnap -> {
                // After both loaded, calculate compatibility when target profile data is ready
                // The calculation will happen in updateUI() after all target data is loaded
                calculateAndDisplayCompatibility();
            });
        });
    }

    private void calculateAndDisplayCompatibility() {
        if (isOwnProfile) return;
        if (age == null || religion == null) return; // Not loaded yet

        double score = 0;
        int factors = 0;

        // Religion match (25%)
        if (!myReligion.isEmpty() && religion != null && !religion.isEmpty()) {
            if (myReligion.equalsIgnoreCase(religion)) score += 25;
            else score += 5;
            factors++;
        }

        // Caste match (15%)
        if (!myCaste.isEmpty() && caste != null && !caste.isEmpty()) {
            if (myCaste.equalsIgnoreCase(caste)) score += 15;
            else score += 3;
            factors++;
        }

        // Education compatibility (15%)
        if (!myEducation.isEmpty() && education != null && !education.isEmpty()) {
            if (myEducation.equalsIgnoreCase(education)) score += 15;
            else score += 8;
            factors++;
        }

        // Location match (15%)
        if (!myCity.isEmpty() && city != null && !city.isEmpty()) {
            if (myCity.equalsIgnoreCase(city)) score += 15;
            else if (city.toLowerCase().contains(myCity.toLowerCase()) || myCity.toLowerCase().contains(city.toLowerCase())) score += 8;
            else score += 3;
            factors++;
        }

        // Age compatibility (15%)
        if (!myAge.isEmpty() && age != null && !age.isEmpty()) {
            try {
                int myAgeInt = Integer.parseInt(myAge.replaceAll("[^0-9]", ""));
                int targetAge = Integer.parseInt(age.replaceAll("[^0-9]", ""));
                int diff = Math.abs(myAgeInt - targetAge);
                if (diff <= 2) score += 15;
                else if (diff <= 5) score += 10;
                else if (diff <= 8) score += 5;
                else score += 2;
                factors++;
            } catch (Exception ignored) {}
        }

        // Occupation compatibility (10%)
        if (!myOccupation.isEmpty() && occupation != null && !occupation.isEmpty()) {
            if (myOccupation.equalsIgnoreCase(occupation)) score += 10;
            else score += 4;
            factors++;
        }

        // Height compatibility (5%)
        if (!myHeight.isEmpty() && height != null && !height.isEmpty()) {
            score += 5; // Simplified
            factors++;
        }

        // If no preferences set, default to 50%
        if (factors == 0) {
            compatibilityScore = 50;
        } else {
            compatibilityScore = Math.min(score, 100);
        }

        layoutCompatibility.setVisibility(View.VISIBLE);
        tvCompatPercent.setText((int) compatibilityScore + "%");

        // Color
        int color;
        if (compatibilityScore >= 80) color = 0xFF4CAF50;
        else if (compatibilityScore >= 50) color = 0xFFFF9800;
        else color = 0xFFF44336;
        tvCompatPercent.setTextColor(color);
    }

    // ===================== PROFILE DATA LOADING =====================

    private void loadFullProfile() {
        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(targetUid);
        DatabaseReference religiousRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid).child("religious");
        DatabaseReference educationRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid).child("education");
        DatabaseReference familyRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid).child("family");
        DatabaseReference partnerRef = FirebaseDatabase.getInstance().getReference("Users").child(targetUid).child("partnerPreference");
        DatabaseReference imageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(targetUid);

        // Read PersonalInfo (primary source)
        personalRef.get().addOnSuccessListener(snapshot -> {
            age = getValue(snapshot, "age");
            height = getValue(snapshot, "height");
            city = getValue(snapshot, "city");
            religion = getValue(snapshot, "religion");
            caste = getValue(snapshot, "caste");
            education = getValue(snapshot, "education");
            occupation = getValue(snapshot, "occupation");
            fatherOcc = getValue(snapshot, "fatherOccupation");
            motherOcc = getValue(snapshot, "motherOccupation");
            partnerAge = getValue(snapshot, "partnerAge");
            partnerReligion = getValue(snapshot, "partnerReligion");
            about = getValue(snapshot, "about");
            updateUI();
        }).addOnFailureListener(e -> {});

        // Fallback reads from original nodes
        religiousRef.get().addOnSuccessListener(snapshot -> {
            if (religion == null) religion = getValue(snapshot, "religion");
            if (caste == null) caste = getValue(snapshot, "caste");
            updateUI();
        }).addOnFailureListener(e -> {});

        educationRef.get().addOnSuccessListener(snapshot -> {
            if (education == null) education = getValue(snapshot, "qualification");
            if (occupation == null) occupation = getValue(snapshot, "occupation");
            if (city == null) city = getValue(snapshot, "city");
            updateUI();
        }).addOnFailureListener(e -> {});

        familyRef.get().addOnSuccessListener(snapshot -> {
            if (fatherOcc == null) fatherOcc = getValue(snapshot, "fatherOccupation");
            if (motherOcc == null) motherOcc = getValue(snapshot, "motherOccupation");
            updateUI();
        }).addOnFailureListener(e -> {});

        partnerRef.get().addOnSuccessListener(snapshot -> {
            if (partnerAge == null) partnerAge = getValue(snapshot, "ageRange");
            if (partnerReligion == null) partnerReligion = getValue(snapshot, "religion");
            updateUI();
        }).addOnFailureListener(e -> {});

        imageRef.get().addOnSuccessListener(snapshot -> {
            if (about == null) about = getValue(snapshot, "aboutMe");
            updateUI();
        }).addOnFailureListener(e -> {});

        // Load trust score and verification
        loadTrustAndVerification();
    }

    private synchronized void updateUI() {
        tvInfo.setText(
            (age != null ? age : "-") + " | " +
            (height != null ? height : "-") + " | " +
            (city != null ? city : "-")
        );

        tvAbout.setText(about != null ? about : "No About Info");

        tvDetails.setText(
            "Religion: " + (religion != null ? religion : "-") + "\n" +
            "Caste: " + (caste != null ? caste : "-") + "\n" +
            "Education: " + (education != null ? education : "-") + "\n" +
            "Occupation: " + (occupation != null ? occupation : "-") + "\n" +
            "Father: " + (fatherOcc != null ? fatherOcc : "-") + "\n" +
            "Mother: " + (motherOcc != null ? motherOcc : "-") + "\n" +
            "Partner Age: " + (partnerAge != null ? partnerAge : "-") + "\n" +
            "Partner Religion: " + (partnerReligion != null ? partnerReligion : "-")
        );

        // Try to calculate compatibility if viewing other profile
        if (!isOwnProfile) {
            calculateAndDisplayCompatibility();
        }
    }

    private String getValue(DataSnapshot snapshot, String key) {
        if (!snapshot.exists()) return null;
        String val = snapshot.child(key).getValue(String.class);
        return (val != null && !val.isEmpty()) ? val : null;
    }

    private void loadProfileImage(DatabaseReference userRef) {
        // Try Users/{uid}/profilePhoto first, then ProfileImage/{uid}/imageUrl
        userRef.child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    Glide.with(ProfileActivity.this).load(url).placeholder(R.drawable.ic_profile).into(imgProfile);
                } else {
                    FirebaseDatabase.getInstance().getReference("ProfileImage").child(targetUid).child("imageUrl")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snap) {
                                String imgUrl = snap.getValue(String.class);
                                if (imgUrl != null && !imgUrl.isEmpty()) {
                                    Glide.with(ProfileActivity.this).load(imgUrl).placeholder(R.drawable.ic_profile).into(imgProfile);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
