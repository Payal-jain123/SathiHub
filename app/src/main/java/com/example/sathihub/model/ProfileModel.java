package com.example.sathihub.model;

public class ProfileModel {

    private String name;
    private String profession;
    private String education;
    private String religion;
    private String height;
    private String income;
    private String maritalStatus;
    private String gender;

    public ProfileModel(String name, String profession, String education,
                        String religion, String height,
                        String income, String maritalStatus, String gender) {
        this.name = name;
        this.profession = profession;
        this.education = education;
        this.religion = religion;
        this.height = height;
        this.income = income;
        this.maritalStatus = maritalStatus;
        this.gender = gender;
    }

    public String getName() { return name; }
    public String getProfession() { return profession; }
    public String getEducation() { return education; }
    public String getReligion() { return religion; }
    public String getHeight() { return height; }
    public String getIncome() { return income; }
    public String getMaritalStatus() { return maritalStatus; }
    public String getGender() { return gender; }
}
