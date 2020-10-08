package com.lennydennis.zerakiapp.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.lennydennis.zerakiapp.R;
import com.lennydennis.zerakiapp.databinding.FragmentRoomBinding;
import com.lennydennis.zerakiapp.dialog.Dialog;
import com.lennydennis.zerakiapp.model.AccessTokenState;
import com.lennydennis.zerakiapp.util.CameraCapturerCompat;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTextureView;

import java.util.Collections;

public class RoomFragment extends Fragment {

    private static final String TAG = "RoomFragment";

    private static final String LOCAL_AUDIO_TRACK_NAME = "microphone";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";
    private static final String SCREEN_TRACK_NAME = "screen";

    private FragmentRoomBinding mFragmentRoomBinding;
    private RoomFragmentViewModel mRoomFragmentViewModel;
    private Context mContext;
    private VideoTextureView mPrimaryVideoView;
    private LocalAudioTrack mLocalAudioTrack;
    private CameraCapturerCompat mCameraCapturerCompat;
    private LocalVideoTrack mLocalVideoTrack;
    private VideoRenderer mLocalVideoView;
    private TextView mPrimaryParticipantIdentity;
    private TextView mSelectedParticipantIdentity;
    private ImageView mPrimaryParticipantStubImage;
    private com.lennydennis.zerakiapp.databinding.ParticipantPrimaryViewBinding mIncludeVideoView;
    private ImageButton mSwitchCameraButton;
    private ImageButton mToggleVideoButton;
    private ImageButton mToggleMicButton;
    private ImageButton mDisconnectCallButton;
    private ImageButton mVideoCallButton;
    private AlertDialog mConnectDialog;
    private String mAccessToken;
    private Room mRoom;
    private LocalParticipant mLocalParticipant;
    private String mUserName;
    private String mRoomName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        mFragmentRoomBinding = FragmentRoomBinding.inflate(inflater, container, false);
        mIncludeVideoView = mFragmentRoomBinding.includeVideoView;
        mPrimaryVideoView = mIncludeVideoView.participantVideo;
        mPrimaryParticipantIdentity = mIncludeVideoView.participantVideoIdentity;
        mSelectedParticipantIdentity = mIncludeVideoView.participantSelectedIdentity;
        mPrimaryParticipantStubImage = mIncludeVideoView.participantStubImage;
        mSwitchCameraButton = mFragmentRoomBinding.switchCamera;
        mToggleVideoButton = mFragmentRoomBinding.disableCamera;
        mToggleMicButton = mFragmentRoomBinding.disableMic;
        mDisconnectCallButton = mFragmentRoomBinding.disconnectCall;
        mVideoCallButton = mFragmentRoomBinding.videoCall;


        mContext = getContext();

        View view = mFragmentRoomBinding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(mFragmentRoomBinding.toolbar);

