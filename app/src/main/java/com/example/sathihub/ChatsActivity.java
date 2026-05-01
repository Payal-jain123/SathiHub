package com.example.sathihub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatsActivity extends AppCompatActivity {

    RecyclerView recyclerPending, recyclerChats, recyclerMessages;
    TextView tvPendingHeader, tvChatsHeader, tvEmpty, tvChatPartnerName;
    ProgressBar progressBar;
    LinearLayout chatPanel;
    EditText etMessage;
    Button btnSend;
    ImageView btnBack, btnCloseChat, imgChatPartner;

    FirebaseAuth auth;
    String currentUid;
    DatabaseReference connectionRequestsRef, usersRef, personalInfoRef, messagesRef;

    List<ConnectionItem> pendingList = new ArrayList<>();
    List<ConnectionItem> acceptedList = new ArrayList<>();
    List<Message> messageList = new ArrayList<>();

    PendingAdapter pendingAdapter;
    ChatListAdapter chatListAdapter;
    MessageAdapter messageAdapter;

    String activeChatPartnerUid = null;
    String activeChatPartnerName = "";
    String activeChatPartnerPhoto = "";
    String activeChatRoomId = null;
    ValueEventListener activeMessageListener = null;
    ValueEventListener connectionsListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        recyclerPending = findViewById(R.id.recyclerPending);
        recyclerChats = findViewById(R.id.recyclerChats);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        tvPendingHeader = findViewById(R.id.tvPendingHeader);
        tvChatsHeader = findViewById(R.id.tvChatsHeader);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvChatPartnerName = findViewById(R.id.tvChatPartnerName);
        progressBar = findViewById(R.id.progressBar);
        chatPanel = findViewById(R.id.chatPanel);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnCloseChat = findViewById(R.id.btnCloseChat);
        imgChatPartner = findViewById(R.id.imgChatPartner);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = auth.getCurrentUser().getUid();
        connectionRequestsRef = FirebaseDatabase.getInstance().getReference("ConnectionRequests");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        personalInfoRef = FirebaseDatabase.getInstance().getReference("PersonalInfo");
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages");

        setupAdapters();

        btnBack.setOnClickListener(v -> finish());
        btnCloseChat.setOnClickListener(v -> closeChat());
        btnSend.setOnClickListener(v -> sendMessage());

        loadConnections();
    }

    private void setupAdapters() {
        recyclerPending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        pendingAdapter = new PendingAdapter(pendingList);
        recyclerPending.setAdapter(pendingAdapter);

        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        chatListAdapter = new ChatListAdapter(acceptedList);
        recyclerChats.setAdapter(chatListAdapter);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList);
        recyclerMessages.setAdapter(messageAdapter);
    }

    // ===================== LOAD CONNECTIONS =====================

    private void loadConnections() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        connectionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingList.clear();
                acceptedList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    updateUIState();
                    return;
                }

                int total = (int) snapshot.getChildrenCount();
                final int[] loaded = {0};

                for (DataSnapshot child : snapshot.getChildren()) {
                    String otherUid = child.getKey();
                    String status = child.getValue(String.class);

                    if (otherUid == null || status == null) {
                        loaded[0]++;
                        if (loaded[0] >= total) updateUIState();
                        continue;
                    }

                    loadUserDetails(otherUid, status, () -> {
                        loaded[0]++;
                        if (loaded[0] >= total) updateUIState();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ChatsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        connectionRequestsRef.child(currentUid).addValueEventListener(connectionsListener);
    }

    private void loadUserDetails(String otherUid, String status, Runnable onComplete) {
        usersRef.child(otherUid).get().addOnSuccessListener(userSnap -> {
            personalInfoRef.child(otherUid).get().addOnSuccessListener(personalSnap -> {
                String name = safeString(personalSnap.child("name").getValue(String.class));
                if (name.isEmpty()) name = safeString(userSnap.child("name").getValue(String.class));

                String photoUrl = safeString(personalSnap.child("imageUrl").getValue(String.class));
                if (photoUrl.isEmpty()) {
                    photoUrl = safeString(personalSnap.child("profilePhoto").getValue(String.class));
                }

                ConnectionItem item = new ConnectionItem(otherUid, name, photoUrl, status);

                if ("pending".equals(status)) {
                    pendingList.add(item);
                } else if ("accepted".equals(status)) {
                    acceptedList.add(item);
                }

                onComplete.run();
            }).addOnFailureListener(e -> onComplete.run());
        }).addOnFailureListener(e -> onComplete.run());
    }

    private void updateUIState() {
        progressBar.setVisibility(View.GONE);

        if (pendingList.isEmpty() && acceptedList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvPendingHeader.setVisibility(View.GONE);
            recyclerPending.setVisibility(View.GONE);
            tvChatsHeader.setVisibility(View.GONE);
            recyclerChats.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        if (!pendingList.isEmpty()) {
            tvPendingHeader.setVisibility(View.VISIBLE);
            recyclerPending.setVisibility(View.VISIBLE);
            pendingAdapter.notifyDataSetChanged();
        } else {
            tvPendingHeader.setVisibility(View.GONE);
            recyclerPending.setVisibility(View.GONE);
        }

        if (!acceptedList.isEmpty()) {
            tvChatsHeader.setVisibility(View.VISIBLE);
            recyclerChats.setVisibility(View.VISIBLE);
            chatListAdapter.notifyDataSetChanged();
        } else {
            tvChatsHeader.setVisibility(View.GONE);
            recyclerChats.setVisibility(View.GONE);
        }
    }

    // ===================== ACCEPT REQUEST =====================

    private void acceptRequest(String fromUid) {
        // Mark as accepted on both sides
        connectionRequestsRef.child(currentUid).child(fromUid).setValue("accepted")
                .addOnSuccessListener(unused -> {
                    connectionRequestsRef.child(fromUid).child(currentUid).setValue("accepted")
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Request accepted! You can now chat.", Toast.LENGTH_SHORT).show();
                                // Create notification for the sender that their request was accepted
                                createAcceptanceNotification(fromUid);
                                // UI will auto-update via ValueEventListener
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createAcceptanceNotification(String toUid) {
        // Get current user's name for the notification
        personalInfoRef.child(currentUid).child("name").get().addOnSuccessListener(nameSnap -> {
            String myName = nameSnap.getValue(String.class);
            if (myName == null || myName.isEmpty()) myName = "Someone";

            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(toUid);
            String notifId = notifRef.push().getKey();
            if (notifId == null) return;

            Notification notif = new Notification();
            notif.type = "accepted";
            notif.fromUid = currentUid;
            notif.fromName = myName;
            notif.message = myName + " accepted your connection request";
            notif.timestamp = System.currentTimeMillis();
            notif.read = false;

            notifRef.child(notifId).setValue(notif);
        });
    }

    static class Notification {
        public String type;
        public String fromUid;
        public String fromName;
        public String message;
        public long timestamp;
        public boolean read;
        public Notification() {}
    }

    // ===================== CHAT PANEL =====================

    private void openChat(String partnerUid, String partnerName, String partnerPhoto) {
        activeChatPartnerUid = partnerUid;
        activeChatPartnerName = partnerName;
        activeChatPartnerPhoto = partnerPhoto;
        activeChatRoomId = getChatRoomId(currentUid, partnerUid);

        tvChatPartnerName.setText(partnerName);
        if (partnerPhoto != null && !partnerPhoto.isEmpty()) {
            Glide.with(this).load(partnerPhoto).placeholder(R.drawable.ic_profile).circleCrop().into(imgChatPartner);
        } else {
            imgChatPartner.setImageResource(R.drawable.ic_profile);
        }

        View.OnClickListener goToProfile = v -> {
            Intent i = new Intent(ChatsActivity.this, ProfileActivity.class);
            i.putExtra("targetUid", partnerUid);
            startActivity(i);
        };
        imgChatPartner.setOnClickListener(goToProfile);
        tvChatPartnerName.setOnClickListener(goToProfile);

        chatPanel.setVisibility(View.VISIBLE);
        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        etMessage.setText("");

        listenForMessages();
    }

    private void closeChat() {
        chatPanel.setVisibility(View.GONE);
        if (activeMessageListener != null) {
            messagesRef.child(activeChatRoomId).removeEventListener(activeMessageListener);
            activeMessageListener = null;
        }
        activeChatPartnerUid = null;
        activeChatPartnerName = "";
        activeChatPartnerPhoto = "";
        activeChatRoomId = null;
        imgChatPartner.setImageResource(R.drawable.ic_profile);
        imgChatPartner.setOnClickListener(null);
        tvChatPartnerName.setOnClickListener(null);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() || activeChatRoomId == null || activeChatPartnerUid == null) return;

        Message msg = new Message(currentUid, text, System.currentTimeMillis(), false);
        messagesRef.child(activeChatRoomId).push().setValue(msg)
                .addOnSuccessListener(unused -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show());
    }

    private void listenForMessages() {
        if (activeMessageListener != null) {
            messagesRef.child(activeChatRoomId).removeEventListener(activeMessageListener);
        }

        activeMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message msg = child.getValue(Message.class);
                    if (msg != null) {
                        msg.messageId = child.getKey();
                        messageList.add(msg);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        messagesRef.child(activeChatRoomId).addValueEventListener(activeMessageListener);
    }

    // ===================== UTILS =====================

    private String getChatRoomId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @NonNull
    private String safeString(String s) { return s != null ? s : ""; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeMessageListener != null && activeChatRoomId != null) {
            messagesRef.child(activeChatRoomId).removeEventListener(activeMessageListener);
        }
        if (connectionsListener != null) {
            connectionRequestsRef.child(currentUid).removeEventListener(connectionsListener);
        }
    }

    // ===================== INNER MODELS =====================

    static class ConnectionItem {
        String uid, name, photoUrl, status;

        ConnectionItem(String uid, String name, String photoUrl, String status) {
            this.uid = uid;
            this.name = name;
            this.photoUrl = photoUrl;
            this.status = status;
        }
    }

    public static class Message {
        public String senderId, text, messageId;
        public long timestamp;
        public boolean read;

        public Message() {}

        public Message(String senderId, String text, long timestamp, boolean read) {
            this.senderId = senderId;
            this.text = text;
            this.timestamp = timestamp;
            this.read = read;
        }
    }

    // ===================== PENDING ADAPTER =====================

    class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.ViewHolder> {

        List<ConnectionItem> list;

        PendingAdapter(List<ConnectionItem> list) { this.list = list; }

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
            img.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
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

            Button btnAccept = new Button(parent.getContext());
            btnAccept.setText("Accept");
            btnAccept.setTextSize(12);
            btnAccept.setTextColor(0xFFFFFFFF);
            btnAccept.setBackgroundColor(0xFF4CAF50);
            btnAccept.setMinHeight(0);
            btnAccept.setMinimumHeight(72);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.topMargin = 8;
            btnAccept.setLayoutParams(btnParams);
            card.addView(btnAccept);

            return new ViewHolder(card, img, tvName, btnAccept);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            ConnectionItem c = list.get(position);
            h.tvName.setText(c.name.isEmpty() ? "User" : c.name);
            if (c.photoUrl != null && !c.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(c.photoUrl).placeholder(R.drawable.ic_profile).circleCrop().into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }
            h.btnAccept.setOnClickListener(v -> acceptRequest(c.uid));
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ChatsActivity.this, ProfileActivity.class);
                i.putExtra("targetUid", c.uid);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName;
            Button btnAccept;

            ViewHolder(View itemView, ImageView img, TextView tvName, Button btnAccept) {
                super(itemView);
                this.img = img; this.tvName = tvName; this.btnAccept = btnAccept;
            }
        }
    }

    // ===================== CHAT LIST ADAPTER =====================

    class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

        List<ConnectionItem> list;

        ChatListAdapter(List<ConnectionItem> list) { this.list = list; }

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
            img.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img);

            LinearLayout infoCol = new LinearLayout(parent.getContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoParams.setMargins(16, 0, 16, 0);
            infoCol.setLayoutParams(infoParams);

            TextView tvName = new TextView(parent.getContext());
            tvName.setTextSize(16);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(0xFF000000);
            infoCol.addView(tvName);

            TextView tvHint = new TextView(parent.getContext());
            tvHint.setTextSize(13);
            tvHint.setTextColor(0xFF888888);
            tvHint.setText("Tap to chat");
            infoCol.addView(tvHint);

            card.addView(infoCol);

            return new ViewHolder(card, img, tvName);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            ConnectionItem c = list.get(position);
            h.tvName.setText(c.name.isEmpty() ? "User" : c.name);
            if (c.photoUrl != null && !c.photoUrl.isEmpty()) {
                Glide.with(h.itemView.getContext()).load(c.photoUrl).placeholder(R.drawable.ic_profile).circleCrop().into(h.img);
            } else {
                h.img.setImageResource(R.drawable.ic_profile);
            }
            h.itemView.setOnClickListener(v -> openChat(c.uid, c.name, c.photoUrl));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tvName;

            ViewHolder(View itemView, ImageView img, TextView tvName) {
                super(itemView);
                this.img = img; this.tvName = tvName;
            }
        }
    }

    // ===================== MESSAGE ADAPTER =====================

    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

        List<Message> list;

        MessageAdapter(List<Message> list) { this.list = list; }

        @Override
        public int getItemViewType(int position) {
            return list.get(position).senderId.equals(currentUid) ? 1 : 0;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            boolean isMine = viewType == 1;
            LinearLayout bubble = new LinearLayout(parent.getContext());
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(16, 12, 16, 12);

            TextView tvText = new TextView(parent.getContext());
            tvText.setTextSize(15);
            tvText.setTextColor(isMine ? 0xFFFFFFFF : 0xFF000000);
            bubble.addView(tvText);

            TextView tvTime = new TextView(parent.getContext());
            tvTime.setTextSize(10);
            tvTime.setTextColor(isMine ? 0xFFE0E0E0 : 0xFF888888);
            tvTime.setPadding(0, 4, 0, 0);
            bubble.addView(tvTime);

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setPadding(8, 6, 8, 6);

            View spacer = new View(parent.getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));

            if (isMine) {
                bubble.setBackgroundColor(0xFFC2185B);
                row.addView(spacer);
                row.addView(bubble);
            } else {
                bubble.setBackgroundColor(0xFFFFFFFF);
                row.addView(bubble);
                row.addView(spacer);
            }

            return new ViewHolder(row, tvText, tvTime);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Message m = list.get(position);
            h.tvText.setText(m.text);
            h.tvTime.setText(android.text.format.DateFormat.getTimeFormat(h.itemView.getContext()).format(m.timestamp));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText, tvTime;

            ViewHolder(View itemView, TextView tvText, TextView tvTime) {
                super(itemView);
                this.tvText = tvText; this.tvTime = tvTime;
            }
        }
    }
}