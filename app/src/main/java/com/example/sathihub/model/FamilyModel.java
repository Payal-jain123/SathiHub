package com.example.sathihub.model;

public class FamilyModel {

    public String fatherOccupation;
    public String motherOccupation;
    public String familyType;
    public String brothers;
    public String sisters;
    public String income;
    public String city;
    public String status;

    // empty constructor (Firebase ke liye compulsory)
    public FamilyModel() {
    }

    public FamilyModel(String fatherOccupation, String motherOccupation,
                       String familyType, String brothers, String sisters,
                       String income, String city, String status) {

        this.fatherOccupation = fatherOccupation;
        this.motherOccupation = motherOccupation;
        this.familyType = familyType;
        this.brothers = brothers;
        this.sisters = sisters;
        this.income = income;
        this.city = city;
        this.status = status;
    }
}
