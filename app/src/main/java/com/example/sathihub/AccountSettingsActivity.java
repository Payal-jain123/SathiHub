package com.example.sathihub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class AccountSettingsActivity extends AppCompatActivity {

    ImageView btnBack;
    EditText etCurrentEmail, etNewPassword, etConfirmPassword;
    Button btnChangePassword, btnLogout, btnDeleteAccount;
    Switch switchNotifications;

    FirebaseAuth auth;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        btnBack = findViewById(R.id.btnBack);
        etCurrentEmail = findViewById(R.id.etCurrentEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        switchNotifications = findViewById(R.id.switchNotifications);

        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("SathiHubPrefs", MODE_PRIVATE);

        // Load current user email
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            etCurrentEmail.setText(user.getEmail());
        }

        // Load notification preference
        boolean notifEnabled = prefs.getBoolean("notifications", true);
        switchNotifications.setChecked(notifEnabled);

        btnBack.setOnClickListener(v -> finish());

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> changePassword());

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        auth.signOut();
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this, LoginActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("This will permanently delete your account and all data. This action cannot be undone. Are you sure?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void changePassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter new password and confirm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPass)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        etNewPassword.setText("");
                        etConfirmPassword.setText("");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Delete user data from database
        FirebaseDatabase.getInstance().getReference("Users").child(uid).removeValue();
        FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid).removeValue();
        FirebaseDatabase.getInstance().getReference("ProfileImage").child(uid).removeValue();

        // Delete auth account
        user.delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account deleted permanently", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
