package com.example.sathihub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    LinearLayout navHome, navSearch, navMatches, navChats, navProfile;
    LinearLayout btnQuickSearch, btnQuickMatches, btnQuickChats, cardCompleteProfile;
    ImageView btnNotification, btnTopProfile, imgBottomProfile, imgProfilePic;
    TextView tvUserName, tvProfileStatus, tvMatchCount, tvMessageCount, tvRequestCount, tvNotifBadge;
    Button btnCompleteProfile;
    RecyclerView recyclerRecommendations;

    FirebaseAuth auth;
    String currentUid;
    DatabaseReference usersRef, personalInfoRef, connectionRequestsRef, messagesRef, profileImageRef;

    boolean isProfileCompleted = false;
    String currentUserGender = "";

    List<RecUser> recList = new ArrayList<>();
    RecAdapter recAdapter;
    java.util.Set<String> acceptedUids = new java.util.HashSet<>();

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
        imgProfilePic = findViewById(R.id.imgProfilePic);

        tvUserName = findViewById(R.id.tvUserName);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        tvMatchCount = findViewById(R.id.tvMatchCount);
        tvMessageCount = findViewById(R.id.tvMessageCount);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        tvNotifBadge = findViewById(R.id.tvNotifBadge);

        cardCompleteProfile = findViewById(R.id.cardCompleteProfile);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);
        btnQuickSearch = findViewById(R.id.btnQuickSearch);
        btnQuickMatches = findViewById(R.id.btnQuickMatches);
        btnQuickChats = findViewById(R.id.btnQuickChats);
        recyclerRecommendations = findViewById(R.id.recyclerRecommendations);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUid = auth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        personalInfoRef = FirebaseDatabase.getInstance().getReference("PersonalInfo");
        connectionRequestsRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests");
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages");
        profileImageRef = FirebaseDatabase.getInstance().getReference("ProfileImage");

        recyclerRecommendations.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recAdapter = new RecAdapter(recList);
        recyclerRecommendations.setAdapter(recAdapter);

        setupNavigation();
        loadUserProfile();
        checkProfileStatus();
        loadStats();
        loadRecommendations();
    }

    // ===================== NAVIGATION =====================

    private void setupNavigation() {
        navHome.setOnClickListener(v ->
                Toast.makeText(this, "You are on Home", Toast.LENGTH_SHORT).show());

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        navSearch.setOnClickListener(v -> checkThenGo(SearchActivity.class));
        navMatches.setOnClickListener(v -> checkThenGo(MatchesActivity.class));
        navChats.setOnClickListener(v -> checkThenGo(ChatsActivity.class));

        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NotificationActivity.class)));

        btnTopProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        btnQuickSearch.setOnClickListener(v -> checkThenGo(SearchActivity.class));
        btnQuickMatches.setOnClickListener(v -> checkThenGo(MatchesActivity.class));
        btnQuickChats.setOnClickListener(v -> checkThenGo(ChatsActivity.class));

        btnCompleteProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PersonalInfoActivity.class)));
    }

    private void checkThenGo(Class<?> cls) {
        if (!isProfileCompleted) {
            Toast.makeText(this, "Complete your profile first", Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(MainActivity.this, cls));
        }
    }

    // ===================== USER PROFILE =====================

    private void loadUserProfile() {
        personalInfoRef.child(currentUid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String name = safeString(snapshot.child("name").getValue(String.class));
                String photoUrl = safeString(snapshot.child("imageUrl").getValue(String.class));

                if (!name.isEmpty()) {
                    tvUserName.setText(name);
                }

                if (!photoUrl.isEmpty()) {
                    loadPhotoIntoViews(photoUrl);
                } else {
                    // Fallback to ProfileImage node
                    FirebaseDatabase.getInstance().getReference("ProfileImage")
                            .child(currentUid).child("imageUrl").get()
                            .addOnSuccessListener(imgSnap -> {
                                String url = imgSnap.getValue(String.class);
                                if (url != null && !url.isEmpty()) {
                                    loadPhotoIntoViews(url);
                                }
                            });
                }
            }
        }).addOnFailureListener(e -> {});
    }

    private void loadPhotoIntoViews(String photoUrl) {
        Glide.with(MainActivity.this).load(photoUrl)
                .placeholder(R.drawable.ic_profile).circleCrop()
                .into(btnTopProfile);
        Glide.with(MainActivity.this).load(photoUrl)
                .placeholder(R.drawable.ic_profile).circleCrop()
                .into(imgBottomProfile);
        Glide.with(MainActivity.this).load(photoUrl)
                .placeholder(R.drawable.ic_profile).circleCrop()
                .into(imgProfilePic);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        loadRecommendations();
    }

    private void checkProfileStatus() {
        usersRef.child(currentUid).child("profileCompleted").get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean flag = snapshot.getValue(Boolean.class);
                        isProfileCompleted = flag != null && flag;
                    }

                    if (isProfileCompleted) {
                        tvProfileStatus.setText("Profile Complete");
                        tvProfileStatus.setTextColor(0xFF4CAF50);
                        cardCompleteProfile.setVisibility(View.GONE);
                    } else {
                        tvProfileStatus.setText("Profile Incomplete");
                        tvProfileStatus.setTextColor(0xFFFF9800);
                        cardCompleteProfile.setVisibility(View.VISIBLE);
                        Toast.makeText(this,
                                "Please complete your profile to view other users",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ===================== STATS =====================

    private void loadStats() {
        // Matches = count of accepted connections
        connectionRequestsRef.child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int accepted = 0;
                int pending = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.getValue(String.class);
                    if ("accepted".equals(status)) accepted++;
                    else if ("pending".equals(status)) pending++;
                }
                tvMatchCount.setText(String.valueOf(accepted));
                tvRequestCount.setText(String.valueOf(pending));

                // Update notification badge
                if (pending > 0) {
                    tvNotifBadge.setText(String.valueOf(pending));
                    tvNotifBadge.setVisibility(View.VISIBLE);
                } else {
                    tvNotifBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Messages = count of chat rooms with unread messages
        // Simplified: count accepted connections as active chats
        connectionRequestsRef.child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int chats = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if ("accepted".equals(child.getValue(String.class))) chats++;
                }
                tvMessageCount.setText(String.valueOf(chats));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ===================== RECOMMENDATIONS =====================

    private void loadRecommendations() {
        acceptedUids.clear();

        connectionRequestsRef.child(currentUid).get().addOnSuccessListener(connSnap -> {
            for (DataSnapshot child : connSnap.getChildren()) {
                if ("accepted".equals(child.getValue(String.class))) {
                    String otherUid = child.getKey();
                    if (otherUid != null) acceptedUids.add(otherUid);
                }
            }

            personalInfoRef.child(currentUid).get().addOnSuccessListener(mySnap -> {
                if (mySnap.exists()) {
                    currentUserGender = safeString(mySnap.child("gender").getValue(String.class));
                }

                personalInfoRef.get().addOnSuccessListener(allSnap -> {
                    profileImageRef.get().addOnSuccessListener(imageSnap -> {
                        recList.clear();
                        for (DataSnapshot child : allSnap.getChildren()) {
                            String otherUid = child.getKey();
                            if (otherUid == null || otherUid.equals(currentUid)) continue;

                            // Skip accepted connections - they only show in Chats
                            if (acceptedUids.contains(otherUid)) continue;

                            String otherGender = safeString(child.child("gender").getValue(String.class));
                            if (!isOppositeGender(currentUserGender, otherGender)) continue;

                            String name = safeString(child.child("name").getValue(String.class));
                            String age = safeString(child.child("age").getValue(String.class));
                            String city = safeString(child.child("city").getValue(String.class));
                            String photoUrl = safeString(child.child("imageUrl").getValue(String.class));
                            if (photoUrl.isEmpty()) {
                                photoUrl = safeString(imageSnap.child(otherUid).child("imageUrl").getValue(String.class));
                            }

                            recList.add(new RecUser(otherUid, name, age, city, photoUrl));
                            if (recList.size() >= 10) break;
                        }
                        recAdapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> {});
                }).addOnFailureListener(e -> {});
            }).addOnFailureListener(e -> {});
        }).addOnFailureListener(e -> {});
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

    // ===================== INNER MODELS & ADAPTER =====================

    static class RecUser {
        String uid, name, age, city, photoUrl;

        RecUser(String uid, String name, String age, String city, String photoUrl) {
            this.uid = uid; this.name = name; this.age = age;
            this.city = city; this.photoUrl = photoUrl;
        }
    }

    class RecAdapter extends RecyclerView.Adapter<RecAdapter.ViewHolder> {

        List<RecUser> list;

        RecAdapter(List<RecUser> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(12, 8, 12, 8);
            card.setLayoutParams(params);
            card.setPadding(16, 16, 16, 16);
            card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

            ImageView img = new ImageView(parent.getContext());
            img.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(14);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF000000);
            tvName.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = 8;
            tvName.setLayoutParams(nameParams);
            card.addView(tvName);

            TextView tvInfo = new TextView(parent.getContext());
            tvInfo.setTextSize(12);
            tvInfo.setTextColor(0xFF666666);
            tvInfo.setGravity(android.view.Gravity.CENTER);
            card.addView(tvInfo);

            return new ViewHolder(card, img, tvName, tvInfo);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            RecUser u = list.get(position);
            h.tvName.setText(u.name.isEmpty() ? "User" : u.name);
            h.tvInfo.setText((u.age.isEmpty() ? "-" : u.age + " yrs") + " | " + (u.city.isEmpty() ? "-" : u.city));

            if (u.photoUrl != null && !u.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(u.photoUrl)
                        .placeholder(R.drawable.ic_profile).circleCrop().into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }

            h.itemView.setOnClickListener(v -> {
                if (!isProfileCompleted) {
                    Toast.makeText(MainActivity.this, "Complete your profile first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(MainActivity.this, ProfileActivity.class);
                i.putExtra("targetUid", u.uid);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName, tvInfo;

            ViewHolder(View itemView, ImageView img, TextView tvName, TextView tvInfo) {
                super(itemView);
                this.img = img; this.tvName = tvName; this.tvInfo = tvInfo;
            }
        }
    }
}
