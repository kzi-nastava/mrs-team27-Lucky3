package com.example.mobile.utils;

import android.content.Context;
import android.util.Log;

import com.example.mobile.BuildConfig;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Singleton manager for the STOMP WebSocket connection.
 * <p>
 * Provides a single shared StompClient instance that fragments can use
 * to subscribe/unsubscribe from topics. Manages connection lifecycle
 * tied to authentication state.
 * <p>
 * Usage:
 * <pre>
 *   WebSocketManager.getInstance().connect(context);
 *   String subId = WebSocketManager.getInstance().subscribe("/topic/ride/5", RideResponse.class, ride -> { ... });
 *   WebSocketManager.getInstance().unsubscribe(subId);
 *   WebSocketManager.getInstance().disconnect();
 * </pre>
 */
public class WebSocketManager {

    private static final String TAG = "WebSocketManager";

    private static volatile WebSocketManager instance;

    private StompClient stompClient;
    private boolean connecting = false;

    /**
     * OkHttpClient dedicated to WebSocket (separate from Retrofit's client).
     * Longer read timeout to keep the connection alive.
     */
    private final OkHttpClient wsHttpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)  // No read timeout for WebSocket
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private final Gson gson = ClientUtils.getGson();

    private WebSocketManager() {
        stompClient = new StompClient(wsHttpClient, gson);
    }

    /**
     * Get the singleton instance.
     */
    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }

    /**
     * Connect to the backend STOMP WebSocket using the stored JWT token.
     *
     * @param context Android context (used to read token from SharedPreferences)
     */
    public void connect(Context context) {
        connect(context, null);
    }

    /**
     * Connect to the backend STOMP WebSocket with a lifecycle callback.
     *
     * @param context  Android context
     * @param callback Optional connection callback
     */
    public void connect(Context context, StompClient.ConnectionCallback callback) {
        if (stompClient.isConnected() || connecting) {
            Log.d(TAG, "Already connected or connecting, skipping");
            if (stompClient.isConnected() && callback != null) {
                callback.onConnected();
            }
            return;
        }

        SharedPreferencesManager prefs = new SharedPreferencesManager(context);
        String token = prefs.getToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "No JWT token available, cannot connect WebSocket");
            if (callback != null) callback.onError("No authentication token");
            return;
        }

        String wsUrl = "ws://" + BuildConfig.IP_ADDR + ":8081/ws";
        connecting = true;

        stompClient.connect(wsUrl, token, new StompClient.ConnectionCallback() {
            @Override
            public void onConnected() {
                connecting = false;
                Log.i(TAG, "WebSocket connected");
                if (callback != null) callback.onConnected();
            }

            @Override
            public void onDisconnected(String reason) {
                connecting = false;
                Log.i(TAG, "WebSocket disconnected: " + reason);
                if (callback != null) callback.onDisconnected(reason);
            }

            @Override
            public void onError(String error) {
                connecting = false;
                Log.e(TAG, "WebSocket error: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    /**
     * Disconnect the WebSocket.
     */
    public void disconnect() {
        connecting = false;
        stompClient.disconnect();
    }

    /**
     * Subscribe to a STOMP destination.
     *
     * @param destination Topic path, e.g. "/topic/ride/5"
     * @param type        Type of the expected payload (Class or TypeToken)
     * @param callback    Called on the main thread with the deserialized payload
     * @return Subscription ID for later unsubscribe
     */
    public <T> String subscribe(String destination, Type type, StompClient.MessageCallback<T> callback) {
        return stompClient.subscribe(destination, type, callback);
    }

    /**
     * Unsubscribe from a topic.
     *
     * @param subscriptionId The ID returned by subscribe()
     */
    public void unsubscribe(String subscriptionId) {
        stompClient.unsubscribe(subscriptionId);
    }

    /**
     * Check if currently connected.
     */
    public boolean isConnected() {
        return stompClient.isConnected();
    }
}
