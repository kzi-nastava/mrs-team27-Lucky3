package com.example.mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;

import com.example.mobile.R;
import com.example.mobile.models.AppNotification;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.NotificationStore;
import com.example.mobile.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.mobile.utils.NavbarHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Notification center panel — shows all in-app notifications
 * with type-based icons, time-ago stamps, unread indicators,
 * and tap-to-navigate functionality.
 * <p>
 * Observes {@link NotificationStore} reactively so the list
 * and badge update in real-time as new notifications arrive.
 */
public class NotificationPanelFragment extends Fragment {

    private ListView listView;
    private View emptyState;
    private TextView tvUnreadCount;
    private NotificationAdapter adapter;
    private List<AppNotification> notifications = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        initViews(root);
        setupNavbar(root);
        setupActions(root);
        setupListView();
        observeNotifications();

        return root;
    }

    private void initViews(View root) {
        listView = root.findViewById(R.id.notification_list_view);
        emptyState = root.findViewById(R.id.empty_state);
        tvUnreadCount = root.findViewById(R.id.tv_unread_count);
    }

    private void setupNavbar(View root) {
        NavbarHelper.setup(this, root, "Notifications", false, true);
    }

    private void setupActions(View root) {
        root.findViewById(R.id.btn_mark_all_read).setOnClickListener(v ->
                NotificationStore.getInstance().markAllAsRead());

        root.findViewById(R.id.btn_clear_all).setOnClickListener(v -> {
            // Clear locally
            NotificationStore.getInstance().clearAll();

            // Delete from backend so they don't reappear
            String token = new SharedPreferencesManager(requireContext()).getToken();
            if (token != null) {
                ClientUtils.notificationService.deleteAll("Bearer " + token)
                        .enqueue(new Callback<Map<String, Integer>>() {
                            @Override
                            public void onResponse(Call<Map<String, Integer>> call,
                                                   Response<Map<String, Integer>> response) {
                                // Deleted on backend — nothing else to do
                            }

                            @Override
                            public void onFailure(Call<Map<String, Integer>> call, Throwable t) {
                                // Best-effort: local clear already happened
                            }
                        });
            }
        });
    }

    private void setupListView() {
        adapter = new NotificationAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= notifications.size()) return;
            AppNotification notif = notifications.get(position);

            // Remove the notification locally and from backend
            NotificationStore.getInstance().removeNotification(notif.getId());
            deleteFromBackend(notif.getBackendId());

            // Navigate based on type
            navigateToNotification(notif);
        });
    }

    private void observeNotifications() {
        NotificationStore.getInstance().getNotifications().observe(getViewLifecycleOwner(), list -> {
            notifications = list != null ? list : new ArrayList<>();
            adapter.notifyDataSetChanged();

            boolean empty = notifications.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        NotificationStore.getInstance().getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                tvUnreadCount.setText(count + " unread");
                tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Builds NavOptions that pop the back stack up to (but not including) the role's
     * home destination, then navigate to the target.  This mimics pressing a sidebar item:
     * the notification panel and any intermediate fragments are removed so the target
     * appears as a direct child of the home screen, not stacked on top of the panel.
     */
    private NavOptions buildNotifNavOptions(androidx.navigation.NavController navController) {
        String role = new SharedPreferencesManager(requireContext()).getUserRole();
        int roleHome;
        if ("DRIVER".equals(role))        roleHome = R.id.nav_driver_dashboard;
        else if ("ADMIN".equals(role))    roleHome = R.id.nav_admin_dashboard;
        else                              roleHome = R.id.nav_passenger_home;

        return new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(roleHome, false)
                .build();
    }

    private void navigateToNotification(AppNotification notif) {
        try {
            String role = new SharedPreferencesManager(requireContext()).getUserRole();
            androidx.navigation.NavController navController =
                    Navigation.findNavController(requireView());
            NavOptions navOptions = buildNotifNavOptions(navController);

            switch (notif.getType()) {
                case RIDE_STATUS:
                case RIDE_INVITE:
                case RIDE_CREATED:
                case DRIVER_ASSIGNED:
                    if (notif.getRideId() != null) {
                        Bundle args = new Bundle();
                        args.putLong("rideId", notif.getRideId());
                        navController.navigate(R.id.nav_active_ride, args, navOptions);
                    }
                    break;

                case STOP_COMPLETED:
                    if (notif.getRideId() != null) {
                        int currentDestId = navController.getCurrentDestination() != null
                                ? navController.getCurrentDestination().getId() : -1;
                        if (currentDestId != R.id.nav_active_ride) {
                            Bundle stopArgs = new Bundle();
                            stopArgs.putLong("rideId", notif.getRideId());
                            navController.navigate(R.id.nav_active_ride, stopArgs, navOptions);
                        }
                        // If already on active ride the notification was dismissed — nothing else to do
                    }
                    break;

                case RIDE_FINISHED:
                case RIDE_CANCELLED:
                    if (notif.getRideId() != null) {
                        Bundle histArgs = new Bundle();
                        histArgs.putLong("rideId", notif.getRideId());
                        if ("DRIVER".equals(role)) {
                            navController.navigate(R.id.nav_ride_details, histArgs, navOptions);
                        } else {
                            navController.navigate(R.id.nav_passenger_ride_detail, histArgs, navOptions);
                        }
                    }
                    break;

                case LEAVE_REVIEW:
                    if (notif.getRideId() != null) {
                        Bundle reviewArgs = new Bundle();
                        reviewArgs.putLong("rideId", notif.getRideId());
                        navController.navigate(R.id.nav_review, reviewArgs, navOptions);
                    }
                    break;

                case PANIC_ALERT:
                    navController.navigate(R.id.nav_admin_panic, null, navOptions);
                    break;

                case SUPPORT_MESSAGE:
                    navigateToSupport(notif.getChatId());
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            // Navigation might fail if destination not in current graph
        }
    }

    private void navigateToSupport() {
        navigateToSupport(null);
    }

    private void navigateToSupport(Long chatId) {
        try {
            androidx.navigation.NavController navController =
                    Navigation.findNavController(requireView());
            NavOptions navOptions = buildNotifNavOptions(navController);

            String role = new SharedPreferencesManager(requireContext()).getUserRole();
            if ("ADMIN".equals(role)) {
                if (chatId != null && chatId > 0) {
                    Bundle args = new Bundle();
                    args.putLong("chatId", chatId);
                    navController.navigate(R.id.nav_admin_support_chat, args, navOptions);
                    return;
                }
                navController.navigate(R.id.nav_admin_support, null, navOptions);
            } else if ("DRIVER".equals(role)) {
                navController.navigate(R.id.nav_driver_support, null, navOptions);
            } else {
                navController.navigate(R.id.nav_passenger_support, null, navOptions);
            }
        } catch (Exception e) {
            // Ignore navigation errors
        }
    }

    // ======================== Adapter ========================

    private class NotificationAdapter extends BaseAdapter {

        @Override
        public int getCount() { return notifications.size(); }

        @Override
        public AppNotification getItem(int position) { return notifications.get(position); }

        @Override
        public long getItemId(int position) { return notifications.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_notification, parent, false);
                holder = new ViewHolder();
                holder.card = convertView.findViewById(R.id.card_notification);
                holder.iconBackground = convertView.findViewById(R.id.icon_background);
                holder.tvTitle = convertView.findViewById(R.id.tv_title);
                holder.tvBody = convertView.findViewById(R.id.tv_body);
                holder.tvTime = convertView.findViewById(R.id.tv_time);
                holder.tvTypeBadge = convertView.findViewById(R.id.tv_type_badge);
                holder.unreadDot = convertView.findViewById(R.id.unread_dot);
                holder.ivTypeIcon = convertView.findViewById(R.id.iv_type_icon);
                holder.btnDismiss = convertView.findViewById(R.id.btn_dismiss);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppNotification notif = getItem(position);

            holder.tvTitle.setText(notif.getTitle());
            holder.tvBody.setText(notif.getBody());
            holder.tvTime.setText(formatTimeAgo(notif.getTimestamp()));

            // Unread indicator
            boolean isPanic = notif.getType() == AppNotification.Type.PANIC_ALERT;
            holder.unreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
            // Panic unread dot is red instead of green
            holder.unreadDot.setBackgroundResource(
                    isPanic ? R.drawable.bg_dot_red : R.drawable.bg_dot_green);
            holder.tvTitle.setTextColor(ContextCompat.getColor(requireContext(),
                    notif.isRead() ? R.color.gray_400 : R.color.white));

            // Type-based styling
            styleForType(holder, notif.getType());

            // Dismiss button
            holder.btnDismiss.setOnClickListener(v -> {
                    NotificationStore.getInstance().removeNotification(notif.getId());
                    deleteFromBackend(notif.getBackendId());
            });

            return convertView;
        }

        private void styleForType(ViewHolder holder, AppNotification.Type type) {
            if (type == null) type = AppNotification.Type.GENERAL;
            switch (type) {
                case RIDE_STATUS:
                    holder.tvTypeBadge.setText("RIDE");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_menu_directions);
                    break;
                case RIDE_INVITE:
                    holder.tvTypeBadge.setText("INVITE");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_active);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_menu_send);
                    break;
                case PANIC_ALERT:
                    holder.tvTypeBadge.setText("🚨 PANIC");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    holder.ivTypeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_500));
                    holder.iconBackground.setBackgroundResource(R.drawable.bg_icon_box_red);
                    holder.card.setCardBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.red_500_10));
                    holder.card.setStrokeColor(
                            ContextCompat.getColor(requireContext(), R.color.red_500_20));
                    holder.card.setStrokeWidth(2);
                    break;
                case SUPPORT_MESSAGE:
                    holder.tvTypeBadge.setText("SUPPORT");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_email);
                    break;
                case STOP_COMPLETED:
                    holder.tvTypeBadge.setText("STOP");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
                    break;
                default:
                    holder.tvTypeBadge.setText("INFO");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_gray);
                    holder.ivTypeIcon.setImageResource(R.drawable.ic_notification_bell);
                    break;
            }

            // Reset card styling for non-panic types (ViewHolder recycling)
            if (type != AppNotification.Type.PANIC_ALERT) {
                holder.card.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.gray_900));
                holder.card.setStrokeWidth(0);
                holder.ivTypeIcon.clearColorFilter();
                holder.iconBackground.setBackgroundResource(R.drawable.bg_icon_box_circle);
            }
        }

        class ViewHolder {
            MaterialCardView card;
            View iconBackground;
            TextView tvTitle, tvBody, tvTime, tvTypeBadge;
            View unreadDot;
            ImageView ivTypeIcon, btnDismiss;
        }
    }

    // ======================== Backend Delete ========================

    /**
     * Fire-and-forget deletion of a single notification from the backend
     * so it doesn't reappear when the history is fetched again.
     */
    private void deleteFromBackend(Long backendId) {
        if (backendId == null) return;
        String token = new SharedPreferencesManager(requireContext()).getToken();
        if (token == null) return;
        ClientUtils.notificationService.deleteNotification("Bearer " + token, backendId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        // Deleted on backend
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        // Best-effort: local removal already happened
                    }
                });
    }

    // ======================== Helpers ========================

    private String formatTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) return "now";

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) return "just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return days / 7 + "w ago";
    }
}
