package com.example.carpalsmartparkingfinder.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carpalsmartparkingfinder.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddParkingLotActivity extends AppCompatActivity {

    private EditText editName, editType, editAvailable, editCapacity, editLatitude, editLongitude;
    private Button btnSave;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parking_lot);

        // Initialize Firestore and UI elements
        db = FirebaseFirestore.getInstance();
        editName = findViewById(R.id.editName);
        editType = findViewById(R.id.editType);
        editAvailable = findViewById(R.id.editAvailable);
        editCapacity = findViewById(R.id.editCapacity);
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        btnSave = findViewById(R.id.btnSave);
        toolbar = findViewById(R.id.topAppBar);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Parking Lot");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Save button click listener
        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String type = editType.getText().toString().trim();
            String availableStr = editAvailable.getText().toString().trim();
            String capacityStr = editCapacity.getText().toString().trim();
            String latitudeStr = editLatitude.getText().toString().trim();
            String longitudeStr = editLongitude.getText().toString().trim();

            // Validation for empty fields
            if (name.isEmpty() || type.isEmpty() || availableStr.isEmpty() || capacityStr.isEmpty() ||
                    latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try-catch blocks for parsing to handle invalid input gracefully
            int available = 0;
            int capacity = 0;
            double latitude = 0.0;
            double longitude = 0.0;

            try {
                available = Integer.parseInt(availableStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for available spots", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                capacity = Integer.parseInt(capacityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for capacity", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                latitude = Double.parseDouble(latitudeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for latitude", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                longitude = Double.parseDouble(longitudeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for longitude", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine status based on availability
            boolean status = available > 0;

            // Create ParkingLot object
            AdminPanelActivity.ParkingLot lot = new AdminPanelActivity.ParkingLot(name, type, latitude, longitude, available, capacity, status);

            // Save to Firestore
            db.collection("ParkingLots")
                    .document(name)
                    .set(lot)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Lot added successfully", Toast.LENGTH_SHORT).show();
                        finish();  // Close the activity after saving
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add lot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