        initializeUI();
        // Inflate the layout for this fragment
        return view;
    }

    private void initializeUI() {
        mVideoCallButton.setOnClickListener(connectVideoCallClickListener());
        mSwitchCameraButton.setOnClickListener(switchCameraClickListener());
        mToggleVideoButton.setOnClickListener(toggleVideoClickListener());
        mToggleMicButton.setOnClickListener(toggleMicClickListener());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRoomFragmentViewModel = new ViewModelProvider(this).get(RoomFragmentViewModel.class);

        checkCameraMicPermission();
    }

    private void checkCameraMicPermission() {
        boolean permissions = mRoomFragmentViewModel.checkPermissionForCameraAndMicrophone();
        if (!permissions) {
            requestPermissionForCameraAndMicrophone();
        } else {
            setupAudioAndVideoTracks();
        }
    }

    private void requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA) || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(mContext, "Camera and Microphone permissions needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
            setupAudioAndVideoTracks();
        }
    }

    private void setupAudioAndVideoTracks() {
        mLocalAudioTrack = LocalAudioTrack.create(mContext, true, LOCAL_AUDIO_TRACK_NAME);
        mCameraCapturerCompat = new CameraCapturerCompat(mContext, mRoomFragmentViewModel.getAvailableCameraSource());
        mLocalVideoTrack = LocalVideoTrack.create(mContext, true, mCameraCapturerCompat.getVideoCapturer(), LOCAL_VIDEO_TRACK_NAME);
        mPrimaryVideoView.setMirror(true);
        mLocalVideoTrack.addRenderer(mPrimaryVideoView);
        mLocalVideoView = mPrimaryVideoView;

        mPrimaryParticipantStubImage.setVisibility(View.INVISIBLE);
        mPrimaryParticipantIdentity.setVisibility(View.INVISIBLE);
        mSelectedParticipantIdentity.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener connectVideoCallClickListener() {
        return v -> showConnectDialog();
    }

    private void showConnectDialog() {
        TextInputEditText userNameEditText = new TextInputEditText(requireActivity());
        TextInputEditText roomNameEditText = new TextInputEditText(requireActivity());
        mConnectDialog = Dialog.createConnectDialog(userNameEditText, roomNameEditText,
                connectClickListener(userNameEditText, roomNameEditText),
                cancelConnectDialogClickListener(),
                requireActivity());
        mConnectDialog.show();
    }

    private DialogInterface.OnClickListener connectClickListener(EditText userNameEditText,EditText roomNameEditText) {
        return (dialog, which) -> {
            mUserName = userNameEditText.getText().toString();
            mRoomName = roomNameEditText.getText().toString();
            mRoomFragmentViewModel.fetchAccessToken(mUserName, mRoomName);
            observerLiveData();
        };
    }

    private void observerLiveData() {
        mRoomFragmentViewModel.mAccessTokenMutableLiveData.observe(getViewLifecycleOwner(), new Observer<AccessTokenState>() {
            @Override
            public void onChanged(AccessTokenState accessTokenState) {
                if(accessTokenState.getAccessToken() != null && mRoomName != null){
                    connectToRoom(mRoomName,accessTokenState.getAccessToken());
                }else{
                    handleError(accessTokenState.getThrowable());
                }
            }
        });
    }

    private void handleError(Throwable throwable) {
        Toast.makeText(getContext(),throwable.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
    }

    private void connectToRoom(String roomName, String accessToken) {
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);
        if (mLocalAudioTrack != null) {
            connectOptionsBuilder
                    .audioTracks(Collections.singletonList(mLocalAudioTrack));
        }
        if (mLocalVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(mLocalVideoTrack));
        }

        mRoom = Video.connect(mContext, connectOptionsBuilder.build(), roomListener());
        setDisconnectAction();
    }

    private void setDisconnectAction() {
        connectVideoCall.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_baseline_call_end_24));
        connectVideoCall.show();
        connectVideoCall.setOnClickListener(disconnectClickListener());
    }

    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                mLocalParticipant = room.getLocalParticipant();

                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    //addRemoteParticipant(remoteParticipant);
                    break;
                }
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
            }

            @Override
            public void onReconnected(@NonNull Room room) {
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                initializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                mLocalParticipant = null;
                mRoom = null;
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
            }

            @Override
            public void onRecordingStarted(Room room) {
            }

            @Override
            public void onRecordingStopped(Room room) {
            }
        };
    }

    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return (dialog, which) -> {
            initializeUI();
            mConnectDialog.dismiss();
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (mCameraCapturerCompat != null) {
                CameraCapturer.CameraSource cameraSource = mCameraCapturerCompat.getCameraSource();
                mCameraCapturerCompat.switchCamera();
//                if (participantThumbnailVideoView.getVisibility() == View.VISIBLE) {
//                    participantThumbnailVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
//                } else {
                mPrimaryVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
                //}
            }
        };
    }


    private View.OnClickListener toggleVideoClickListener() {
        return v -> {
            if (mLocalVideoTrack != null) {
                boolean enable = !mLocalVideoTrack.isEnabled();
                mLocalVideoTrack.enable(enable);
                int icon;
                if (enable) {
                    icon = R.drawable.ic_videocam_white_24px;
                    mSwitchCameraButton.setEnabled(true);
                    mPrimaryParticipantStubImage.setVisibility(View.INVISIBLE);
                } else {
                    icon = R.drawable.ic_baseline_videocam_off_24;
                    mSwitchCameraButton.setEnabled(false);
                    mPrimaryParticipantStubImage.setVisibility(View.VISIBLE);
                }
                mToggleVideoButton.setImageDrawable(
                        ContextCompat.getDrawable(mContext, icon));
            }
        };
    }

    private View.OnClickListener toggleMicClickListener() {
        return v -> {
            if (mLocalAudioTrack != null) {
                boolean enable = !mLocalAudioTrack.isEnabled();
                mLocalAudioTrack.enable(enable);
                int icon = enable ?
                        R.drawable.ic_mic_white_24px : R.drawable.ic_baseline_mic_off_24;
                mToggleMicButton.setImageDrawable(ContextCompat.getDrawable(
                        mContext, icon));
            }
        };
    }


//    private void setDisconnectVideoCallAction() {
//        connectVideoCall.setImageDrawable(ContextCompat.getDrawable(this,
//                R.drawable.ic_baseline_call_end_24));
//        connectVideoCall.show();
//        connectVideoCall.setOnClickListener(disconnectClickListener());
//    }
}