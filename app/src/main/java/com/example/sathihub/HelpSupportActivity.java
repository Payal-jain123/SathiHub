package com.example.sathihub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class HelpSupportActivity extends AppCompatActivity {

    ImageView btnBack;
    Button btnEmailSupport, btnCallSupport, btnSubmitReport;
    EditText etProblem;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        btnBack = findViewById(R.id.btnBack);
        btnEmailSupport = findViewById(R.id.btnEmailSupport);
        btnCallSupport = findViewById(R.id.btnCallSupport);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        etProblem = findViewById(R.id.etProblem);

        auth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> finish());

        btnEmailSupport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@sathihub.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SathiHub Support Request");
            startActivity(Intent.createChooser(emailIntent, "Send email via"));
        });

        btnCallSupport.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+911234567890"));
            startActivity(callIntent);
        });

        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String problem = etProblem.getText().toString().trim();

        if (problem.isEmpty()) {
            Toast.makeText(this, "Please describe your issue", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "guest";

        FirebaseDatabase.getInstance().getReference("SupportReports")
                .push()
                .setValue(new Report(uid, email, problem, System.currentTimeMillis()))
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                    etProblem.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    static class Report {
        public String userId, userEmail, message;
        public long timestamp;

        public Report() {}

        public Report(String userId, String userEmail, String message, long timestamp) {
            this.userId = userId;
            this.userEmail = userEmail;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
