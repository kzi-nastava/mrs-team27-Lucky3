package com.example.mobile.services;

import com.example.mobile.models.SupportChatListItemResponse;
import com.example.mobile.models.SupportChatResponse;
import com.example.mobile.models.SupportMessageRequest;
import com.example.mobile.models.SupportMessageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit service for support chat REST endpoints.
 * All endpoints require JWT auth via {@code @Header("Authorization")}.
 */
public interface SupportService {

    // ==================== User Endpoints ====================

    /** Get or create the current user's support chat. */
    @GET("api/support/chat")
    Call<SupportChatResponse> getMyChat(@Header("Authorization") String token);

    /** Send a message as user. */
    @POST("api/support/chat/message")
    Call<SupportMessageResponse> sendUserMessage(
            @Header("Authorization") String token,
            @Body SupportMessageRequest request);

    // ==================== Admin Endpoints ====================

    /** Get all support chats (admin only). */
    @GET("api/support/admin/chats")
    Call<List<SupportChatListItemResponse>> getAllChats(@Header("Authorization") String token);

    /** Get a specific chat with all messages (admin only). */
    @GET("api/support/admin/chat/{chatId}")
    Call<SupportChatResponse> getChatById(
            @Header("Authorization") String token,
            @Path("chatId") long chatId);

    /** Get messages for a specific chat (admin only). */
    @GET("api/support/admin/chat/{chatId}/messages")
    Call<List<SupportMessageResponse>> getChatMessages(
            @Header("Authorization") String token,
            @Path("chatId") long chatId);

    /** Send a message as admin to a specific chat. */
    @POST("api/support/admin/chat/{chatId}/message")
    Call<SupportMessageResponse> sendAdminMessage(
            @Header("Authorization") String token,
            @Path("chatId") long chatId,
            @Body SupportMessageRequest request);

    /** Mark a chat as read (resets unread count). */
    @POST("api/support/admin/chat/{chatId}/read")
    Call<Void> markChatAsRead(
            @Header("Authorization") String token,
            @Path("chatId") long chatId);
}
