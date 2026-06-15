package com.example.mobile.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.mobile.models.AppNotification;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages local reminder notifications for scheduled rides.
 * Periodically checks for rides with statuses SCHEDULED, ACCEPTED, or PENDING
 * and triggers reminders at 14, 9, and 4 minutes before start time.
 */
public class ScheduledRideReminderManager {
    private static final String TAG = "ScheduledRideReminder";
    private static final long CHECK_INTERVAL = 60_000; // 1 minute

    private static ScheduledRideReminderManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkRunnable;
    private Long passengerId;
    private String token;

    // Track which reminders have been sent to avoid duplicates.
    // Map: rideId -> last sent threshold (e.g. 14, 9, or 4)
    private final Map<Long, Integer> lastSentReminders = new HashMap<>();

    private ScheduledRideReminderManager() {}

    public static synchronized ScheduledRideReminderManager getInstance() {
        if (instance == null) {
            instance = new ScheduledRideReminderManager();
        }
        return instance;
    }

    /**
     * Start the reminder manager.
     */
    public void start(Context context, Long passengerId, String token) {
        if (passengerId == null || token == null) return;
        this.passengerId = passengerId;
        this.token = "Bearer " + token;

        stop();
        checkRunnable = this::fetchRides;
        handler.post(checkRunnable);
        Log.i(TAG, "Started for passenger " + passengerId);
    }

    /**
     * Stop the reminder manager and clear tracking state.
     */
    public void stop() {
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
            checkRunnable = null;
        }
        lastSentReminders.clear();
        Log.i(TAG, "Stopped");
    }

    private void fetchRides() {
        if (passengerId == null || token == null) return;

        // Fetch rides with different statuses that could be considered "scheduled"
        // We check SCHEDULED, ACCEPTED, and PENDING sequentially or combined if API allowed.
        // For simplicity and consistency with MainActivity, we'll check them individually.
        fetchStatusRides("SCHEDULED", () -> 
            fetchStatusRides("ACCEPTED", () -> 
                fetchStatusRides("PENDING", this::scheduleNext)
            )
        );
    }

    private void fetchStatusRides(String status, Runnable onComplete) {
        ClientUtils.rideService.getActiveRides(null, passengerId, status, 0, 20, token)
                .enqueue(new Callback<PageResponse<RideResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            processRides(response.body().getContent());
                        }
                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                        Log.e(TAG, "Failed to fetch " + status + " rides", t);
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    private void scheduleNext() {
        if (checkRunnable != null) {
            handler.postDelayed(checkRunnable, CHECK_INTERVAL);
        }
    }

    private void processRides(List<RideResponse> rides) {
        if (rides == null || rides.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (RideResponse ride : rides) {
            if (ride.getScheduledTime() == null) continue;

            try {
                LocalDateTime scheduledTime = LocalDateTime.parse(ride.getScheduledTime(),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                long minutesUntilStart = Duration.between(now, scheduledTime).toMinutes();
                
                checkAndTriggerReminder(ride, (int) minutesUntilStart);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing scheduled time for ride " + ride.getId(), e);
            }
        }
    }

    private void checkAndTriggerReminder(RideResponse ride, int minutesUntilStart) {
        Integer lastThreshold = lastSentReminders.get(ride.getId());

        // Reminder thresholds: 14, 9, 4, -1, -6, -11...
        // We look for the most urgent threshold that hasn't been sent yet.
        int threshold = 14;
        while (threshold > -60) { // Limit reminders for very late rides (e.g., 1 hour)
            if (minutesUntilStart <= threshold) {
                // If we haven't sent any reminder or this threshold is newer (smaller) than last sent
                if (lastThreshold == null || threshold < lastThreshold) {
                    
                    // Check if there's a smaller threshold that also fits
                    // We want to send the "best" one if we just woke up or logged in
                    if (minutesUntilStart <= (threshold - 5)) {
                        threshold -= 5;
                        continue;
                    }
                    
                    sendReminder(ride, threshold);
                    lastSentReminders.put(ride.getId(), threshold);
                    break;
                }
            } else {
                // Threshold not reached yet
                break;
            }
            threshold -= 5;
        }
    }

    private void sendReminder(RideResponse ride, int threshold) {
        String title = "Upcoming Ride Reminder";
        String body;
        
        if (threshold > 0) {
            body = "Your ride is scheduled to start in " + threshold + " minutes.";
        } else if (threshold == -1) {
            body = "Your ride is starting soon.";
        } else if (threshold < 0) {
            body = "Your ride is " + Math.abs(threshold + 1) + " minutes late.";
        } else {
            body = "Your ride is scheduled to start now.";
        }
        
        Log.i(TAG, "Sending " + threshold + " min reminder for ride " + ride.getId());

        // 1. Add to in-app NotificationStore
        AppNotification notification = new AppNotification(
                AppNotification.Type.RIDE_STATUS, title, body);
        notification.setRideId(ride.getId());
        NotificationStore.getInstance().addNotification(notification);

        // 2. Post Android system notification via AppNotificationManager
        AppNotificationManager.getInstance().postSystemNotification(
                title, body, NotificationHelper.CHANNEL_RIDE_UPDATES,
                "active_ride", ride.getId());
    }
}
