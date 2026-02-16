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
import androidx.navigation.Navigation;

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

            // Remove the notification (delete it, not just mark as read)
            NotificationStore.getInstance().removeNotification(notif.getId());

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

    private void navigateToNotification(AppNotification notif) {
        try {
            String role = new com.example.mobile.utils.SharedPreferencesManager(
                    requireContext()).getUserRole();

            switch (notif.getType()) {
                case RIDE_STATUS:
                case RIDE_INVITE:
                case RIDE_CREATED:
                case DRIVER_ASSIGNED:
                    if (notif.getRideId() != null) {
                        Bundle args = new Bundle();
                        args.putLong("rideId", notif.getRideId());
                        Navigation.findNavController(requireView())
                                .navigate(R.id.nav_active_ride, args);
                    }
                    break;

                case RIDE_FINISHED:
                case RIDE_CANCELLED:
                    if (notif.getRideId() != null) {
                        Bundle histArgs = new Bundle();
                        histArgs.putLong("rideId", notif.getRideId());
                        // Navigate to role-specific ride detail
                        if ("DRIVER".equals(role)) {
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.nav_ride_details, histArgs);
                        } else {
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.nav_passenger_ride_detail, histArgs);
                        }
                    }
                    break;

                case PANIC_ALERT:
                    Navigation.findNavController(requireView())
                            .navigate(R.id.nav_admin_panic);
                    break;

                case SUPPORT_MESSAGE:
                    // Navigate to support chat — use role-specific destination
                    // Pass chatId for admin direct navigation to the specific chat
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
            String role = new com.example.mobile.utils.SharedPreferencesManager(
                    requireContext()).getUserRole();
            int destId;
            if ("ADMIN".equals(role)) {
                if (chatId != null && chatId > 0) {
                    // Navigate directly to the admin chat with this chatId
                    Bundle args = new Bundle();
                    args.putLong("chatId", chatId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.nav_admin_support_chat, args);
                    return;
                }
                destId = R.id.nav_admin_support;
            } else if ("DRIVER".equals(role)) {
                destId = R.id.nav_driver_support;
            } else {
                destId = R.id.nav_passenger_support;
            }
            Navigation.findNavController(requireView()).navigate(destId);
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
            holder.unreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
            holder.tvTitle.setTextColor(ContextCompat.getColor(requireContext(),
                    notif.isRead() ? R.color.gray_400 : R.color.white));

            // Type-based styling
            styleForType(holder, notif.getType());

            // Dismiss button
            holder.btnDismiss.setOnClickListener(v ->
                    NotificationStore.getInstance().removeNotification(notif.getId()));

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
                    holder.tvTypeBadge.setText("PANIC");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    break;
                case SUPPORT_MESSAGE:
                    holder.tvTypeBadge.setText("SUPPORT");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    holder.ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_email);
                    break;
                default:
                    holder.tvTypeBadge.setText("INFO");
                    holder.tvTypeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
                    holder.tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_gray);
                    holder.ivTypeIcon.setImageResource(R.drawable.ic_notification_bell);
                    break;
            }
        }

        class ViewHolder {
            TextView tvTitle, tvBody, tvTime, tvTypeBadge;
            View unreadDot;
            ImageView ivTypeIcon, btnDismiss;
        }
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
