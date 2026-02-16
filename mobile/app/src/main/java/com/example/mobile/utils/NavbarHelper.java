package com.example.mobile.utils;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import com.example.mobile.MainActivity;
import com.example.mobile.R;

/**
 * Shared helper that wires the custom navbar's notification bell button
 * and badge in any fragment that includes {@code view_custom_navbar.xml}.
 * <p>
 * Call {@link #setup(Fragment, View)} from {@code onCreateView()} after inflating
 * the root view. It sets the menu button, bell click listener, and badge observer.
 */
public final class NavbarHelper {

    private NavbarHelper() {}

    /**
     * Wire the navbar's menu button, notification bell, and badge.
     *
     * @param fragment The host fragment (used for lifecycle-aware badge observation)
     * @param root     The inflated root view that contains the included navbar
     */
    public static void setup(Fragment fragment, View root) {
        setup(fragment, root, null);
    }

    /**
     * Wire the navbar's menu button, notification bell, badge, and optional title.
     *
     * @param fragment The host fragment
     * @param root     The inflated root view
     * @param title    Optional toolbar title; if null, the title is left unchanged
     */
    public static void setup(Fragment fragment, View root, String title) {
        setup(fragment, root, title, true, false);
    }

    /**
     * Wire the navbar's menu button, notification bell, badge, and optional title.
     *
     * @param fragment  The host fragment
     * @param root      The inflated root view
     * @param title     Optional toolbar title; if null, the title is left unchanged
     * @param showBell  Whether to show the notification bell (false hides it)
     */
    public static void setup(Fragment fragment, View root, String title, boolean showBell) {
        setup(fragment, root, title, showBell, false);
    }

    /**
     * Full overload for navbar setup.
     *
     * @param fragment       The host fragment
     * @param root           The inflated root view
     * @param title          Optional toolbar title; if null, the title is left unchanged
     * @param showBell       Whether to show the notification bell (false hides it)
     * @param showBackButton If true, replaces the hamburger with a back arrow that pops the back stack
     */
    public static void setup(Fragment fragment, View root, String title,
                             boolean showBell, boolean showBackButton) {
        View navbar = root.findViewById(R.id.navbar);
        if (navbar == null) return;

        // Menu / back button
        ImageView btnMenu = navbar.findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            if (showBackButton) {
                btnMenu.setImageResource(R.drawable.ic_arrow_back);
                btnMenu.setOnClickListener(v ->
                        Navigation.findNavController(v).popBackStack());
            } else {
                btnMenu.setOnClickListener(v ->
                        ((MainActivity) fragment.requireActivity()).openDrawer());
            }
        }

        // Title
        if (title != null) {
            TextView tvTitle = navbar.findViewById(R.id.toolbar_title);
            if (tvTitle != null) tvTitle.setText(title);
        }

        // Notification bell → navigate to notification panel
        View btnNotifications = navbar.findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            if (!showBell) {
                btnNotifications.setVisibility(View.GONE);
            } else {
                btnNotifications.setOnClickListener(v -> {
                    try {
                        Navigation.findNavController(v).navigate(R.id.nav_notifications);
                    } catch (Exception e) {
                        // Already on notifications or navigation error — ignore
                    }
                });
            }
        }

        // Badge — observe unread count with the fragment's lifecycle
        TextView tvBadge = navbar.findViewById(R.id.tv_notification_badge);
        if (tvBadge != null) {
            LifecycleOwner owner = fragment.getViewLifecycleOwner();
            NotificationStore.getInstance().getUnreadCount().observe(owner, count -> {
                if (count != null && count > 0) {
                    tvBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            });
        }
    }
}
