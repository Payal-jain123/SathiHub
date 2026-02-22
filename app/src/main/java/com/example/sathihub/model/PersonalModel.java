package com.example.sathihub.model;

public class PersonalModel {
    public String name, gender, dob, age, height, weight, bloodGroup, maritalStatus, disability, religion;

    public PersonalModel() {}

    public PersonalModel(String name, String gender, String dob, String age,
                         String height, String weight, String bloodGroup,
                         String maritalStatus, String disability) {
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.bloodGroup = bloodGroup;
        this.maritalStatus = maritalStatus;
        this.disability = disability;
    }

}
