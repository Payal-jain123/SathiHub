package com.example.sathihub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    ImageView imgProfileEdit;
    Button btnEditPhoto, btnSave;
    EditText etAbout, etName, etCity, etAge, etHeight,
            etReligion, etCaste, etEducation, etOccupation,
            etFatherOcc, etMotherOcc, etPartnerAge, etPartnerReligion;

    FirebaseAuth auth;
    DatabaseReference userRef, personalRef, imageRef;
    StorageReference storageRef;

    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imgProfileEdit = findViewById(R.id.imgProfileEdit);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        btnSave = findViewById(R.id.btnSave);

        etAbout = findViewById(R.id.etAbout);
        etName = findViewById(R.id.etName);
        etCity = findViewById(R.id.etCity);
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etReligion = findViewById(R.id.etReligion);
        etCaste = findViewById(R.id.etCaste);
        etEducation = findViewById(R.id.etEducation);
        etOccupation = findViewById(R.id.etOccupation);
        etFatherOcc = findViewById(R.id.etFatherOcc);
        etMotherOcc = findViewById(R.id.etMotherOcc);
        etPartnerAge = findViewById(R.id.etPartnerAge);
        etPartnerReligion = findViewById(R.id.etPartnerReligion);

        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);
        imageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("ProfileImages").child(uid);

        loadData();

        btnEditPhoto.setOnClickListener(v -> openGallery());

        btnSave.setOnClickListener(v -> saveData());
    }

    private void loadData() {

        userRef.child("name").get().addOnSuccessListener(s ->
                etName.setText(s.getValue(String.class)));

        personalRef.get().addOnSuccessListener(snapshot -> {
            etAge.setText(snapshot.child("age").getValue(String.class));
            etHeight.setText(snapshot.child("height").getValue(String.class));
            etCity.setText(snapshot.child("city").getValue(String.class));
            etReligion.setText(snapshot.child("religion").getValue(String.class));
            etCaste.setText(snapshot.child("caste").getValue(String.class));
            etEducation.setText(snapshot.child("education").getValue(String.class));
            etOccupation.setText(snapshot.child("occupation").getValue(String.class));
            etFatherOcc.setText(snapshot.child("fatherOccupation").getValue(String.class));
            etMotherOcc.setText(snapshot.child("motherOccupation").getValue(String.class));
            etPartnerAge.setText(snapshot.child("partnerAge").getValue(String.class));
            etPartnerReligion.setText(snapshot.child("partnerReligion").getValue(String.class));
            etAbout.setText(snapshot.child("about").getValue(String.class));
        });

        imageRef.child("imageUrl").get().addOnSuccessListener(snapshot -> {
            String url = snapshot.getValue(String.class);
            if (url != null) {
                Glide.with(this).load(url).into(imgProfileEdit);
            }
        });
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");
        startActivityForResult(i, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            imageUri = data.getData();
            imgProfileEdit.setImageURI(imageUri);
        }
    }

    private void saveData() {

        userRef.child("name").setValue(etName.getText().toString());

        personalRef.child("age").setValue(etAge.getText().toString());
        personalRef.child("height").setValue(etHeight.getText().toString());
        personalRef.child("city").setValue(etCity.getText().toString());
        personalRef.child("religion").setValue(etReligion.getText().toString());
        personalRef.child("caste").setValue(etCaste.getText().toString());
        personalRef.child("education").setValue(etEducation.getText().toString());
        personalRef.child("occupation").setValue(etOccupation.getText().toString());
        personalRef.child("fatherOccupation").setValue(etFatherOcc.getText().toString());
        personalRef.child("motherOccupation").setValue(etMotherOcc.getText().toString());
        personalRef.child("partnerAge").setValue(etPartnerAge.getText().toString());
        personalRef.child("partnerReligion").setValue(etPartnerReligion.getText().toString());
        personalRef.child("about").setValue(etAbout.getText().toString());

        if (imageUri != null) {
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(task ->
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                                String imageUrl = uri.toString();

                                // ✅ 1. save in ProfileImage
                                imageRef.child("imageUrl").setValue(imageUrl);

                                // ✅ 2. save in Users (for MainActivity)
                                userRef.child("profilePhoto").setValue(imageUrl);

                                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                    );
        } else {
            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
