package com.example.carpalsmartparkingfinder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carpalsmartparkingfinder.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerLots;
    private ParkingLotAdapter adapter;
    private TextView emptyMessageTextView;
    private EditText searchBar;
    private Button btnAddLot, btnSeedDatabase;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        Log.d("AdminPanelActivity", "Activity created");

        // Initialize Firebase Firestore instance
        db = FirebaseFirestore.getInstance();

        // Initialize the views
        recyclerLots = findViewById(R.id.recyclerLots);
        emptyMessageTextView = findViewById(R.id.emptyMessageTextView);
        searchBar = findViewById(R.id.searchBar);
        btnAddLot = findViewById(R.id.btnAddLot);
        btnSeedDatabase = findViewById(R.id.btnSeedDatabase);
        toolbar = findViewById(R.id.topAppBar);

        // Set up MaterialToolbar as the action bar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable the "up" button (back button)
        }

        // Back button functionality
        toolbar.setNavigationOnClickListener(v -> navigateToMainActivity());

        // Set up RecyclerView with an adapter
        recyclerLots.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParkingLotAdapter(new ArrayList<>(), this::onEditClick, this::onDeleteClick, this::onUseClick);
        recyclerLots.setAdapter(adapter);

        // Add text watcher to filter parking lots by name
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Load parking lots in real-time
        loadParkingLotsInRealTime();

        // Set up button click listeners
        btnAddLot.setOnClickListener(v -> startActivity(new Intent(this, AddParkingLotActivity.class)));
        btnSeedDatabase.setOnClickListener(v -> seedDatabaseWithMockData());
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(AdminPanelActivity.this, MainActivity.class);
        // Add flags to clear the activity stack and start MainActivity as a new task
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadParkingLotsInRealTime() {
        db.collection("ParkingLots")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(AdminPanelActivity.this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<ParkingLot> lots = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            ParkingLot lot = doc.toObject(ParkingLot.class);
                            if (lot != null) lots.add(lot);
                        }
                        updateUI(lots);
                    }
                });
    }

    private void updateUI(List<ParkingLot> lots) {
        if (lots.isEmpty()) {
            emptyMessageTextView.setVisibility(View.VISIBLE);
            recyclerLots.setVisibility(View.GONE);
        } else {
            emptyMessageTextView.setVisibility(View.GONE);
            recyclerLots.setVisibility(View.VISIBLE);
            adapter.updateData(lots);
        }
    }

    private void seedDatabaseWithMockData() {
        List<Map<String, Object>> mockData = new ArrayList<>();
        mockData.add(createParkingLotMap("Dataran Cendekia", "Student", 3.0698, 101.4973, 150, 300, true));
        mockData.add(createParkingLotMap("Kolej Perindu", "Student", 3.0658, 101.5005, 20, 200, true));
        mockData.add(createParkingLotMap("Kolej Melati", "Student", 3.0714, 101.4978, 100, 200, true));
        mockData.add(createParkingLotMap("Kolej Mawar", "Student", 3.0694, 101.4958, 150, 300, true));
        mockData.add(createParkingLotMap("Kolej Teratai", "Student", 3.0699, 101.4993, 100, 200, true));
        mockData.add(createParkingLotMap("Kolej Seroja", "Student", 3.0691, 101.5059, 120, 250, true));
        mockData.add(createParkingLotMap("Padang Kawad UITM Shah Alam", "Field", 3.0660, 101.4963, 80, 200, true));
        mockData.add(createParkingLotMap("Faculty of Computer and Mathematical Sciences (FCS)", "Faculty", 3.0738, 101.5007, 50, 100, true));
        mockData.add(createParkingLotMap("Faculty of Electrical Engineering (FE)", "Faculty", 3.0729, 101.4973, 60, 120, true));

        db.collection("ParkingLots")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        for (Map<String, Object> lot : mockData) {
                            db.collection("ParkingLots")
                                    .document((String) lot.get("name"))
                                    .set(lot)
                                    .addOnFailureListener(e -> Toast.makeText(this, "Seeding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                        Toast.makeText(this, "Database seeded successfully", Toast.LENGTH_SHORT).show();
                        loadParkingLotsInRealTime();
                    } else {
                        Toast.makeText(this, "Database already contains data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking database: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private Map<String, Object> createParkingLotMap(String name, String type, double latitude, double longitude, int available, int capacity, boolean status) {
        Map<String, Object> lot = new HashMap<>();
        lot.put("name", name);
        lot.put("type", type);
        lot.put("latitude", latitude);
        lot.put("longitude", longitude);
        lot.put("available", available);
        lot.put("capacity", capacity);
        lot.put("status", status);
        return lot;
    }

    // Handle Edit, Delete, and Use buttons for each parking lot
    private void onEditClick(ParkingLot lot) {
        Intent intent = new Intent(this, EditParkingLotActivity.class);
        intent.putExtra("lot", lot);
        startActivity(intent);
    }

    private void onDeleteClick(ParkingLot lot) {
        db.collection("ParkingLots")
                .document(lot.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Lot deleted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete lot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onUseClick(ParkingLot lot) {
        if (lot.getAvailable() > 0) {
            int newAvailable = lot.getAvailable() - 1;
            lot = new ParkingLot(lot.getName(), lot.getType(), lot.getLatitude(), lot.getLongitude(), newAvailable, lot.getCapacity(), newAvailable > 0);
            db.collection("ParkingLots")
                    .document(lot.getName())
                    .set(lot)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Spot used, " + newAvailable + " remaining", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update spot: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No available spots left", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Handle back navigation
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ParkingLot implements Parcelable {
        private String name;
        private String type;
        private int available;
        private int capacity;
        private boolean status;
        private double latitude;
        private double longitude;

        public ParkingLot() {}

        public ParkingLot(String name, String type, double latitude, double longitude, int available, int capacity, boolean status) {
            this.name = name;
            this.type = type;
            this.latitude = latitude;
            this.longitude = longitude;
            this.available = available;
            this.capacity = capacity;
            this.status = status;
        }

        protected ParkingLot(Parcel in) {
            name = in.readString();
            type = in.readString();
            latitude = in.readDouble();
            longitude = in.readDouble();
            available = in.readInt();
            capacity = in.readInt();
            status = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(type);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeInt(available);
            dest.writeInt(capacity);
            dest.writeByte((byte) (status ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<ParkingLot> CREATOR = new Creator<ParkingLot>() {
            @Override
            public ParkingLot createFromParcel(Parcel in) {
                return new ParkingLot(in);
            }

            @Override
            public ParkingLot[] newArray(int size) {
                return new ParkingLot[size];
            }
        };

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public int getAvailable() { return available; }
        public int getCapacity() { return capacity; }
        public boolean isStatus() { return status; }
    }

    public static class ParkingLotAdapter extends RecyclerView.Adapter<ParkingLotAdapter.ViewHolder> {
        private List<ParkingLot> lots;
        private List<ParkingLot> filteredLots;
        private OnEditClickListener editListener;
        private OnDeleteClickListener deleteListener;
        private OnUseClickListener useListener;

        public ParkingLotAdapter(List<ParkingLot> lots, OnEditClickListener editListener, OnDeleteClickListener deleteListener, OnUseClickListener useListener) {
            this.lots = new ArrayList<>(lots);
            this.filteredLots = new ArrayList<>(lots);
            this.editListener = editListener;
            this.deleteListener = deleteListener;
            this.useListener = useListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking_lot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ParkingLot lot = filteredLots.get(position);
            holder.nameTextView.setText(lot.getName());
            holder.typeTextView.setText(lot.getType());
            holder.availabilityTextView.setText(lot.getAvailable() + " / " + lot.getCapacity());
            holder.statusTextView.setText(lot.isStatus() ? "Open" : "Full");

            // Set status text color based on availability
            holder.statusTextView.setTextColor(lot.isStatus() ? 0xFF4CAF50 : 0xFFF44336); // Green for open, Red for full

            // Use card color from XML
            holder.cardView.setCardBackgroundColor(0xFFF0F4F8); // Light blueish background, no change

            // Set button click listeners
            holder.useButton.setOnClickListener(v -> useListener.onUseClick(lot));
            holder.editButton.setOnClickListener(v -> editListener.onEditClick(lot));
            holder.deleteButton.setOnClickListener(v -> deleteListener.onDeleteClick(lot));
        }

        @Override
        public int getItemCount() {
            return filteredLots.size();
        }

        public void updateData(List<ParkingLot> newLots) {
            lots.clear();
            lots.addAll(newLots);
            filteredLots.clear();
            filteredLots.addAll(newLots);
            notifyDataSetChanged();
        }

        public void filter(String query) {
            filteredLots.clear();
            if (query.isEmpty()) {
                filteredLots.addAll(lots);
            } else {
                String lowerCaseQuery = query.toLowerCase();
                for (ParkingLot lot : lots) {
                    if (lot.getName().toLowerCase().contains(lowerCaseQuery)) {
                        filteredLots.add(lot);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView nameTextView, typeTextView, availabilityTextView, statusTextView;
            Button useButton, editButton, deleteButton;

            public ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                nameTextView = itemView.findViewById(R.id.textName);
                typeTextView = itemView.findViewById(R.id.textType);
                availabilityTextView = itemView.findViewById(R.id.textAvailability);
                statusTextView = itemView.findViewById(R.id.textStatus);
                useButton = itemView.findViewById(R.id.btnUse);
                editButton = itemView.findViewById(R.id.btnEdit);
                deleteButton = itemView.findViewById(R.id.btnDelete);
            }
        }

        interface OnEditClickListener {
            void onEditClick(ParkingLot lot);
        }

        interface OnDeleteClickListener {
            void onDeleteClick(ParkingLot lot);
        }

        interface OnUseClickListener {
            void onUseClick(ParkingLot lot);
        }
    }
}
