package com.example.carpalsmartparkingfinder.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpalsmartparkingfinder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginBtn;
    private TextView registerRedirect, forgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerRedirect = findViewById(R.id.registerRedirect);
        forgotPassword = findViewById(R.id.forgotPassword);

        loginBtn.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(username)) {
                usernameInput.setError("Username required");
                return;
            }

            if (password.length() < 6) {
                passwordInput.setError("Password must be at least 6 characters");
                return;
            }

            progressDialog.show();

            db.collection("Users")
                    .whereEqualTo("name", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                        } else {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                String email = doc.getString("email");
                                if (email != null) {
                                    signInWithEmail(email, password);
                                    break;
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error finding user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        registerRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        forgotPassword.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Enter your username to reset password", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Checking username...");
            progressDialog.show();

            db.collection("Users")
                    .whereEqualTo("name", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        progressDialog.dismiss();
                        if (!querySnapshot.isEmpty()) {
                            String email = querySnapshot.getDocuments().get(0).getString("email");
                            if (email != null) {
                                sendResetEmail(email);
                            } else {
                                Toast.makeText(this, "Email not found for this user", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to find username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        mAuth.signOut();
                        progressDialog.dismiss();
                        Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Reset link sent to: " + email, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send reset link: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
