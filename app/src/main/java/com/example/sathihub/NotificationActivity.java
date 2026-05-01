package com.example.sathihub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class NotificationActivity extends AppCompatActivity {

    RecyclerView recyclerNotifications;
    TextView tvEmpty;
    ProgressBar progressBar;
    ImageView btnBack;

    FirebaseAuth auth;
    String currentUid;
    DatabaseReference connectionRequestsRef, personalInfoRef;

    List<NotifItem> notifList = new ArrayList<>();
    NotifAdapter notifAdapter;
    ValueEventListener notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = auth.getCurrentUser().getUid();
        connectionRequestsRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests");
        personalInfoRef = FirebaseDatabase.getInstance().getReference("PersonalInfo");

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotifAdapter(notifList);
        recyclerNotifications.setAdapter(notifAdapter);

        btnBack.setOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerNotifications.setVisibility(View.GONE);

        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerNotifications.setVisibility(View.GONE);
                    return;
                }

                int total = (int) snapshot.getChildrenCount();
                final int[] loaded = {0};

                for (DataSnapshot child : snapshot.getChildren()) {
                    String otherUid = child.getKey();
                    String status = child.getValue(String.class);

                    if (otherUid == null || status == null) {
                        loaded[0]++;
                        if (loaded[0] >= total) updateUI();
                        continue;
                    }

                    loadUserDetails(otherUid, status, () -> {
                        loaded[0]++;
                        if (loaded[0] >= total) updateUI();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        connectionRequestsRef.child(currentUid).addValueEventListener(notifListener);
    }

    private void loadUserDetails(String otherUid, String status, Runnable onComplete) {
        personalInfoRef.child(otherUid).get().addOnSuccessListener(personalSnap -> {
            String name = safeString(personalSnap.child("name").getValue(String.class));
            String photoUrl = safeString(personalSnap.child("imageUrl").getValue(String.class));
            if (photoUrl.isEmpty()) {
                photoUrl = safeString(personalSnap.child("profilePhoto").getValue(String.class));
            }

            String message;
            if ("pending".equals(status)) {
                message = "sent you a connection request";
            } else if ("accepted".equals(status)) {
                message = "accepted your connection request";
            } else {
                message = "updated connection status";
            }

            notifList.add(new NotifItem(otherUid, name, photoUrl, message, status));
            onComplete.run();
        }).addOnFailureListener(e -> onComplete.run());
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        if (notifList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerNotifications.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerNotifications.setVisibility(View.VISIBLE);
            notifAdapter.notifyDataSetChanged();
        }
    }

    @NonNull
    private String safeString(String s) { return s != null ? s : ""; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) {
            connectionRequestsRef.child(currentUid).removeEventListener(notifListener);
        }
    }

    // ===================== INNER MODELS =====================

    static class NotifItem {
        String uid, name, photoUrl, message, status;

        NotifItem(String uid, String name, String photoUrl, String message, String status) {
            this.uid = uid; this.name = name; this.photoUrl = photoUrl;
            this.message = message; this.status = status;
        }
    }

    // ===================== ADAPTER =====================

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.ViewHolder> {

        List<NotifItem> list;

        NotifAdapter(List<NotifItem> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.card_bg);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(12, 8, 12, 8);
            card.setLayoutParams(params);
            card.setPadding(16, 16, 16, 16);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);

            ImageView img = new ImageView(parent.getContext());
            img.setLayoutParams(new LinearLayout.LayoutParams(70, 70));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img);

            LinearLayout infoCol = new LinearLayout(parent.getContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoParams.setMargins(16, 0, 16, 0);
            infoCol.setLayoutParams(infoParams);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF000000);
            infoCol.addView(tvName);

            TextView tvMessage = new TextView(parent.getContext());
            tvMessage.setTextSize(13);
            tvMessage.setTextColor(0xFF555555);
            infoCol.addView(tvMessage);

            TextView tvAction = new TextView(parent.getContext());
            tvAction.setTextSize(12);
            tvAction.setTextColor(0xFFC2185B);
            tvAction.setTypeface(null, Typeface.BOLD);
            tvAction.setPadding(0, 4, 0, 0);
            infoCol.addView(tvAction);

            card.addView(infoCol);

            return new ViewHolder(card, img, tvName, tvMessage, tvAction);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            NotifItem n = list.get(position);
            h.tvName.setText(n.name.isEmpty() ? "User" : n.name);
            h.tvMessage.setText(n.message);
            h.tvAction.setText("Tap to view");

            if (n.photoUrl != null && !n.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(n.photoUrl)
                        .placeholder(R.drawable.ic_profile).circleCrop().into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(NotificationActivity.this, ChatsActivity.class);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName, tvMessage, tvAction;

            ViewHolder(View itemView, ImageView img, TextView tvName, TextView tvMessage, TextView tvAction) {
                super(itemView);
                this.img = img; this.tvName = tvName;
                this.tvMessage = tvMessage; this.tvAction = tvAction;
            }
        }
    }
}
