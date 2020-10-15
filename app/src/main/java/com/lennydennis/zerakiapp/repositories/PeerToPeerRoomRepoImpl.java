package com.lennydennis.zerakiapp.repositories;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.lennydennis.zerakiapp.api.TwilioApi;
import com.lennydennis.zerakiapp.api.TwilioRetrofitInstance;
import com.lennydennis.zerakiapp.model.PeerToPeerRoomState;
import com.twilio.video.Room;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PeerToPeerRoomRepoImpl implements PeerToPeerRoomRepo{
    private static final String TAG = "PeerToPeerRoomRepoImpl";

    public PeerToPeerRoomRepoImpl() {
    }

    @Override
    public MutableLiveData<PeerToPeerRoomState> createPeerToPeerRoom(String roomName) {
        TwilioApi twilioApiService = TwilioRetrofitInstance.getTwilioReftrofitInstance();
        MutableLiveData<PeerToPeerRoomState> peerToPeerRoom = new MutableLiveData<>();
        Call<Room> call = twilioApiService.createPeerToPeerRoom(roomName);
        call.enqueue(new Callback<Room>() {
            @Override
            public void onResponse(Call<Room> call, Response<Room> response) {
                if (response.isSuccessful()) {
                    peerToPeerRoom.setValue(new PeerToPeerRoomState(response.body()));
                } else {
                    Log.e(TAG, "onResponse: Not Successful");
                }
            }

            @Override
            public void onFailure(Call<Room> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t);
            }
        });
        return peerToPeerRoom;
    }
}
