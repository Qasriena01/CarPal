package com.example.carpalsmartparkingfinder.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;  // Import Log for logging

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpalsmartparkingfinder.R;
import com.example.carpalsmartparkingfinder.models.ParkingSpot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.SpotViewHolder> {
    private Context context;
    private List<ParkingSpot> spotList;
    private FirebaseFirestore db;

    public SpotAdapter(Context context, List<ParkingSpot> spotList) {
        this.context = context;
        this.spotList = spotList;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout for each item
        View view = LayoutInflater.from(context).inflate(R.layout.item_parking_spot, parent, false);
        return new SpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
        ParkingSpot spot = spotList.get(position);

        // Set the parking spot name
        holder.spotName.setText(spot.getName());

        // Set the status based on availability (available or not)
        holder.spotStatus.setText(spot.isAvailable() ? "Available" : "Not Available");

        // Set up delete button
        holder.deleteIcon.setOnClickListener(v -> {
            String spotId = spot.getId(); // Ensure this ID is not null

            if (spotId != null && !spotId.isEmpty()) {
                // Log the ID to confirm it
                Log.d("Delete Parking", "Document ID: " + spotId);

                // Delete the parking spot from Firestore using its document ID
                db.collection("ParkingSpots").document(spotId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Successfully deleted the spot from Firestore, now update the UI
                            spotList.remove(position);
                            notifyItemRemoved(position);  // Update RecyclerView
                            Toast.makeText(context, "Parking spot deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // If Firestore deletion fails
                            Toast.makeText(context, "Failed to delete parking spot", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // If no valid document ID exists
                Toast.makeText(context, "Failed to find parking spot ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return spotList != null ? spotList.size() : 0;  // Ensure that the size is 0 if the list is null
    }

    // ViewHolder class to hold views for each item in the RecyclerView
    public static class SpotViewHolder extends RecyclerView.ViewHolder {
        TextView spotName, spotStatus;
        ImageView deleteIcon;

        public SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            spotName = itemView.findViewById(R.id.spotName);
            spotStatus = itemView.findViewById(R.id.spotStatus);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);  // Initialize delete icon
        }
    }
}
