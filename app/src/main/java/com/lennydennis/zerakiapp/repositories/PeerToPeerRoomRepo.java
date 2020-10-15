package com.lennydennis.zerakiapp.repositories;

import androidx.lifecycle.MutableLiveData;

import com.lennydennis.zerakiapp.model.PeerToPeerRoomState;

public interface PeerToPeerRoomRepo {
    MutableLiveData<PeerToPeerRoomState> createPeerToPeerRoom(String roomName);
}
