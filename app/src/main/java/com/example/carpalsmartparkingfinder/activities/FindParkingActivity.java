package com.example.carpalsmartparkingfinder.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.carpalsmartparkingfinder.R;
import com.example.carpalsmartparkingfinder.notifications.NotificationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.appbar.MaterialToolbar;
import android.content.pm.PackageManager;
import com.google.android.material.textfield.TextInputEditText;
import com.bumptech.glide.Glide;

public class FindParkingActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double UITM_LATITUDE = 3.0698;
    private static final double UITM_LONGITUDE = 101.5037;

    private MaterialToolbar toolbar;
    private TextView predictedAvailabilityText;
    private ImageView parkingImageView;
    private Uri galleryImageUri;
    private TextInputEditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_parking);

        NotificationHelper.createNotificationChannel(this);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        predictedAvailabilityText = findViewById(R.id.predictedAvailabilityText);
        parkingImageView = findViewById(R.id.parkingImageView);
        parkingImageView.setVisibility(ImageView.GONE); // Hide initially until an image is loaded
        searchBar = findViewById(R.id.searchBar);

        findViewById(R.id.searchButton).setOnClickListener(v -> performSearch());
        findViewById(R.id.takePictureButton).setOnClickListener(v -> openCamera());

        // Restore last image if available
        if (savedInstanceState != null) {
            galleryImageUri = savedInstanceState.getParcelable("galleryImageUri");
            if (galleryImageUri != null) {
                parkingImageView.setVisibility(ImageView.VISIBLE);
                Glide.with(this).load(galleryImageUri).into(parkingImageView);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (galleryImageUri != null) {
            outState.putParcelable("galleryImageUri", galleryImageUri);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create a ContentValues object to store the image in MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "ParkingImage_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
            values.put(MediaStore.Images.Media.DESCRIPTION, "Image captured from Parking App");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            galleryImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, galleryImageUri);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera application found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (galleryImageUri != null) {
                parkingImageView.setVisibility(ImageView.VISIBLE);
                Glide.with(this).load(galleryImageUri).into(parkingImageView);
                Toast.makeText(this, "Image saved to gallery and displayed", Toast.LENGTH_SHORT).show();
                // Optionally, scan the gallery to ensure it appears immediately
                MediaScannerConnection.scanFile(this, new String[]{galleryImageUri.getPath()}, null, null);
            } else {
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            setCurrentLocationToUITM();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        LatLng uitmLocation = new LatLng(UITM_LATITUDE, UITM_LONGITUDE);
        Marker uitmMarker = mMap.addMarker(new MarkerOptions().position(uitmLocation).title("UiTM Shah Alam"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitmLocation, 16));

        fetchParkingSpots();
    }

    private void setCurrentLocationToUITM() {
        LatLng uitmLocation = new LatLng(UITM_LATITUDE, UITM_LONGITUDE);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitmLocation, 16));
    }

    private void fetchParkingSpots() {
        db.collection("ParkingLots")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mMap.clear();
                    LatLng uitmLocation = new LatLng(UITM_LATITUDE, UITM_LONGITUDE);
                    mMap.addMarker(new MarkerOptions().position(uitmLocation).title("UiTM Shah Alam"));

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No parking spots found", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Double lat = doc.getDouble("latitude");
                            Double lng = doc.getDouble("longitude");
                            String name = doc.getString("name");
                            Long availableLong = doc.getLong("available");
                            Long totalLong = doc.getLong("capacity");

                            if (lat != null && lng != null && name != null && availableLong != null && totalLong != null) {
                                int available = availableLong.intValue();
                                int total = totalLong.intValue();
                                LatLng parkingLocation = new LatLng(lat, lng);

                                float markerColor = getMarkerColor(doc.getString("type"));

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(parkingLocation)
                                        .title(name)
                                        .snippet("Available: " + available + " / " + total)
                                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                                if (doc.getId() != null) {
                                    marker.setTag(doc.getId());
                                }

                                int predictedAvailability = predictAvailability(available, total);
                                predictedAvailabilityText.setText("Predicted Availability: " + predictedAvailability + " spots");
                            }
                        }
                    }

                    mMap.setOnMarkerClickListener(marker -> {
                        Log.d("Map", "Marker clicked: " + marker.getTitle());
                        if (marker != null) {
                            showActionDialog(marker);
                        }
                        return true;
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load parking spots: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void performSearch() {
        String searchQuery = searchBar.getText().toString().trim().toLowerCase();
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter a parking lot name", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("ParkingLots")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mMap.clear();
                    LatLng uitmLocation = new LatLng(UITM_LATITUDE, UITM_LONGITUDE);
                    mMap.addMarker(new MarkerOptions().position(uitmLocation).title("UiTM Shah Alam"));

                    boolean found = false;
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No parking lots found", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String name = doc.getString("name");
                            Double lat = doc.getDouble("latitude");
                            Double lng = doc.getDouble("longitude");
                            Long availableLong = doc.getLong("available");
                            Long totalLong = doc.getLong("capacity");

                            if (name != null && lat != null && lng != null && availableLong != null && totalLong != null) {
                                if (name.toLowerCase().contains(searchQuery)) {
                                    int available = availableLong.intValue();
                                    int total = totalLong.intValue();
                                    LatLng parkingLocation = new LatLng(lat, lng);

                                    float markerColor = getMarkerColor(doc.getString("type"));

                                    Marker searchMarker = mMap.addMarker(new MarkerOptions()
                                            .position(parkingLocation)
                                            .title(name)
                                            .snippet("Available: " + available + " / " + total)
                                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                                    if (doc.getId() != null) {
                                        searchMarker.setTag(doc.getId());
                                    }

                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(parkingLocation, 16));
                                    Toast.makeText(this, "Found: " + name, Toast.LENGTH_SHORT).show();
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {
                        Toast.makeText(this, "No parking lot found with name: " + searchQuery, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private float getMarkerColor(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type) {
            case "Student": return BitmapDescriptorFactory.HUE_GREEN;
            case "Staff": return BitmapDescriptorFactory.HUE_BLUE;
            default: return BitmapDescriptorFactory.HUE_YELLOW;
        }
    }

    private int predictAvailability(int available, int total) {
        double percentageFull = ((double) (total - available) / total) * 100;
        return percentageFull > 50 ? (int) (available * 0.7) : available;
    }

    private void showActionDialog(Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_parking_options, null);
        if (dialogView == null) {
            Toast.makeText(this, "Error: Dialog view not inflated", Toast.LENGTH_SHORT).show();
            return;
        }
        builder.setView(dialogView)
                .setTitle(marker.getTitle())
                .setMessage(marker.getSnippet());

        // Find and set click listeners for all buttons
        ImageButton btnShare = dialogView.findViewById(R.id.btnShare);
        Button btnDirections = dialogView.findViewById(R.id.btnDirections);
        Button btnReportFull = dialogView.findViewById(R.id.btnReportFull);
        Button btnUseSpot = dialogView.findViewById(R.id.btnUseSpot);

        // Verify buttons are found
        if (btnShare == null || btnDirections == null || btnReportFull == null || btnUseSpot == null) {
            Toast.makeText(this, "Error: One or more buttons not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = builder.create();

        btnShare.setOnClickListener(v -> {
            Log.d("Dialog", "Share clicked");
            shareParkingInfo(marker);
            dialog.dismiss();
        });

        btnDirections.setOnClickListener(v -> {
            Log.d("Dialog", "Directions clicked");
            double lat = marker.getPosition().latitude;
            double lng = marker.getPosition().longitude;
            getDirections(lat, lng);
            dialog.dismiss();
        });

        btnReportFull.setOnClickListener(v -> {
            Log.d("Dialog", "Report Full clicked");
            reportFull(marker);
            dialog.dismiss();
        });

        btnUseSpot.setOnClickListener(v -> {
            Log.d("Dialog", "Use Spot clicked");
            useParkingSpot(marker);
            dialog.dismiss();
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    private void getDirections(double lat, double lng) {
        if (lat == 0 || lng == 0) {
            Toast.makeText(this, "Invalid parking spot location", Toast.LENGTH_SHORT).show();
            return;
        }
        String uri = "google.navigation:q=" + lat + "," + lng;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            uri = "https://www.google.com/maps?q=" + lat + "," + lng;
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(webIntent);
        }
    }

    private void reportFull(Marker marker) {
        String docId = (String) marker.getTag();
        if (docId != null) {
            db.collection("ParkingLots").document(docId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long availableLong = documentSnapshot.getLong("available");
                            int available = availableLong != null ? availableLong.intValue() : 0;
                            if (available > 0) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("available", 0);
                                updates.put("status", false);
                                db.collection("ParkingLots").document(docId).update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            marker.setSnippet("Available: 0 / " + documentSnapshot.getLong("capacity"));
                                            marker.showInfoWindow();
                                            Toast.makeText(this, marker.getTitle() + " reported as full", Toast.LENGTH_SHORT).show();
                                            NotificationHelper.sendLocalNotification(this, "Parking Lot Full", "The parking spot " + marker.getTitle() + " has been reported as full!");
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(this, marker.getTitle() + " is already full", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void useParkingSpot(Marker marker) {
        String docId = (String) marker.getTag();
        if (docId != null) {
            db.collection("ParkingLots").document(docId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long availableLong = documentSnapshot.getLong("available");
                            int available = availableLong != null ? availableLong.intValue() : 0;
                            if (available > 0) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("available", available - 1);
                                db.collection("ParkingLots").document(docId).update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            marker.setSnippet("Available: " + (available - 1) + " / " + documentSnapshot.getLong("capacity"));
                                            marker.showInfoWindow();
                                            Toast.makeText(this, marker.getTitle() + " spot used", Toast.LENGTH_SHORT).show();
                                            NotificationHelper.sendLocalNotification(this, "Parking Spot Used", "The parking spot " + marker.getTitle() + " has been used!");
                                        }).addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(this, marker.getTitle() + " is full", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                setCurrentLocationToUITM();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareParkingInfo(Marker marker) {
        String shareText = "Check out this parking spot: " + marker.getTitle() + "! " +
                "\nAvailability: " + marker.getSnippet() +
                "\nLocation: https://maps.google.com/?q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        // Add image if available
        if (galleryImageUri != null) {
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, galleryImageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Share " + marker.getTitle()));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No app available to share.", Toast.LENGTH_SHORT).show();
        }
    }
}