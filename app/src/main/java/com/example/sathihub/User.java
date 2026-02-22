package com.example.sathihub;

public class User {

    public String userId;
    public String name;
    public String email;
    public String mobile;
    public String password;

    private boolean profileCompleted;

    // 🔹 Empty constructor (Firebase ke liye zaroori)
    public User() {
    }

    // 🔹 Constructor
    public User(String userId, String name, String email, String mobile, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.profileCompleted = false; // 👈 IMPORTANT (default false)
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
