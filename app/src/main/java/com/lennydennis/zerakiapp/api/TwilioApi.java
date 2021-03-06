package com.lennydennis.zerakiapp.api;

import com.twilio.video.Room;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TwilioApi {
    @GET("token")
    Call<String> getAccessToken(@Query("username") String username, @Query("roomname") String roomName);

    @GET("room")
    Call<Room> createPeerToPeerRoom(@Query("roomname") String roomName);
}
