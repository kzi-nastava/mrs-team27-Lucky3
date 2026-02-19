package com.example.mobile.ui.support;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.models.SupportChatListItemResponse;
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
 * Admin view: list of all support conversations.
 * Subscribes via WebSocket for real-time chat list updates and new messages.
 * Tapping a conversation navigates to AdminSupportChatFragment.
 */
public class AdminSupportListFragment extends Fragment {

    private static final String TAG = "AdminSupportList";

    private ListView lvChats;
    private ProgressBar progressBar;
    private TextView tvError;
    private View emptyState;

    private SupportChatListAdapter adapter;
    private SharedPreferencesManager prefs;

    private String chatListSubId;
    private String adminMsgSubId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_support_list, container, false);

        // Navbar
        NavbarHelper.setup(this, root, "Support");

        lvChats = root.findViewById(R.id.lv_chats);
        progressBar = root.findViewById(R.id.progress_bar);
        tvError = root.findViewById(R.id.tv_error);
        emptyState = root.findViewById(R.id.empty_state);

        prefs = new SharedPreferencesManager(requireContext());
        adapter = new SupportChatListAdapter(requireContext());
        lvChats.setAdapter(adapter);

        // Tap to open chat
        lvChats.setOnItemClickListener((parent, view, position, id) -> {
            SupportChatListItemResponse chat = adapter.getItem(position);
            if (chat == null || chat.getId() == null) return;

            Bundle args = new Bundle();
            args.putLong("chatId", chat.getId());
            args.putString("userName", chat.getUserName());
            args.putString("userRole", chat.getUserRole());

            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_admin_support_list_to_chat, args);
        });

        loadChats();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-subscribe when coming back from chat
        subscribeWebSocket();
        // Refresh list in case chats were read
        refreshChats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unsubscribeWebSocket();
    }

    // ====================== REST ======================

    private void loadChats() {
        showLoading();
        String token = getAuthToken();
        if (token == null) {
            showError("Not logged in");
            return;
        }

        ClientUtils.supportService.getAllChats(token).enqueue(new Callback<List<SupportChatListItemResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportChatListItemResponse>> call,
                                   @NonNull Response<List<SupportChatListItemResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<SupportChatListItemResponse> chats = response.body();
                    adapter.setChats(chats);
                    if (chats.isEmpty()) showEmpty(); else showList();
                    subscribeWebSocket();
                } else {
                    showError("Failed to load chats (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportChatListItemResponse>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showError("Connection error: " + t.getMessage());
            }
        });
    }

    private void refreshChats() {
        String token = getAuthToken();
        if (token == null) return;

        ClientUtils.supportService.getAllChats(token).enqueue(new Callback<List<SupportChatListItemResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupportChatListItemResponse>> call,
                                   @NonNull Response<List<SupportChatListItemResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<SupportChatListItemResponse> chats = response.body();
                    adapter.setChats(chats);
                    if (chats.isEmpty()) showEmpty(); else showList();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SupportChatListItemResponse>> call, @NonNull Throwable t) {
                // Silently ignore refresh errors
            }
        });
    }

    // ====================== WebSocket ======================

    private void subscribeWebSocket() {
        unsubscribeWebSocket();

        // Subscribe to chat list updates (unread counts, new chats)
        chatListSubId = WebSocketManager.getInstance().subscribe(
                "/topic/support/admin/chats",
                SupportChatListItemResponse.class,
                (SupportChatListItemResponse chatItem) -> {
                    if (!isAdded() || chatItem == null) return;
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateChat(chatItem);
                        showList();
                    });
                });

        // Subscribe to new messages (to update list even when a new message arrives)
        adminMsgSubId = WebSocketManager.getInstance().subscribe(
                "/topic/support/admin/messages",
                SupportMessageResponse.class,
                (SupportMessageResponse msg) -> {
                    // Chat list update handles UI; this is for awareness
                    Log.d(TAG, "Admin WS message: " + (msg != null ? msg.getChatId() : "null"));
                });
    }

    private void unsubscribeWebSocket() {
        if (chatListSubId != null) {
            WebSocketManager.getInstance().unsubscribe(chatListSubId);
            chatListSubId = null;
        }
        if (adminMsgSubId != null) {
            WebSocketManager.getInstance().unsubscribe(adminMsgSubId);
            adminMsgSubId = null;
        }
    }

    // ====================== UI helpers ======================

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        lvChats.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        lvChats.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        lvChats.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        lvChats.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }

    private String getAuthToken() {
        String token = prefs.getToken();
        if (token == null) return null;
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
