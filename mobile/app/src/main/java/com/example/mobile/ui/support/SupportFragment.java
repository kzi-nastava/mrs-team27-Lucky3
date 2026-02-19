package com.example.mobile.ui.support;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.models.SupportChatResponse;
import com.example.mobile.models.SupportMessageRequest;
import com.example.mobile.models.SupportMessageResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.example.mobile.utils.WebSocketManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.mobile.utils.NavbarHelper;

/**
 * Support chat screen for drivers and passengers.
 * Loads (or creates) the user's single support chat and displays messages.
 * Subscribes via WebSocket for real-time incoming messages.
 */
public class SupportFragment extends Fragment {

    private static final String TAG = "SupportFragment";

    private ListView lvMessages;
    private EditText etMessage;
    private View btnSend;
    private ProgressBar progressBar;
    private TextView tvError;
    private View emptyState;

    private ChatMessageAdapter adapter;
    private SharedPreferencesManager prefs;

    private Long chatId;
    private String wsSubscriptionId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_support_chat, container, false);

        // Navbar setup
        NavbarHelper.setup(this, root, "Support");

        lvMessages = root.findViewById(R.id.lv_messages);
        etMessage = root.findViewById(R.id.et_message);
        btnSend = root.findViewById(R.id.btn_send);
        progressBar = root.findViewById(R.id.progress_bar);
        tvError = root.findViewById(R.id.tv_error);
        emptyState = root.findViewById(R.id.empty_state);

        prefs = new SharedPreferencesManager(requireContext());
        adapter = new ChatMessageAdapter(requireContext(), false);
        lvMessages.setAdapter(adapter);

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        loadChat();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeWebSocket();
    }

    // ====================== REST calls ======================

    private void loadChat() {
        showLoading();
        String token = getAuthToken();
        if (token == null) {
            showError("Not logged in");
            return;
        }

        ClientUtils.supportService.getMyChat(token).enqueue(new Callback<SupportChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<SupportChatResponse> call,
                                   @NonNull Response<SupportChatResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    SupportChatResponse chat = response.body();
                    chatId = chat.getId();
                    List<SupportMessageResponse> messages = chat.getMessages();

                    if (messages != null && !messages.isEmpty()) {
                        adapter.setMessages(messages);
                        showMessages();
                        scrollToBottom();
                    } else {
                        showEmpty();
                    }
                    subscribeWebSocket();
                } else {
                    showError("Failed to load chat (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupportChatResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Connection error: " + t.getMessage());
            }
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        String token = getAuthToken();
        if (token == null) return;

        etMessage.setText("");

        ClientUtils.supportService.sendUserMessage(token, new SupportMessageRequest(content))
                .enqueue(new Callback<SupportMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SupportMessageResponse> call,
                                           @NonNull Response<SupportMessageResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            // Message will arrive via WebSocket, but add immediately for responsiveness
                            if (adapter.addMessage(response.body())) {
                                showMessages();
                                scrollToBottom();
                            }
                        } else {
                            Log.w(TAG, "Send message failed: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SupportMessageResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Send message error", t);
                    }
                });
    }

    // ====================== WebSocket ======================

    private void subscribeWebSocket() {
        if (chatId == null) return;
        unsubscribeWebSocket();

        String destination = "/topic/support/chat/" + chatId;
        wsSubscriptionId = WebSocketManager.getInstance().subscribe(
                destination, SupportMessageResponse.class, (SupportMessageResponse msg) -> {
                    if (!isAdded() || msg == null) return;
                    requireActivity().runOnUiThread(() -> {
                        if (adapter.addMessage(msg)) {
                            showMessages();
                            scrollToBottom();
                        }
                    });
                });
        Log.d(TAG, "Subscribed to " + destination);
    }

    private void unsubscribeWebSocket() {
        if (wsSubscriptionId != null) {
            WebSocketManager.getInstance().unsubscribe(wsSubscriptionId);
            wsSubscriptionId = null;
        }
    }

    // ====================== UI helpers ======================

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        lvMessages.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showMessages() {
        progressBar.setVisibility(View.GONE);
        lvMessages.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        lvMessages.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        lvMessages.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    private void scrollToBottom() {
        lvMessages.post(() -> lvMessages.setSelection(adapter.getCount() - 1));
    }

    private String getAuthToken() {
        String token = prefs.getToken();
        if (token == null) return null;
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
