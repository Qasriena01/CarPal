package com.example.carpalsmartparkingfinder.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpalsmartparkingfinder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText username, phone, email, password;
    private Button registerBtn;
    private TextView loginRedirect;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");

        // Redirect if already logged in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // Bind UI
        username = findViewById(R.id.username);
        phone = findViewById(R.id.phone);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerBtn = findViewById(R.id.registerBtn);
        loginRedirect = findViewById(R.id.loginRedirect);

        registerBtn.setOnClickListener(v -> registerUser());

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String userName = username.getText().toString().trim();
        String userPhone = phone.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        // Input validation
        if (userName.isEmpty()) {
            username.setError("Username is required");
            username.requestFocus();
            return;
        }
        if (!Patterns.PHONE.matcher(userPhone).matches()) {
            phone.setError("Invalid phone number");
            phone.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Invalid email");
            email.requestFocus();
            return;
        }
        if (userPass.length() < 6) {
            password.setError("Password must be at least 6 characters");
            password.requestFocus();
            return;
        }

        progressDialog.show();

        // 🔍 Check if username already exists
        db.collection("Users")
                .whereEqualTo("name", userName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        username.setError("Username already taken");
                        username.requestFocus();
                    } else {
                        createAuth(userName, userPhone, userEmail, userPass);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createAuth(String name, String phone, String email, String pass) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification() // 👈 Send verification email
                                .addOnSuccessListener(aVoid -> {
                                    saveUserToFirestore(user, name, phone);
                                    // Show a message indicating the verification email has been sent
                                    Toast.makeText(this, "Email verification sent to: " + email, Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void saveUserToFirestore(FirebaseUser user, String name, String phone) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("role", "user");
        userMap.put("createdAt", FieldValue.serverTimestamp());

        db.collection("Users").document(user.getUid()).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    mAuth.signOut();
                    progressDialog.dismiss();
                    Toast.makeText(this, "Account created. Please log in after verifying your email.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Saved Auth but Firestore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}