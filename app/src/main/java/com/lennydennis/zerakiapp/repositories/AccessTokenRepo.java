package com.lennydennis.zerakiapp.repositories;

import androidx.lifecycle.MutableLiveData;

import com.lennydennis.zerakiapp.model.AccessTokenState;

public interface AccessTokenRepo {
    MutableLiveData<AccessTokenState> fetchAccessToken(String userName, String roomName);
}
