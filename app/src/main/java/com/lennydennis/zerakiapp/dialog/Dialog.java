package com.lennydennis.zerakiapp.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lennydennis.zerakiapp.R;

public class Dialog {


    public static AlertDialog createConnectDialog(TextInputEditText userNameEditText, TextInputEditText roomNameEditText,
                                                  DialogInterface.OnClickListener callParticipantsClickListener,
                                                  DialogInterface.OnClickListener cancelClickListener,
                                                  Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setIcon(R.drawable.ic_baseline_videocam_24_black);
        dialog.setTitle("Connect to a room");
        dialog.setPositiveButton("Connect", callParticipantsClickListener);
        dialog.setNegativeButton("Cancel", cancelClickListener);
        dialog.setCancelable(false);

        setFieldsInDialog(userNameEditText, roomNameEditText, dialog, context);

        return dialog.create();
    }

    private static void setFieldsInDialog(TextInputEditText userNameEditText, TextInputEditText roomNameEditText,
                                          MaterialAlertDialogBuilder alertDialogBuilder,
                                          Context context) {

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60,0,60,0);

        userNameEditText.setHint("User name");
        layout.addView(userNameEditText);

        roomNameEditText.setHint("Room name");
        layout.addView(roomNameEditText);

        alertDialogBuilder.setView(layout);

    }

}
