package com.example.mobile.utils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

/**
 * Simple utility to check whether the app is currently in the foreground.
 * <p>
 * Uses {@link ProcessLifecycleOwner} which tracks the overall process lifecycle:
 * <ul>
 *   <li>ON_START / ON_RESUME → at least one Activity is visible → foreground</li>
 *   <li>ON_STOP → no Activity visible → background</li>
 * </ul>
 * <p>
 * No initialisation is needed — {@code ProcessLifecycleOwner} is automatically
 * initialised via the {@code androidx.startup} content provider.
 */
public final class AppLifecycleTracker {

    private AppLifecycleTracker() {}

    /**
     * Returns {@code true} when at least one Activity is in the STARTED state
     * (i.e. the app is visible to the user).
     */
    public static boolean isAppInForeground() {
        try {
            Lifecycle.State state = ProcessLifecycleOwner.get()
                    .getLifecycle()
                    .getCurrentState();
            return state.isAtLeast(Lifecycle.State.STARTED);
        } catch (Exception e) {
            // Fallback: if we can't determine, assume foreground (suppress system notifs)
            return true;
        }
    }
}
