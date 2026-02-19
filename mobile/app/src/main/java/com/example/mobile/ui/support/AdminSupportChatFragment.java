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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

/**
 * Admin view: single chat with a user. Reached from AdminSupportListFragment.
 * Has a back button to return to the conversation list.
 * Subscribes via WebSocket for real-time messages.
 */
public class AdminSupportChatFragment extends Fragment {

    private static final String TAG = "AdminSupportChat";

    private ListView lvMessages;
    private EditText etMessage;
    private View btnSend;
    private View btnBack;
    private ProgressBar progressBar;
    private TextView tvUserName;
    private TextView tvUserRole;

    private ChatMessageAdapter adapter;
    private SharedPreferencesManager prefs;

    private long chatId;
    private String wsSubscriptionId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_support_chat, container, false);

        lvMessages = root.findViewById(R.id.lv_messages);
        etMessage = root.findViewById(R.id.et_message);
        btnSend = root.findViewById(R.id.btn_send);
        btnBack = root.findViewById(R.id.btn_back);
        progressBar = root.findViewById(R.id.progress_bar);
        tvUserName = root.findViewById(R.id.tv_chat_user_name);
        tvUserRole = root.findViewById(R.id.tv_chat_user_role);

        prefs = new SharedPreferencesManager(requireContext());
        adapter = new ChatMessageAdapter(requireContext(), true); // admin mode
        lvMessages.setAdapter(adapter);

        // Parse arguments
        Bundle args = getArguments();
        if (args != null) {
            chatId = args.getLong("chatId", -1);
            tvUserName.setText(args.getString("userName", "User"));
            tvUserRole.setText(args.getString("userRole", ""));
        }

        // Back button
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        if (chatId > 0) loadChat();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeWebSocket();
    }

    // ====================== REST ======================

    private void loadChat() {
        progressBar.setVisibility(View.VISIBLE);
        String token = getAuthToken();
        if (token == null) return;

        ClientUtils.supportService.getChatById(token, chatId).enqueue(new Callback<SupportChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<SupportChatResponse> call,
                                   @NonNull Response<SupportChatResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    SupportChatResponse chat = response.body();
                    tvUserName.setText(chat.getUserName());
                    tvUserRole.setText(chat.getUserRole());

                    List<SupportMessageResponse> messages = chat.getMessages();
                    if (messages != null) {
                        adapter.setMessages(messages);
                        scrollToBottom();
                    }

                    // Mark as read
                    markAsRead();
                    subscribeWebSocket();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SupportChatResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Load chat failed", t);
            }
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() || chatId <= 0) return;

        String token = getAuthToken();
        if (token == null) return;

        etMessage.setText("");

        ClientUtils.supportService.sendAdminMessage(token, chatId, new SupportMessageRequest(content))
                .enqueue(new Callback<SupportMessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<SupportMessageResponse> call,
                                           @NonNull Response<SupportMessageResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            if (adapter.addMessage(response.body())) {
                                scrollToBottom();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SupportMessageResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Send message error", t);
                    }
                });
    }

    private void markAsRead() {
        String token = getAuthToken();
        if (token == null) return;
        ClientUtils.supportService.markChatAsRead(token, chatId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
        });
    }

    // ====================== WebSocket ======================

    private void subscribeWebSocket() {
        unsubscribeWebSocket();
        String destination = "/topic/support/chat/" + chatId;
        wsSubscriptionId = WebSocketManager.getInstance().subscribe(
                destination, SupportMessageResponse.class, (SupportMessageResponse msg) -> {
                    if (!isAdded() || msg == null) return;
                    requireActivity().runOnUiThread(() -> {
                        if (adapter.addMessage(msg)) {
                            scrollToBottom();
                        }
                        // Keep marking as read while admin is viewing
                        markAsRead();
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

    // ====================== Helpers ======================

    private void scrollToBottom() {
        lvMessages.post(() -> lvMessages.setSelection(adapter.getCount() - 1));
    }

    private String getAuthToken() {
        String token = prefs.getToken();
        if (token == null) return null;
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
