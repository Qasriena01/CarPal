package com.example.carpalsmartparkingfinder.models;

public class ParkingSpot {

    private String name;
    private double latitude;
    private double longitude;
    private boolean available;
    private String id; // Firestore document ID

    // No-argument constructor (required by Firebase Firestore)
    public ParkingSpot() {
    }

    // Constructor with parameters
    public ParkingSpot(String name, double latitude, double longitude, boolean available) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
    }

    // Getter and setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    // Getter and setter for Firestore document ID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", available=" + available +
                ", id='" + id + '\'' +
                '}';
    }
}
