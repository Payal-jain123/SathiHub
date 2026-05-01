package com.example.sathihub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;

public class EditProfileActivity extends AppCompatActivity {

    ImageView imgProfileEdit;
    Button btnEditPhoto, btnSave;
    EditText etAbout, etName, etCity, etAge, etHeight,
            etReligion, etCaste, etEducation, etOccupation,
            etFatherOcc, etMotherOcc, etPartnerAge, etPartnerReligion;

    FirebaseAuth auth;
    String uid;
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
        uid = auth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        personalRef = FirebaseDatabase.getInstance().getReference("PersonalInfo").child(uid);
        imageRef = FirebaseDatabase.getInstance().getReference("ProfileImage").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("ProfileImages").child(uid);

        loadData();

        btnEditPhoto.setOnClickListener(v -> openGallery());

        btnSave.setOnClickListener(v -> saveData());
    }

    private void loadData() {
        // Load name
        userRef.child("name").get().addOnSuccessListener(s ->
                etName.setText(s.getValue(String.class)));

        // Load all profile data from multiple nodes in parallel
        DatabaseReference religiousRef = userRef.child("religious");
        DatabaseReference educationRef = userRef.child("education");
        DatabaseReference familyRef = userRef.child("family");
        DatabaseReference partnerRef = userRef.child("partnerPreference");

        Tasks.whenAllSuccess(
            Arrays.asList(personalRef.get(), religiousRef.get(), educationRef.get(), familyRef.get(), partnerRef.get(), imageRef.get())
        ).addOnSuccessListener(results -> {
            DataSnapshot personalSnap = (DataSnapshot) results.get(0);
            DataSnapshot religiousSnap = (DataSnapshot) results.get(1);
            DataSnapshot educationSnap = (DataSnapshot) results.get(2);
            DataSnapshot familySnap = (DataSnapshot) results.get(3);
            DataSnapshot partnerSnap = (DataSnapshot) results.get(4);
            DataSnapshot imageSnap = (DataSnapshot) results.get(5);

            // Read with fallback
            etAge.setText(getValue(personalSnap, "age"));
            etHeight.setText(getValue(personalSnap, "height"));
            etCity.setText(firstNotNull(getValue(personalSnap, "city"), getValue(educationSnap, "city")));
            etReligion.setText(firstNotNull(getValue(personalSnap, "religion"), getValue(religiousSnap, "religion")));
            etCaste.setText(firstNotNull(getValue(personalSnap, "caste"), getValue(religiousSnap, "caste")));
            etEducation.setText(firstNotNull(getValue(personalSnap, "education"), getValue(educationSnap, "qualification")));
            etOccupation.setText(firstNotNull(getValue(personalSnap, "occupation"), getValue(educationSnap, "occupation")));
            etFatherOcc.setText(firstNotNull(getValue(personalSnap, "fatherOccupation"), getValue(familySnap, "fatherOccupation")));
            etMotherOcc.setText(firstNotNull(getValue(personalSnap, "motherOccupation"), getValue(familySnap, "motherOccupation")));
            etPartnerAge.setText(firstNotNull(getValue(personalSnap, "partnerAge"), getValue(partnerSnap, "ageRange")));
            etPartnerReligion.setText(firstNotNull(getValue(personalSnap, "partnerReligion"), getValue(partnerSnap, "religion")));
            etAbout.setText(firstNotNull(getValue(personalSnap, "about"), getValue(imageSnap, "aboutMe")));
        });

        // Load image
        imageRef.child("imageUrl").get().addOnSuccessListener(snapshot -> {
            String url = snapshot.getValue(String.class);
            if (url != null) {
                Glide.with(this).load(url).into(imgProfileEdit);
            }
        });
    }

    private String getValue(DataSnapshot snapshot, String key) {
        if (!snapshot.exists()) return null;
        String val = snapshot.child(key).getValue(String.class);
        return (val != null && !val.isEmpty()) ? val : null;
    }

    private String firstNotNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return "";
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
        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String religion = etReligion.getText().toString().trim();
        String caste = etCaste.getText().toString().trim();
        String education = etEducation.getText().toString().trim();
        String occupation = etOccupation.getText().toString().trim();
        String fatherOcc = etFatherOcc.getText().toString().trim();
        String motherOcc = etMotherOcc.getText().toString().trim();
        String partnerAge = etPartnerAge.getText().toString().trim();
        String partnerReligion = etPartnerReligion.getText().toString().trim();
        String about = etAbout.getText().toString().trim();

        // Save name
        userRef.child("name").setValue(name);

        // Save to PersonalInfo (summary node for ProfileActivity/EditProfileActivity)
        personalRef.child("age").setValue(age);
        personalRef.child("height").setValue(height);
        personalRef.child("city").setValue(city);
        personalRef.child("religion").setValue(religion);
        personalRef.child("caste").setValue(caste);
        personalRef.child("education").setValue(education);
        personalRef.child("occupation").setValue(occupation);
        personalRef.child("fatherOccupation").setValue(fatherOcc);
        personalRef.child("motherOccupation").setValue(motherOcc);
        personalRef.child("partnerAge").setValue(partnerAge);
        personalRef.child("partnerReligion").setValue(partnerReligion);
        personalRef.child("about").setValue(about);

        // ✅ ALSO save to original nodes so SearchActivity/MatchesActivity see updated data
        userRef.child("religious").child("religion").setValue(religion);
        userRef.child("religious").child("caste").setValue(caste);
        userRef.child("education").child("qualification").setValue(education);
        userRef.child("education").child("occupation").setValue(occupation);
        userRef.child("education").child("city").setValue(city);
        userRef.child("family").child("fatherOccupation").setValue(fatherOcc);
        userRef.child("family").child("motherOccupation").setValue(motherOcc);
        userRef.child("partnerPreference").child("ageRange").setValue(partnerAge);
        userRef.child("partnerPreference").child("religion").setValue(partnerReligion);
        imageRef.child("aboutMe").setValue(about);

        if (imageUri != null) {
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(task ->
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                                String imageUrl = uri.toString();

                                // Save in ProfileImage
                                imageRef.child("imageUrl").setValue(imageUrl);

                                // Save in Users (for MainActivity)
                                userRef.child("profilePhoto").setValue(imageUrl);

                                // Save in PersonalInfo so all activities can find it
                                personalRef.child("imageUrl").setValue(imageUrl);

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
