package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
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
    Button btnEditProfile, btnLogout;

    FirebaseAuth auth;
    DatabaseReference userRef, personalRef, imageRef;

    boolean isProfileCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfile = findViewById(R.id.imgProfile);
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        tvAbout = findViewById(R.id.tvAbout);
        tvDetails = findViewById(R.id.tvDetails);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);
        imageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(uid);

        // 🔹 PROFILE COMPLETED CHECK
        userRef.child("profileCompleted").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            isProfileCompleted = snapshot.getValue(Boolean.class);
                        } else {
                            isProfileCompleted = false;
                        }

                        if (!isProfileCompleted) {
                            btnEditProfile.setText("Complete Profile");
                            Toast.makeText(ProfileActivity.this,
                                    "Please complete your profile",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            btnEditProfile.setText("Edit Profile");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                }
        );

        // 🔹 NAME
        userRef.child("name").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        tvName.setText(name != null ? name : "No Name");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                }
        );

        // 🔹 PERSONAL + ABOUT + ALL DETAILS
        personalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String age = snapshot.child("age").getValue(String.class);
                String height = snapshot.child("height").getValue(String.class);
                String city = snapshot.child("city").getValue(String.class);
                String religion = snapshot.child("religion").getValue(String.class);
                String caste = snapshot.child("caste").getValue(String.class);
                String education = snapshot.child("education").getValue(String.class);
                String occupation = snapshot.child("occupation").getValue(String.class);
                String fatherOcc = snapshot.child("fatherOccupation").getValue(String.class);
                String motherOcc = snapshot.child("motherOccupation").getValue(String.class);
                String partnerAge = snapshot.child("partnerAge").getValue(String.class);
                String partnerReligion = snapshot.child("partnerReligion").getValue(String.class);
                String about = snapshot.child("about").getValue(String.class);

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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 🔹 IMAGE LOAD
        imageRef.child("imageUrl").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String imageUrl = snapshot.getValue(String.class);

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .into(imgProfile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                }
        );

        // 🔹 EDIT PROFILE
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

        // 🔹 LOGOUT
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }
}
