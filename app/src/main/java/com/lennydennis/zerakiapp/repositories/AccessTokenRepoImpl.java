package com.lennydennis.zerakiapp.repositories;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.lennydennis.zerakiapp.api.TwilioApi;
import com.lennydennis.zerakiapp.api.TwilioRetrofitInstance;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessTokenRepoImpl implements AccessTokenRepo {

    private static final String TAG = "AccessTokenRepoImpl";

    public AccessTokenRepoImpl() {
    }

    private TwilioApi twilioApiService = TwilioRetrofitInstance.getTwilioReftrofitInstance();

    @Override
    public MutableLiveData<String> fetchAccessToken(String userName, String roomName) {
        final MutableLiveData<String> roomAccessToken = new MutableLiveData<>();
        Call<String> call = twilioApiService.getAccessToken(userName, roomName);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    roomAccessToken.setValue(response.body());
                } else {
                    Log.e(TAG, "onResponse: Not Successful");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t );
            }
        });

        return null;
    }
}
