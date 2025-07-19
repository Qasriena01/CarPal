package com.example.carpalsmartparkingfinder.activities;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View; // Added this import
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.carpalsmartparkingfinder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Button btnOpenMap, btnAdmin, btnLogout;
    private Switch notificationToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // UI binding
        welcomeText = findViewById(R.id.welcomeText);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnLogout = findViewById(R.id.btnLogout);
        notificationToggle = findViewById(R.id.notificationToggle);

        // Hide Admin Panel button by default
        btnAdmin.setVisibility(View.GONE);

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Button actions
        btnOpenMap.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FindParkingActivity.class));
        });

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AdminPanelActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Notification toggle listener
        notificationToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                subscribeToNotifications();
            } else {
                unsubscribeFromNotifications();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();

        // If the user is not logged in, go to LoginActivity
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("name");
                            String role = document.getString("role");

                            // Set the welcome text
                            welcomeText.setText("Welcome, " + (username != null ? username : "User"));

                            // Show Admin button if the user is an admin
                            if ("admin".equalsIgnoreCase(role)) {
                                btnAdmin.setVisibility(View.VISIBLE);
                            } else {
                                btnAdmin.setVisibility(View.GONE);
                            }

                            // Debugging: Log role information
                            Log.d("MainActivity", "User role: " + role);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to fetch user info", e);
                    });
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notificationToggle.setEnabled(true); // Enable toggle only if permission is granted
            } else {
                notificationToggle.setEnabled(false); // Disable toggle if permission denied
                notificationToggle.setChecked(false);
            }
        }
    }

    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("parkingUpdates")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "Subscribed to parkingUpdates topic");
                    } else {
                        Log.e("MainActivity", "Subscription failed", task.getException());
                        notificationToggle.setChecked(false); // Revert toggle if subscription fails
                    }
                });
    }

    private void unsubscribeFromNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("parkingUpdates")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("MainActivity", "Unsubscribed from parkingUpdates topic");
                    } else {
                        Log.e("MainActivity", "Unsubscription failed", task.getException());
                        notificationToggle.setChecked(true); // Revert toggle if unsubscription fails
                    }
                });
    }
}