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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadPhotoActivity extends AppCompatActivity {

    ImageView imgProfile;
    Button btnSelectImage, btnSelectResume, btnFinish;
    EditText etAbout;
    TextView tvResumeStatus;

    Uri imageUri, resumeUri;
    FirebaseAuth auth;

    String cloudName = "djgnxmqnf";
    String uploadPreset = "sathihub_preset";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        imgProfile = findViewById(R.id.imgProfile);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectResume = findViewById(R.id.btnSelectResume);
        btnFinish = findViewById(R.id.btnFinish);
        etAbout = findViewById(R.id.etAbout);
        tvResumeStatus = findViewById(R.id.tvResumeStatus);

        auth = FirebaseAuth.getInstance();

        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSelectResume.setOnClickListener(v -> selectResume());
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

                FirebaseDatabase.getInstance().getReference("users")
                        .child(uid).child("profilePhoto").setValue(imageUrl);

                FirebaseDatabase.getInstance().getReference("users")
                        .child(uid).child("resume").setValue(resumeUrl);

                FirebaseDatabase.getInstance().getReference("users")
                        .child(uid).child("aboutMe").setValue(etAbout.getText().toString());

                FirebaseDatabase.getInstance().getReference("users")
                        .child(uid).child("profileCompleted").setValue(true);

                Toast.makeText(this, "Profile Completed", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(UploadPhotoActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();

            });
        });
    }

    private void uploadToCloudinary(Uri fileUri, String resourceType, CloudinaryCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);

                OkHttpClient client = new OkHttpClient();

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
                String json = response.body().string();

                JSONObject obj = new JSONObject(json);
                String fileUrl = obj.getString("secure_url");

                runOnUiThread(() -> callback.onSuccess(fileUrl));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    interface CloudinaryCallback {
        void onSuccess(String url);
    }
}
