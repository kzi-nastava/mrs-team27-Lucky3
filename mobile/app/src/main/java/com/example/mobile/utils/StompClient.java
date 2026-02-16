package com.example.mobile.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Lightweight STOMP-over-WebSocket client using OkHttp.
 * <p>
 * Connects to a STOMP endpoint (bypassing SockJS by appending /websocket),
 * authenticates via JWT in the CONNECT frame, and supports SUBSCRIBE/UNSUBSCRIBE.
 * <p>
 * Threading: all callbacks are dispatched to the main thread via Handler.
 * Reconnection uses Handler.postDelayed (no ExecutorService/RxJava/Coroutines).
 */
public class StompClient {

    private static final String TAG = "StompClient";
    private static final String STOMP_VERSION = "1.1";
    private static final long HEARTBEAT_INTERVAL_MS = 10_000;
    private static final long RECONNECT_DELAY_MS = 3_000;
    private static final long MAX_RECONNECT_DELAY_MS = 30_000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    /**
     * Callback for STOMP messages on a subscribed topic.
     */
    public interface MessageCallback<T> {
        void onMessage(T payload);
    }

    /**
     * Callback for connection lifecycle events.
     */
    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected(String reason);
        void onError(String error);
    }

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger subscriptionIdCounter = new AtomicInteger(0);
    private final Map<String, SubscriptionInfo> subscriptions = new HashMap<>();

    private WebSocket webSocket;
    private String serverUrl;
    private String jwtToken;
    private ConnectionCallback connectionCallback;
    private volatile boolean connected = false;
    private volatile boolean intentionalDisconnect = false;
    private int reconnectAttempts = 0;

    /**
     * Holds subscription metadata so we can resubscribe on reconnect.
     */
    private static class SubscriptionInfo {
        final String id;
        final String destination;
        final MessageCallback<String> rawCallback;

        SubscriptionInfo(String id, String destination, MessageCallback<String> rawCallback) {
            this.id = id;
            this.destination = destination;
            this.rawCallback = rawCallback;
        }
    }

    public StompClient(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Connect to the STOMP server.
     *
     * @param wsUrl    Base WebSocket URL, e.g. "ws://10.0.2.2:8081/ws"
     * @param token    JWT token (without "Bearer " prefix)
     * @param callback Connection lifecycle callback (nullable)
     */
    public void connect(String wsUrl, String token, ConnectionCallback callback) {
        this.serverUrl = wsUrl;
        this.jwtToken = token;
        this.connectionCallback = callback;
        this.intentionalDisconnect = false;
        this.reconnectAttempts = 0;
        doConnect();
    }

    private void doConnect() {
        // Append /websocket to bypass SockJS and get raw STOMP-over-WebSocket
        String url = serverUrl.endsWith("/") ? serverUrl + "websocket" : serverUrl + "/websocket";
        Log.d(TAG, "Connecting to " + url);

        Request request = new Request.Builder().url(url).build();
        webSocket = httpClient.newWebSocket(request, new StompWebSocketListener());
    }

    /**
     * Disconnect gracefully.
     */
    public void disconnect() {
        intentionalDisconnect = true;
        connected = false;
        mainHandler.removeCallbacksAndMessages(null);

        if (webSocket != null) {
            try {
                sendFrame("DISCONNECT", null, null);
                webSocket.close(1000, "Client disconnect");
            } catch (Exception e) {
                Log.w(TAG, "Error during disconnect", e);
            }
            webSocket = null;
        }

        subscriptions.clear();
    }

    /**
     * Subscribe to a STOMP destination and receive typed payloads.
     *
     * @param destination STOMP destination, e.g. "/topic/ride/5"
     * @param type        Class of the expected payload
     * @param callback    Called on the main thread with deserialized object
     * @return Subscription ID (use for unsubscribe)
     */
    public <T> String subscribe(String destination, Type type, MessageCallback<T> callback) {
        String subId = "sub-" + subscriptionIdCounter.incrementAndGet();

        // Wrap typed callback in a raw string callback that does Gson deserialization
        MessageCallback<String> rawCallback = body -> {
            try {
                T parsed = gson.fromJson(body, type);
                callback.onMessage(parsed);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse message from " + destination, e);
            }
        };

        SubscriptionInfo info = new SubscriptionInfo(subId, destination, rawCallback);
        subscriptions.put(subId, info);

        if (connected) {
            sendSubscribe(subId, destination);
        }
        // If not connected yet, will be sent on CONNECTED frame (resubscribe)

        return subId;
    }

    /**
     * Unsubscribe from a subscription.
     */
    public void unsubscribe(String subscriptionId) {
        SubscriptionInfo info = subscriptions.remove(subscriptionId);
        if (info != null && connected) {
            Map<String, String> headers = new HashMap<>();
            headers.put("id", subscriptionId);
            sendFrame("UNSUBSCRIBE", headers, null);
        }
    }

    /**
     * Check if currently connected.
     */
    public boolean isConnected() {
        return connected;
    }

    // ==================== Internal STOMP frame handling ====================

    private void sendStompConnect() {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept-version", STOMP_VERSION);
        headers.put("heart-beat", HEARTBEAT_INTERVAL_MS + "," + HEARTBEAT_INTERVAL_MS);
        if (jwtToken != null && !jwtToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + jwtToken);
        }
        sendFrame("CONNECT", headers, null);
    }

    private void sendSubscribe(String subId, String destination) {
        Map<String, String> headers = new HashMap<>();
        headers.put("id", subId);
        headers.put("destination", destination);
        sendFrame("SUBSCRIBE", headers, null);
    }

    private void sendFrame(String command, Map<String, String> headers, String body) {
        if (webSocket == null) return;

        StringBuilder frame = new StringBuilder();
        frame.append(command).append("\n");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                frame.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
        }
        frame.append("\n");
        if (body != null) {
            frame.append(body);
        }
        frame.append("\u0000"); // NULL terminator

        webSocket.send(frame.toString());
    }

    private void handleStompMessage(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("\n") || raw.equals("\r\n")) {
            // Heartbeat — respond with newline
            if (webSocket != null) webSocket.send("\n");
            return;
        }

        // Parse STOMP frame: COMMAND\nheaders...\n\nbody\0
        int firstNewline = raw.indexOf('\n');
        if (firstNewline < 0) return;

        String command = raw.substring(0, firstNewline).trim();
        String rest = raw.substring(firstNewline + 1);

        // Split headers from body at the blank line
        int blankLine = rest.indexOf("\n\n");
        String headersStr = blankLine >= 0 ? rest.substring(0, blankLine) : rest;
        String body = blankLine >= 0 ? rest.substring(blankLine + 2) : "";

        // Remove NULL terminator
        if (body.endsWith("\u0000")) {
            body = body.substring(0, body.length() - 1);
        }

        Map<String, String> headers = new HashMap<>();
        for (String line : headersStr.split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
            }
        }

        switch (command) {
            case "CONNECTED":
                connected = true;
                reconnectAttempts = 0;
                Log.i(TAG, "STOMP CONNECTED");
                // Resubscribe all existing subscriptions
                for (SubscriptionInfo info : subscriptions.values()) {
                    sendSubscribe(info.id, info.destination);
                }
                mainHandler.post(() -> {
                    if (connectionCallback != null) connectionCallback.onConnected();
                });
                break;

            case "MESSAGE":
                String subId = headers.get("subscription");
                if (subId != null) {
                    SubscriptionInfo info = subscriptions.get(subId);
                    if (info != null) {
                        String finalBody = body;
                        mainHandler.post(() -> info.rawCallback.onMessage(finalBody));
                    }
                }
                break;

            case "ERROR":
                String errorMsg = headers.getOrDefault("message", body);
                Log.e(TAG, "STOMP ERROR: " + errorMsg);
                mainHandler.post(() -> {
                    if (connectionCallback != null) connectionCallback.onError(errorMsg);
                });
                break;

            case "RECEIPT":
                Log.d(TAG, "STOMP RECEIPT: " + headers.get("receipt-id"));
                break;

            default:
                Log.d(TAG, "Unhandled STOMP command: " + command);
                break;
        }
    }

    private void scheduleReconnect() {
        if (intentionalDisconnect) return;
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached, giving up");
            mainHandler.post(() -> {
                if (connectionCallback != null) {
                    connectionCallback.onDisconnected("Max reconnect attempts reached");
                }
            });
            return;
        }

        long delay = Math.min(RECONNECT_DELAY_MS * (1L << reconnectAttempts), MAX_RECONNECT_DELAY_MS);
        reconnectAttempts++;
        Log.i(TAG, "Scheduling reconnect in " + delay + "ms (attempt " + reconnectAttempts + ")");

        mainHandler.postDelayed(this::doConnect, delay);
    }

    // ==================== OkHttp WebSocket listener ====================

    private class StompWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            Log.d(TAG, "WebSocket opened");
            sendStompConnect();
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            handleStompMessage(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "WebSocket closing: " + code + " " + reason);
            webSocket.close(1000, null);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            Log.d(TAG, "WebSocket closed: " + code + " " + reason);
            connected = false;
            mainHandler.post(() -> {
                if (connectionCallback != null) connectionCallback.onDisconnected(reason);
            });
            scheduleReconnect();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            Log.e(TAG, "WebSocket failure", t);
            connected = false;
            mainHandler.post(() -> {
                if (connectionCallback != null) connectionCallback.onError(t.getMessage());
            });
            scheduleReconnect();
        }
    }
}
