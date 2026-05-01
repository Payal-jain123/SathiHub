package com.example.sathihub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadPhotoActivity extends AppCompatActivity {

    ImageView imgProfile;
    Button btnSelectImage, btnSelectResume, btnSelectDoc, btnFinish;
    EditText etAbout;
    TextView tvResumeStatus, tvDocStatus;

    Uri imageUri, resumeUri, docUri;
    FirebaseAuth auth;

    // 🔴 CHECK THESE IN CLOUDINARY DASHBOARD
    String cloudName = "djgnxmqnf";
    String uploadPreset = "sathihub_preset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        imgProfile = findViewById(R.id.imgProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectResume = findViewById(R.id.btnSelectResume);
        btnSelectDoc = findViewById(R.id.btnSelectDoc);
        btnFinish = findViewById(R.id.btnFinish);
        etAbout = findViewById(R.id.etAbout);
        tvResumeStatus = findViewById(R.id.tvResumeStatus);
        tvDocStatus = findViewById(R.id.tvDocStatus);

        auth = FirebaseAuth.getInstance();

        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSelectResume.setOnClickListener(v -> selectResume());
        btnSelectDoc.setOnClickListener(v -> selectDoc());
        btnFinish.setOnClickListener(v -> uploadData());
    }

    private void selectImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, 1);
    }

    private void selectResume() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("application/pdf");
        startActivityForResult(i, 2);
    }

    private void selectDoc() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*"};
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(i, 3);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {

            if (requestCode == 1) {
                imageUri = data.getData();
                imgProfile.setImageURI(imageUri);
            }

            if (requestCode == 2) {
                resumeUri = data.getData();
                String fileName = resumeUri.getLastPathSegment();
                tvResumeStatus.setText("Selected: " + fileName);
                Toast.makeText(this, "Resume Selected", Toast.LENGTH_SHORT).show();
            }

            if (requestCode == 3) {
                docUri = data.getData();
                String fileName = docUri.getLastPathSegment();
                tvDocStatus.setText("Selected: " + fileName);
                Toast.makeText(this, "Verification Document Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadData() {

        if (imageUri == null || resumeUri == null || etAbout.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Select photo, resume & write about yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadToCloudinary(imageUri, "image", imageUrl -> {
            uploadToCloudinary(resumeUri, "raw", resumeUrl -> {

                String uid = auth.getCurrentUser().getUid();
                DatabaseReference profileImageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(uid);
                DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);
                DatabaseReference verificationRef = FirebaseDatabase.getInstance().getReference("Verification").child(uid);
                String about = etAbout.getText().toString();

                // Build list of tasks
                java.util.List<com.google.android.gms.tasks.Task<?>> tasks = new java.util.ArrayList<>();
                tasks.add(profileImageRef.child("imageUrl").setValue(imageUrl));
                tasks.add(profileImageRef.child("resumeUrl").setValue(resumeUrl));
                tasks.add(profileImageRef.child("aboutMe").setValue(about));
                tasks.add(personalRef.child("about").setValue(about));
                tasks.add(personalRef.child("imageUrl").setValue(imageUrl));
                tasks.add(FirebaseDatabase.getInstance().getReference("Users").child(uid).child("profileCompleted").setValue(true));

                // If verification doc selected, upload it too
                if (docUri != null) {
                    uploadToCloudinary(docUri, "raw", docUrl -> {
                        tasks.add(verificationRef.child("docUrl").setValue(docUrl));
                        tasks.add(verificationRef.child("status").setValue("pending"));
                        tasks.add(verificationRef.child("submittedAt").setValue(System.currentTimeMillis()));
                        tasks.add(personalRef.child("verificationStatus").setValue("pending"));
                        tasks.add(FirebaseDatabase.getInstance().getReference("Users").child(uid).child("verificationStatus").setValue("pending"));

                        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                            Toast.makeText(this, "Profile Completed. Verification under review.", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }).addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
                } else {
                    tasks.add(personalRef.child("verificationStatus").setValue("none"));
                    tasks.add(FirebaseDatabase.getInstance().getReference("Users").child(uid).child("verificationStatus").setValue("none"));

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        Toast.makeText(this, "Profile Completed", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
    }

    private void navigateToMain() {
        Intent i = new Intent(UploadPhotoActivity.this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void uploadToCloudinary(Uri fileUri, String resourceType, CloudinaryCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] fileBytes = readBytes(inputStream);

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "file",
                                RequestBody.create(fileBytes, MediaType.parse("*/*")))
                        .addFormDataPart("upload_preset", uploadPreset)
                        .build();

                String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload";

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("HTTP Error: " + response.code());
                }

                String json = response.body().string();
                JSONObject obj = new JSONObject(json);
                String fileUrl = obj.getString("secure_url");

                runOnUiThread(() -> callback.onSuccess(fileUrl));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];

        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    interface CloudinaryCallback {
        void onSuccess(String url);
    }
}
