package com.example.mobile.ui.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobile.R;
import com.example.mobile.models.SupportMessageResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ListView adapter for chat messages. Uses the ViewHolder pattern.
 * Switches between admin (left, gray) and user (right, yellow) bubbles.
 */
public class ChatMessageAdapter extends BaseAdapter {

    private final Context context;
    private final List<SupportMessageResponse> messages;
    private final boolean isAdmin;
    private final Set<Long> messageIds;

    /**
     * @param context Activity/Fragment context
     * @param isAdmin true when admin is viewing (their messages are right-aligned)
     */
    public ChatMessageAdapter(Context context, boolean isAdmin) {
        this.context = context;
        this.messages = new ArrayList<>();
        this.isAdmin = isAdmin;
        this.messageIds = new HashSet<>();
    }

    public void setMessages(List<SupportMessageResponse> newMessages) {
        messages.clear();
        messageIds.clear();
        if (newMessages != null) {
            for (SupportMessageResponse msg : newMessages) {
                messages.add(msg);
                if (msg.getId() != null) messageIds.add(msg.getId());
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Adds a message if it doesn't already exist (deduplication by ID).
     * @return true if the message was added
     */
    public boolean addMessage(SupportMessageResponse msg) {
        if (msg == null) return false;
        if (msg.getId() != null && messageIds.contains(msg.getId())) return false;
        messages.add(msg);
        if (msg.getId() != null) messageIds.add(msg.getId());
        notifyDataSetChanged();
        return true;
    }

    @Override
    public int getCount() { return messages.size(); }

    @Override
    public SupportMessageResponse getItem(int position) { return messages.get(position); }

    @Override
    public long getItemId(int position) {
        Long id = messages.get(position).getId();
        return id != null ? id : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_chat_message, parent, false);
            holder = new ViewHolder();
            holder.adminBubble = convertView.findViewById(R.id.admin_bubble);
            holder.tvAdminSender = convertView.findViewById(R.id.tv_admin_sender);
            holder.tvAdminContent = convertView.findViewById(R.id.tv_admin_content);
            holder.tvAdminTime = convertView.findViewById(R.id.tv_admin_time);
            holder.userBubble = convertView.findViewById(R.id.user_bubble);
            holder.tvUserContent = convertView.findViewById(R.id.tv_user_content);
            holder.tvUserTime = convertView.findViewById(R.id.tv_user_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SupportMessageResponse msg = getItem(position);
        boolean showAsRight = isAdmin ? msg.isFromAdmin() : !msg.isFromAdmin();

        if (showAsRight) {
            // Current user's message → right (yellow)
            holder.userBubble.setVisibility(View.VISIBLE);
            holder.adminBubble.setVisibility(View.GONE);
            holder.tvUserContent.setText(msg.getContent());
            holder.tvUserTime.setText(formatTime(msg.getTimestamp()));
        } else {
            // Other party's message → left (gray)
            holder.adminBubble.setVisibility(View.VISIBLE);
            holder.userBubble.setVisibility(View.GONE);
            holder.tvAdminSender.setText(msg.getSenderName());
            holder.tvAdminContent.setText(msg.getContent());
            holder.tvAdminTime.setText(formatTime(msg.getTimestamp()));
        }

        return convertView;
    }

    private String formatTime(String timestamp) {
        if (timestamp == null) return "";
        try {
            // Backend returns ISO LocalDateTime format: yyyy-MM-dd'T'HH:mm:ss
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            if (date == null) return timestamp;

            long diff = System.currentTimeMillis() - date.getTime();
            long dayMs = 86400000L;

            if (diff < dayMs) {
                // Today — show time only
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } else if (diff < 7 * dayMs) {
                // This week — show day + time
                return new SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(date);
            } else {
                // Older — show full date
                return new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            return timestamp;
        }
    }

    static class ViewHolder {
        LinearLayout adminBubble;
        TextView tvAdminSender;
        TextView tvAdminContent;
        TextView tvAdminTime;
        LinearLayout userBubble;
        TextView tvUserContent;
        TextView tvUserTime;
    }
}
