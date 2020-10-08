package com.lennydennis.zerakiapp.repositories;

import androidx.lifecycle.MutableLiveData;

public interface AccessTokenRepo {
    MutableLiveData<String> fetchAccessToken(String userName, String roomName);
}
