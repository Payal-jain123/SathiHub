package com.example.sathihub;

public class User {

    public String userId;
    public String name;
    public String email;
    public String mobile;
    public String password;

    // 🔹 Empty constructor (Firebase ke liye zaroori)
    public User() {
    }

    // 🔹 New constructor with EMAIL
    public User(String userId, String name, String email, String mobile, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
    }
}
