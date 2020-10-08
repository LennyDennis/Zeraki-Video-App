package com.lennydennis.zerakiapp.ui;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.lennydennis.zerakiapp.util.PermissionUtil;

public class RoomFragmentViewModel extends AndroidViewModel {

    PermissionUtil mPermissionUtil;

    public RoomFragmentViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO);
        return (resultCamera == PackageManager.PERMISSION_GRANTED) && (resultMic == PackageManager.PERMISSION_GRANTED);
    }
}
