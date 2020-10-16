package com.lennydennis.zerakiapp.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.lennydennis.zerakiapp.databinding.RoomDialogBinding;

public class RoomDialog extends DialogFragment {

    private RoomDialogBinding mRoomDialogBinding;
    private TextInputEditText mEditTextUsername;
    private TextInputEditText mEditTextPassword;
    private RoomDialogListener listener;
    private Spinner mRoomTypeSpinner;
    private ImageView mCancelButton;
    private Button mConnectButton;
    private String mRoomType;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mRoomDialogBinding = RoomDialogBinding.inflate(requireActivity().getLayoutInflater());
        View view = mRoomDialogBinding.getRoot();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view);

        mEditTextUsername = mRoomDialogBinding.etUserName;
        mEditTextPassword = mRoomDialogBinding.etRoomName;
        mRoomTypeSpinner = mRoomDialogBinding.roomTypeSpinner;
        mCancelButton = mRoomDialogBinding.closeDialog;
        mConnectButton = mRoomDialogBinding.connectRoomButton;

        mRoomTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mRoomType = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = mEditTextUsername.getText().toString();
                String roomName = mEditTextPassword.getText().toString();
                if (!userName.isEmpty() && !roomName.isEmpty() && !mRoomType.isEmpty()) {
                    listener.connectRoom(userName, roomName, mRoomType);
                    getDialog().dismiss();
                } else {
                    Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (RoomDialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement ExampleDialogListener");
        }
    }

    public interface RoomDialogListener {
        void connectRoom(String username, String password, String roomType);
    }
}
