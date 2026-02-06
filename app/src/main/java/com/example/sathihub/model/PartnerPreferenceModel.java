package com.example.sathihub.model;

public class PartnerPreferenceModel {

    public String age;
    public String height;
    public String religion;
    public String caste;
    public String location;
    public String education;
    public String occupation;

    // Empty constructor (Firebase ke liye compulsory)
    public PartnerPreferenceModel() {
    }

    public PartnerPreferenceModel(String age, String height, String religion,
                                  String caste, String location,
                                  String education, String occupation) {
        this.age = age;
        this.height = height;
        this.religion = religion;
        this.caste = caste;
        this.location = location;
        this.education = education;
        this.occupation = occupation;
    }
}
