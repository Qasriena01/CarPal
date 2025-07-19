const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

// Cloud Function to decrease available spots
exports.updateAvailability = functions.firestore
    .document('ParkingLots/{lotId}')
    .onUpdate((change, context) => {
      const data = change.after.data();
      const available = data.available;
      const capacity = data.capacity;

      if (available < 0) {
        return null;  // Avoid negative availability
      }

      // Simple logic: Update availability if someone parks or leaves
      if (data.status === "occupied") {
        // Decrease available spots
        return db.collection('ParkingLots').doc(context.params.lotId).update({
          available: available - 1,
        });
      } else {
        // Increase available spots when someone leaves
        return db.collection('ParkingLots').doc(context.params.lotId).update({
          available: available + 1,
        });
      }
    });
