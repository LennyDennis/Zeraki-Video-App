package com.lennydennis.zerakiapp.ui.viewmodels;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.lennydennis.zerakiapp.model.AccessTokenState;
import com.lennydennis.zerakiapp.repositories.AccessTokenRepo;
import com.lennydennis.zerakiapp.repositories.AccessTokenRepoImpl;
import com.twilio.video.CameraCapturer;

public class RoomFragmentViewModel extends AndroidViewModel {

    private AccessTokenRepo mAccessTokenRepo = new AccessTokenRepoImpl();
    public MutableLiveData<AccessTokenState> mAccessTokenMutableLiveData = new MutableLiveData<>();

    public RoomFragmentViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO);
        return (resultCamera == PackageManager.PERMISSION_GRANTED) && (resultMic == PackageManager.PERMISSION_GRANTED);
    }

    public CameraCapturer.CameraSource getAvailableCameraSource() {
        return (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA)) ?
                (CameraCapturer.CameraSource.FRONT_CAMERA) :
                (CameraCapturer.CameraSource.BACK_CAMERA);
    }

    public MutableLiveData<AccessTokenState> fetchAccessToken(String userName, String roomName){
        return  mAccessTokenMutableLiveData = mAccessTokenRepo.fetchAccessToken(userName,roomName);
    }

}
