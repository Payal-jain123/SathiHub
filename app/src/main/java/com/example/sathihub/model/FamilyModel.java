package com.example.sathihub.model;

public class FamilyModel {

    public String fatherOccupation, motherOccupation, familyType,
            brothers, sisters, familyIncome, familyCity, familyStatus;

    public FamilyModel() {}

    public FamilyModel(String fatherOccupation, String motherOccupation,
                       String familyType, String brothers, String sisters,
                       String familyIncome, String familyCity, String familyStatus) {
        this.fatherOccupation = fatherOccupation;
        this.motherOccupation = motherOccupation;
        this.familyType = familyType;
        this.brothers = brothers;
        this.sisters = sisters;
        this.familyIncome = familyIncome;
        this.familyCity = familyCity;
        this.familyStatus = familyStatus;
    }
}
