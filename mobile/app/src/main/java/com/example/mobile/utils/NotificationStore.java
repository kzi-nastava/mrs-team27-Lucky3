package com.example.mobile.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.AppNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton in-memory store for in-app notifications.
 * <p>
 * Exposes {@link LiveData} so any Fragment or Activity can observe
 * the notification list and unread count reactively — the navbar
 * badge updates automatically whenever a new notification arrives
 * or is marked as read.
 * <p>
 * Thread-safe: all mutations happen on the main thread via LiveData.
 * <p>
 * Usage:
 * <pre>
 *   NotificationStore.getInstance().getUnreadCount().observe(this, count -&gt; badge.setText(String.valueOf(count)));
 *   NotificationStore.getInstance().addNotification(notification);
 * </pre>
 */
public class NotificationStore {

    private static final int MAX_NOTIFICATIONS = 100;

    private static volatile NotificationStore instance;

    private final MutableLiveData<List<AppNotification>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final AtomicLong idGenerator = new AtomicLong(1);

    private NotificationStore() {}

    public static NotificationStore getInstance() {
        if (instance == null) {
            synchronized (NotificationStore.class) {
                if (instance == null) {
                    instance = new NotificationStore();
                }
            }
        }
        return instance;
    }

    // ======================== Public API ========================

    /**
     * Observable list of all notifications (newest first).
     */
    public LiveData<List<AppNotification>> getNotifications() {
        return notifications;
    }

    /**
     * Observable count of unread notifications (for badge).
     */
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    /**
     * Add a new notification. Assigns an auto-incremented ID.
     * Trims oldest notifications if exceeding {@link #MAX_NOTIFICATIONS}.
     */
    public void addNotification(AppNotification notification) {
        notification.setId(idGenerator.getAndIncrement());
        List<AppNotification> current = new ArrayList<>(getList());
        current.add(0, notification); // newest first
        if (current.size() > MAX_NOTIFICATIONS) {
            current = new ArrayList<>(current.subList(0, MAX_NOTIFICATIONS));
        }
        notifications.postValue(current);
        recalculateUnreadCount(current);
    }

    /**
     * Mark a single notification as read.
     */
    public void markAsRead(long notificationId) {
        List<AppNotification> current = new ArrayList<>(getList());
        for (AppNotification n : current) {
            if (n.getId() == notificationId) {
                n.setRead(true);
                break;
            }
        }
        notifications.postValue(current);
        recalculateUnreadCount(current);
    }

    /**
     * Mark all notifications as read.
     */
    public void markAllAsRead() {
        List<AppNotification> current = new ArrayList<>(getList());
        for (AppNotification n : current) {
            n.setRead(true);
        }
        notifications.postValue(current);
        unreadCount.postValue(0);
    }

    /**
     * Remove a single notification.
     */
    public void removeNotification(long notificationId) {
        List<AppNotification> current = new ArrayList<>(getList());
        current.removeIf(n -> n.getId() == notificationId);
        notifications.postValue(current);
        recalculateUnreadCount(current);
    }

    /**
     * Clear all notifications.
     */
    public void clearAll() {
        notifications.postValue(new ArrayList<>());
        unreadCount.postValue(0);
    }

    /**
     * Get the current unread count synchronously (for one-off reads).
     */
    public int getUnreadCountSync() {
        Integer count = unreadCount.getValue();
        return count != null ? count : 0;
    }

    // ======================== Internal ========================

    private List<AppNotification> getList() {
        List<AppNotification> list = notifications.getValue();
        return list != null ? list : Collections.emptyList();
    }

    private void recalculateUnreadCount(List<AppNotification> list) {
        int count = 0;
        for (AppNotification n : list) {
            if (!n.isRead()) count++;
        }
        unreadCount.postValue(count);
    }
}
