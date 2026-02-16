package com.example.mobile.ui.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.mobile.R;
import com.example.mobile.models.SupportChatListItemResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ListView adapter for the admin's support conversation list.
 * Shows user name, role badge, last message preview, time, and unread count.
 */
public class SupportChatListAdapter extends BaseAdapter {

    private final Context context;
    private final List<SupportChatListItemResponse> chats;

    public SupportChatListAdapter(Context context) {
        this.context = context;
        this.chats = new ArrayList<>();
    }

    public void setChats(List<SupportChatListItemResponse> newChats) {
        chats.clear();
        if (newChats != null) {
            chats.addAll(newChats);
        }
        notifyDataSetChanged();
    }

    /**
     * Update a single chat in the list (used for real-time WebSocket updates).
     * Moves updated chat to top and re-sorts by lastMessageTime.
     */
    public void updateChat(SupportChatListItemResponse updated) {
        if (updated == null || updated.getId() == null) return;

        // Remove existing entry
        for (int i = 0; i < chats.size(); i++) {
            if (updated.getId().equals(chats.get(i).getId())) {
                chats.remove(i);
                break;
            }
        }

        // Insert at top (newest first)
        chats.add(0, updated);
        notifyDataSetChanged();
    }

    public int getTotalUnreadCount() {
        int total = 0;
        for (SupportChatListItemResponse chat : chats) {
            total += chat.getUnreadCount();
        }
        return total;
    }

    @Override
    public int getCount() { return chats.size(); }

    @Override
    public SupportChatListItemResponse getItem(int position) { return chats.get(position); }

    @Override
    public long getItemId(int position) {
        Long id = chats.get(position).getId();
        return id != null ? id : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_support_chat, parent, false);
            holder = new ViewHolder();
            holder.tvInitial = convertView.findViewById(R.id.tv_user_initial);
            holder.tvUserName = convertView.findViewById(R.id.tv_user_name);
            holder.tvRoleBadge = convertView.findViewById(R.id.tv_role_badge);
            holder.tvLastMessage = convertView.findViewById(R.id.tv_last_message);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.tvUnreadCount = convertView.findViewById(R.id.tv_unread_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SupportChatListItemResponse chat = getItem(position);

        // User initial
        String name = chat.getUserName();
        holder.tvInitial.setText(name != null && !name.isEmpty()
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?");

        // Name
        holder.tvUserName.setText(name != null ? name : "Unknown");

        // Role badge
        String role = chat.getUserRole();
        holder.tvRoleBadge.setText(role != null ? role : "");
        holder.tvRoleBadge.setVisibility(role != null ? View.VISIBLE : View.GONE);

        // Last message
        String lastMsg = chat.getLastMessage();
        holder.tvLastMessage.setText(lastMsg != null && !lastMsg.isEmpty() ? lastMsg : "No messages");

        // Time
        holder.tvTime.setText(formatRelativeTime(chat.getLastMessageTime()));

        // Unread count
        int unread = chat.getUnreadCount();
        if (unread > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(unread > 99 ? "99+" : String.valueOf(unread));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        return convertView;
    }

    private String formatRelativeTime(String timestamp) {
        if (timestamp == null) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            if (date == null) return "";

            long diff = System.currentTimeMillis() - date.getTime();
            long minute = 60_000L;
            long hour = 3_600_000L;
            long day = 86_400_000L;

            if (diff < minute) return "now";
            if (diff < hour) return (diff / minute) + "m";
            if (diff < day) return (diff / hour) + "h";
            if (diff < 7 * day) return (diff / day) + "d";
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    static class ViewHolder {
        TextView tvInitial;
        TextView tvUserName;
        TextView tvRoleBadge;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
    }
}
