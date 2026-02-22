package com.example.sathihub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    LinearLayout navHome, navSearch, navMatches, navChats, navProfile;
    ImageView btnNotification, btnTopProfile, imgBottomProfile;

    FirebaseAuth auth;
    DatabaseReference userRef;

    boolean isProfileCompleted = false; // 👈 flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navMatches = findViewById(R.id.navMatches);
        navChats = findViewById(R.id.navChats);
        navProfile = findViewById(R.id.navProfile);

        btnNotification = findViewById(R.id.btnNotification);
        btnTopProfile = findViewById(R.id.btnTopProfile);
        imgBottomProfile = findViewById(R.id.imgBottomProfile);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")   // ⚠️ Capital U (same everywhere)
                .child(auth.getCurrentUser().getUid());

        // 🔥 CHECK profileCompleted
        userRef.child("profileCompleted").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean flag = snapshot.getValue(Boolean.class);
                        if (flag != null) {
                            isProfileCompleted = flag;
                        }
                    } else {
                        isProfileCompleted = false;
                    }

                    if (!isProfileCompleted) {
                        Toast.makeText(MainActivity.this,
                                "⚠ Please complete your profile to view other users",
                                Toast.LENGTH_LONG).show();
                    }
                });

        // 🔥 Load profile photo
        userRef.child("profilePhoto").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String photoUrl = snapshot.getValue(String.class);

                        Glide.with(MainActivity.this)
                                .load(photoUrl)
                                .into(btnTopProfile);

                        Glide.with(MainActivity.this)
                                .load(photoUrl)
                                .into(imgBottomProfile);
                    }
                });

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        navHome.setOnClickListener(v -> {
            if (!isProfileCompleted) {
                Toast.makeText(this,
                        "Complete your profile first",
                        Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
            }
        });

        navSearch.setOnClickListener(v -> {
            if (!isProfileCompleted) {
                Toast.makeText(this,
                        "Complete your profile first",
                        Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });

        navMatches.setOnClickListener(v -> {
            if (!isProfileCompleted) {
                Toast.makeText(this,
                        "Complete your profile first",
                        Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(MainActivity.this, MatchesActivity.class));
            }
        });

        navChats.setOnClickListener(v -> {
            if (!isProfileCompleted) {
                Toast.makeText(this,
                        "Complete your profile first",
                        Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(MainActivity.this, ChatsActivity.class));
            }
        });

        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NotificationActivity.class)));
    }
}
