package com.example.carpalsmartparkingfinder.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carpalsmartparkingfinder.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditParkingLotActivity extends AppCompatActivity {

    private EditText editName, editType, editAvailable, editCapacity, editLatitude, editLongitude;
    private Button btnSave;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;
    private AdminPanelActivity.ParkingLot lot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_parking_lot);

        db = FirebaseFirestore.getInstance();
        editName = findViewById(R.id.editName);
        editType = findViewById(R.id.editType);
        editAvailable = findViewById(R.id.editAvailable);
        editCapacity = findViewById(R.id.editCapacity);
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        btnSave = findViewById(R.id.btnSave);
        toolbar = findViewById(R.id.topAppBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Parking Lot");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Get the lot from Intent
        lot = getIntent().getParcelableExtra("lot");
        if (lot != null) {
            editName.setText(lot.getName());
            editType.setText(lot.getType());
            editLatitude.setText(String.valueOf(lot.getLatitude()));
            editLongitude.setText(String.valueOf(lot.getLongitude()));
            editAvailable.setText(String.valueOf(lot.getAvailable()));
            editCapacity.setText(String.valueOf(lot.getCapacity()));
        }

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String type = editType.getText().toString().trim();
            String availableStr = editAvailable.getText().toString().trim();
            String capacityStr = editCapacity.getText().toString().trim();
            String latitudeStr = editLatitude.getText().toString().trim();
            String longitudeStr = editLongitude.getText().toString().trim();

            if (name.isEmpty() || type.isEmpty() || availableStr.isEmpty() || capacityStr.isEmpty() ||
                    latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            int available = Integer.parseInt(availableStr);
            int capacity = Integer.parseInt(capacityStr);
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);
            boolean status = available > 0;

            AdminPanelActivity.ParkingLot updatedLot = new AdminPanelActivity.ParkingLot(name, type, latitude, longitude, available, capacity, status);
            db.collection("ParkingLots")
                    .document(name)
                    .set(updatedLot)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Lot updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update lot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}