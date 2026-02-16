package com.example.mobile.ui.admin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.PanicResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.mobile.utils.NavbarHelper;

/**
 * Admin page for viewing and receiving PANIC alerts in real-time.
 * Polls the backend every 10 seconds for new panic alerts,
 * plays an audible alert sound, and shows Android notifications.
 */
public class AdminPanicFragment extends Fragment {

    private static final String TAG = "AdminPanic";
    private static final long POLL_INTERVAL = 10_000; // 10 seconds
    private static final String NOTIFICATION_CHANNEL_ID = "panic_alerts";
    private static final int NOTIFICATION_BASE_ID = 9000;

    private SharedPreferencesManager preferencesManager;

    // Views
    private LinearLayout panicListContainer;
    private LinearLayout emptyState;
    private LinearLayout toastBanner;
    private TextView tvToastMessage;
    private TextView tvTotalAlerts;
    private ProgressBar progressBar;

    // State
    private final List<PanicResponse> panics = new ArrayList<>();
    private final Set<Long> knownPanicIds = new HashSet<>();
    private final Set<Long> newPanicIds = new HashSet<>();
    private boolean isFirstLoad = true;

    // Polling
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private boolean isPolling = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_panic, container, false);
        preferencesManager = new SharedPreferencesManager(requireContext());

        bindViews(root);
        setupNavbar(root);
        setupRefreshButton(root);
        createNotificationChannel();

        loadPanics();
        startPolling();

        return root;
    }

    private void bindViews(View root) {
        panicListContainer = root.findViewById(R.id.panic_list_container);
        emptyState = root.findViewById(R.id.empty_state);
        toastBanner = root.findViewById(R.id.panic_toast_banner);
        tvToastMessage = root.findViewById(R.id.tv_toast_message);
        tvTotalAlerts = root.findViewById(R.id.tv_total_alerts);
        progressBar = root.findViewById(R.id.progress_bar);

        // Dismiss toast
        root.findViewById(R.id.btn_dismiss_toast).setOnClickListener(v ->
                toastBanner.setVisibility(View.GONE));
    }

    private void setupNavbar(View root) {
        NavbarHelper.setup(this, root, "Panic Alerts");
    }

    private void setupRefreshButton(View root) {
        root.findViewById(R.id.btn_refresh).setOnClickListener(v -> loadPanics());
    }

    // ======================== Notification Channel ========================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Panic Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Emergency panic alert notifications from rides");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            channel.enableLights(true);
            channel.setLightColor(Color.RED);

            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ======================== REST Loading ========================

    private void loadPanics() {
        if (isFirstLoad) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.panicService.getPanics(0, 50, token).enqueue(new Callback<PageResponse<PanicResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<PanicResponse>> call,
                                   Response<PageResponse<PanicResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().getContent() != null) {
                    List<PanicResponse> loaded = response.body().getContent();
                    processPanics(loaded);
                } else {
                    Log.e(TAG, "Failed to load panics: " + response.code());
                }
                isFirstLoad = false;
            }

            @Override
            public void onFailure(Call<PageResponse<PanicResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error loading panics", t);
                isFirstLoad = false;
            }
        });
    }

    private void processPanics(List<PanicResponse> loaded) {
        List<PanicResponse> newAlerts = new ArrayList<>();

        for (PanicResponse p : loaded) {
            if (p.getId() != null && !knownPanicIds.contains(p.getId())) {
                if (!isFirstLoad) {
                    newAlerts.add(p);
                    newPanicIds.add(p.getId());
                }
                knownPanicIds.add(p.getId());
            }
        }

        // Replace the full list
        panics.clear();
        panics.addAll(loaded);

        // Update UI
        tvTotalAlerts.setText("Total alerts: " + panics.size());
        renderPanicList();

        // Handle new alerts (not on first load)
        if (!newAlerts.isEmpty()) {
            for (PanicResponse alert : newAlerts) {
                showToastNotification(alert);
                sendAndroidNotification(alert);
            }
            playAlertSound();

            // Remove flash highlight after 5 seconds
            pollingHandler.postDelayed(() -> {
                newPanicIds.clear();
                renderPanicList();
            }, 5000);
        }
    }

    // ======================== UI Rendering ========================

    private void renderPanicList() {
        panicListContainer.removeAllViews();

        if (panics.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            return;
        }
        emptyState.setVisibility(View.GONE);

        for (PanicResponse panic : panics) {
            View card = buildPanicCard(panic);
            panicListContainer.addView(card);
        }
    }

    private View buildPanicCard(PanicResponse panic) {
        boolean isNew = panic.getId() != null && newPanicIds.contains(panic.getId());

        // Card container
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(12);
        card.setLayoutParams(cardParams);

        // Red left border for new alerts
        if (isNew) {
            card.setBackgroundColor(Color.parseColor("#1AEF4444"));
        }

        // ---- Header row: Panic # + time since ----
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvId = new TextView(requireContext());
        tvId.setText("🚨 Panic #" + (panic.getId() != null ? panic.getId() : "?"));
        tvId.setTextColor(Color.parseColor("#EF4444"));
        tvId.setTextSize(16);
        tvId.setTypeface(null, android.graphics.Typeface.BOLD);
        tvId.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        headerRow.addView(tvId);

        if (isNew) {
            TextView newBadge = new TextView(requireContext());
            newBadge.setText("NEW");
            newBadge.setTextColor(Color.WHITE);
            newBadge.setTextSize(11);
            newBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            newBadge.setBackgroundColor(Color.parseColor("#EF4444"));
            newBadge.setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2));
            headerRow.addView(newBadge);
        }

        TextView tvTimeSince = new TextView(requireContext());
        tvTimeSince.setText(getTimeSince(panic.getTime()));
        tvTimeSince.setTextColor(getResources().getColor(R.color.gray_400, null));
        tvTimeSince.setTextSize(12);
        tvTimeSince.setPadding(dpToPx(8), 0, 0, 0);
        headerRow.addView(tvTimeSince);

        card.addView(headerRow);

        // ---- Triggered by ----
        if (panic.getUser() != null) {
            addSpacer(card, 12);
            addSectionLabel(card, "Triggered by:");

            PanicResponse.PanicUserResponse user = panic.getUser();
            addInfoRow(card, "👤", user.getFullName());
            if (user.getEmail() != null) {
                addInfoRow(card, "✉", user.getEmail());
            }
            if (user.getRole() != null) {
                addInfoRow(card, "🏷", user.getRole());
            }
            if (user.getPhoneNumber() != null) {
                addPhoneRow(card, "📞", user.getPhoneNumber());
            }
        }

        // ---- Reason ----
        if (panic.getReason() != null && !panic.getReason().trim().isEmpty()) {
            addSpacer(card, 12);
            addSectionLabel(card, "Reason:");

            TextView tvReason = new TextView(requireContext());
            tvReason.setText(panic.getReason());
            tvReason.setTextColor(Color.WHITE);
            tvReason.setTextSize(14);
            tvReason.setPadding(0, dpToPx(4), 0, 0);
            card.addView(tvReason);
        }

        // ---- Ride details ----
        if (panic.getRide() != null) {
            PanicResponse.PanicRideResponse ride = panic.getRide();
            addSpacer(card, 12);
            addSectionLabel(card, "Ride Details:");

            addInfoRow(card, "🚗", "Ride #" + (ride.getId() != null ? ride.getId() : "?"));
            if (ride.getStartLocation() != null && ride.getStartLocation().getAddress() != null) {
                addInfoRow(card, "📍", "From: " + ride.getStartLocation().getAddress());
            }
            if (ride.getEndLocation() != null && ride.getEndLocation().getAddress() != null) {
                addInfoRow(card, "🏁", "To: " + ride.getEndLocation().getAddress());
            }
            if (ride.getDriver() != null) {
                addInfoRow(card, "🚘", "Driver: " + ride.getDriver().getFullName());
                if (ride.getDriver().getPhoneNumber() != null) {
                    addPhoneRow(card, "📞", "Call Driver: " + ride.getDriver().getPhoneNumber());
                }
            }
        }

        return card;
    }

    private void addSectionLabel(LinearLayout parent, String text) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextColor(getResources().getColor(R.color.gray_400, null));
        label.setTextSize(12);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(label);
    }

    private void addInfoRow(LinearLayout parent, String icon, String text) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(3), 0, dpToPx(3));

        TextView iconView = new TextView(requireContext());
        iconView.setText(icon);
        iconView.setTextSize(14);
        iconView.setPadding(0, 0, dpToPx(8), 0);
        row.addView(iconView);

        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14);
        row.addView(textView);

        parent.addView(row);
    }

    private void addPhoneRow(LinearLayout parent, String icon, String text) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(3), 0, dpToPx(3));

        TextView iconView = new TextView(requireContext());
        iconView.setText(icon);
        iconView.setTextSize(14);
        iconView.setPadding(0, 0, dpToPx(8), 0);
        row.addView(iconView);

        // Extract phone number from text
        String phone = text;
        if (text.contains(":")) {
            phone = text.substring(text.lastIndexOf(":") + 1).trim();
        }

        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextColor(Color.parseColor("#60A5FA")); // Blue for clickable
        textView.setTextSize(14);

        final String phoneNumber = phone;
        textView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open dialer", e);
            }
        });

        row.addView(textView);
        parent.addView(row);
    }

    private void addSpacer(LinearLayout parent, int dpHeight) {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dpHeight)));
        parent.addView(spacer);
    }

    private String getTimeSince(String timeStr) {
        if (timeStr == null) return "";
        try {
            LocalDateTime time = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Duration duration = Duration.between(time, LocalDateTime.now());
            long seconds = duration.getSeconds();
            if (seconds < 60) return seconds + "s ago";
            long minutes = seconds / 60;
            if (minutes < 60) return minutes + "m ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h ago";
            long days = hours / 24;
            return days + "d ago";
        } catch (Exception e) {
            return timeStr;
        }
    }

    // ======================== Notifications ========================

    private void showToastNotification(PanicResponse alert) {
        String userName = alert.getUser() != null ? alert.getUser().getFullName() : "Unknown";
        Long rideId = alert.getRide() != null ? alert.getRide().getId() : null;
        String message = "🚨 PANIC ALERT — " + userName + " on ride #" +
                (rideId != null ? rideId : "?");
        tvToastMessage.setText(message);
        toastBanner.setVisibility(View.VISIBLE);

        // Auto-dismiss after 8 seconds
        pollingHandler.postDelayed(() -> {
            if (isAdded() && toastBanner != null) {
                toastBanner.setVisibility(View.GONE);
            }
        }, 8000);
    }

    private void sendAndroidNotification(PanicResponse alert) {
        try {
            String userName = alert.getUser() != null ? alert.getUser().getFullName() : "Unknown";
            Long rideId = alert.getRide() != null ? alert.getRide().getId() : null;
            String title = "🚨 PANIC ALERT";
            String body = userName + " triggered panic on ride #" +
                    (rideId != null ? rideId : "?");
            if (alert.getReason() != null && !alert.getReason().trim().isEmpty()) {
                body += "\nReason: " + alert.getReason();
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    requireContext(), NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                    .setLights(Color.RED, 1000, 300);

            int notificationId = NOTIFICATION_BASE_ID +
                    (alert.getId() != null ? alert.getId().intValue() : 0);

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(requireContext());
            try {
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                Log.w(TAG, "Notification permission not granted", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification", e);
        }
    }

    /**
     * Plays a beep-beep-beep alert sound using ToneGenerator.
     * Similar to the web's AudioContext beep pattern.
     */
    private void playAlertSound() {
        new Thread(() -> {
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                // Three beeps: 150ms on, 100ms off
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(250);
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(250);
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150);
                Thread.sleep(200);
                toneGen.release();
            } catch (Exception e) {
                Log.e(TAG, "Failed to play alert sound", e);
            }
        }).start();
    }

    // ======================== Polling ========================

    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        pollingHandler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    private void stopPolling() {
        isPolling = false;
        pollingHandler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling || !isAdded()) return;
            loadPanics();
            pollingHandler.postDelayed(this, POLL_INTERVAL);
        }
    };

    // ======================== Lifecycle ========================

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
