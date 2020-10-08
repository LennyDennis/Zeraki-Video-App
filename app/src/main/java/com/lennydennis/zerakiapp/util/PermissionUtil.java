package com.lennydennis.zerakiapp.util;

import android.content.Context;

import androidx.core.content.ContextCompat;

public class PermissionUtil {
    private Context mContext;

    public int isPermissionGranted(String permission){
        return ContextCompat.checkSelfPermission(mContext, permission);
    }
}




