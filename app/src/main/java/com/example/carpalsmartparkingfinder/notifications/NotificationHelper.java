package com.example.carpalsmartparkingfinder.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.carpalsmartparkingfinder.R;

public class NotificationHelper {

    public static final String CHANNEL_ID = "parking_notification_channel";

    // Create notification channel for Android O and above
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Parking Notifications";
            String description = "Notifications about available parking spots";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Method to send a local notification
    public static void sendLocalNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // Create the notification
            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_notification)  // Your app's notification icon
                    .setAutoCancel(true)  // Dismiss notification after clicking
                    .build();

            notificationManager.notify(1, notification);  // Send notification
        }
    }
}
