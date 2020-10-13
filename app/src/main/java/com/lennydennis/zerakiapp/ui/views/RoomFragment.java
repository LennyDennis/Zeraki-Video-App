package com.lennydennis.zerakiapp.ui.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.lennydennis.zerakiapp.R;
import com.lennydennis.zerakiapp.databinding.FragmentRoomBinding;
import com.lennydennis.zerakiapp.dialog.Dialog;
import com.lennydennis.zerakiapp.model.AccessTokenState;
import com.lennydennis.zerakiapp.services.ScreenCapturerManager;
import com.lennydennis.zerakiapp.ui.participants.ParticipantController;
import com.lennydennis.zerakiapp.ui.participants.ParticipantPrimaryView;
import com.lennydennis.zerakiapp.ui.viewmodels.RoomFragmentViewModel;
import com.lennydennis.zerakiapp.util.CameraCapturerCompat;
import com.twilio.audioswitch.AudioSwitch;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.ScreenCapturer;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;

import java.util.Collections;
import java.util.List;

public class RoomFragment extends Fragment {

    private static final String TAG = "RoomFragment";

    private static final String LOCAL_AUDIO_TRACK_NAME = "microphone";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";
    private static final String SCREEN_TRACK_NAME = "screen";
    private static final int REQUEST_MEDIA_PROJECTION = 100;

    private FragmentRoomBinding mFragmentRoomBinding;
    private RoomFragmentViewModel mRoomFragmentViewModel;
    private Context mContext;
    private VideoView mPrimaryVideoView;
    private LocalAudioTrack mLocalAudioTrack;
    private CameraCapturerCompat mCameraCapturerCompat;
    private LocalVideoTrack mLocalVideoTrack;
    private VideoRenderer mLocalVideoView;
    //private TextView mPrimaryParticipantIdentity;
    private TextView mSelectedParticipantIdentity;
    private ImageView mPrimaryParticipantStubImage;
    private ImageButton mSwitchCameraButton;
    private ImageButton mToggleVideoButton;
    private ImageButton mToggleMicButton;
    private ImageButton mVideoCallButton;
    private AlertDialog mConnectDialog;
    private Room mRoom;
    private LocalParticipant mLocalParticipant;
    private String mUserName;
    private String mRoomName;
    private ProgressBar mReconnectingProgressBar;
    private VideoView mParticipantThumbNailView;
    private MenuItem mScreenCaptureMenuItem;
    private String mRemoteParticipantIdentity;
    private Boolean mDisconnectedFromOnDestroy;
    private int mSavedVolumeControlStream;
    private AudioSwitch mAudioSwitch;
    private LocalVideoTrack mScreenVideoTrack;

    private ScreenCapturer mScreenCapturer;
    private final ScreenCapturer.Listener screenCapturerListener =
            new ScreenCapturer.Listener() {
                @Override
                public void onScreenCaptureError(@NonNull String errorDescription) {
                    Log.e(TAG, "onScreenCaptureError:"+errorDescription );
                    stopScreenCapture();
                    Snackbar.make(
                            mPrimaryVideoView,
                            R.string.screen_capture_error,
                            Snackbar.LENGTH_LONG)
                            .show();
                }

                @Override
                public void onFirstFrameAvailable() {
                    Log.d(TAG, "onFirstFrameAvailable: First frame from screen capturer available");
                }
            };
    private ParticipantController mParticipantController;

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
//        TextView
//        mIncludeVideoView = mFragmentRoomBinding.includeVideoView;
//        mPrimaryVideoView = mIncludeVideoView.participantVideo;
//        mPrimaryParticipantIdentity = mIncludeVideoView.participantVideoIdentity;
//        mSelectedParticipantIdentity = mIncludeVideoView.participantSelectedIdentity;
//        mPrimaryParticipantStubImage = mIncludeVideoView.participantStubImage;
//        mSwitchCameraButton = mFragmentRoomBinding.switchCamera;
//        mToggleVideoButton = mFragmentRoomBinding.disableCamera;
//        mToggleMicButton = mFragmentRoomBinding.disableMic;
//        mVideoCallButton = mFragmentRoomBinding.videoCall;
//        mReconnectingProgressBar = mIncludeVideoView.reconnectingProgressBar;
//        mParticipantThumbNailView = mFragmentRoomBinding.participantVideo;

        mContext = getContext();

        mDisconnectedFromOnDestroy = false;
        mAudioSwitch = new AudioSwitch(mContext);
        mSavedVolumeControlStream = requireActivity().getVolumeControlStream();


        View view = mFragmentRoomBinding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(mFragmentRoomBinding.toolbar);

        initializeUI();
        // Inflate the layout for this fragment
        return view;
    }

    private void initializeUI() {
//        mVideoCallButton.setImageDrawable(ContextCompat.getDrawable(mContext,
//                R.drawable.ic_baseline_video_call_24));
//        mVideoCallButton.setVisibility(View.VISIBLE);
//        mVideoCallButton.setOnClickListener(connectVideoCallClickListener());
//        mSwitchCameraButton.setVisibility(View.VISIBLE);
//        mSwitchCameraButton.setOnClickListener(switchCameraClickListener());
//        mToggleVideoButton.setVisibility(View.VISIBLE);
//        mToggleVideoButton.setOnClickListener(toggleVideoClickListener());
//        mToggleMicButton.setVisibility(View.VISIBLE);
//        mToggleMicButton.setOnClickListener(toggleMicClickListener());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRoomFragmentViewModel = new ViewModelProvider(this).get(RoomFragmentViewModel.class);

        checkCameraMicPermission();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        if (mLocalVideoTrack == null && mRoomFragmentViewModel.checkPermissionForCameraAndMicrophone()) {
            mLocalVideoTrack = LocalVideoTrack.create(mContext,
                    true,
                    mCameraCapturerCompat.getVideoCapturer(),
                    LOCAL_VIDEO_TRACK_NAME);
            mLocalVideoTrack.addRenderer(mLocalVideoView);


            if (mLocalParticipant != null) {
                mLocalParticipant.publishTrack(mLocalVideoTrack);
            }
        }

        if (mRoom != null) {
            mReconnectingProgressBar.setVisibility((mRoom.getState() != Room.State.RECONNECTING) ?
                    View.GONE :
                    View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        if (mLocalVideoTrack != null) {
            if (mLocalParticipant != null) {
                mLocalParticipant.unpublishTrack(mLocalVideoTrack);
            }
            mLocalVideoTrack.release();
            mLocalVideoTrack = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {

        mAudioSwitch.stop();
        getActivity().setVolumeControlStream(mSavedVolumeControlStream);

        if (mRoom != null && mRoom.getState() != Room.State.DISCONNECTED) {
            mRoom.disconnect();
            mDisconnectedFromOnDestroy = true;
        }

        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.release();
            mLocalAudioTrack = null;
        }
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.release();
            mLocalVideoTrack = null;
        }
        super.onDestroy();
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
//        mPrimaryVideoView.setMirror(true);
//        mLocalVideoTrack.addRenderer(mPrimaryVideoView);
  //      mLocalVideoView = mPrimaryVideoView;

     // mPrimaryParticipantStubImage.setVisibility(View.INVISIBLE);
      //  mPrimaryParticipantIdentity.setVisibility(View.INVISIBLE);
     //   mSelectedParticipantIdentity.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mScreenCaptureMenuItem = menu.findItem(R.id.share_screen_menu_item);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.share_screen_menu_item:
                String shareScreen = getString(R.string.share_screen);

                if (item.getTitle().equals(shareScreen)) {
                    if (mScreenCapturer == null) {
                        requestScreenCapturePermission();
                    } else {
                        startScreenCapture();
                    }
                } else {
                    stopScreenCapture();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestScreenCapturePermission() {
        Log.d(TAG, "Requesting permission to capture screen");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // This initiates a prompt dialog for the user to confirm screen projection.
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                Toast.makeText(mContext, R.string.screen_capture_permission_not_granted,
                        Toast.LENGTH_LONG).show();
                return;
            }
            mScreenCapturer = new ScreenCapturer(mContext, resultCode, data, screenCapturerListener);
            startScreenCapture();
        }
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

    private DialogInterface.OnClickListener connectClickListener(EditText userNameEditText, EditText roomNameEditText) {
        return (dialog, which) -> {
            mUserName = userNameEditText.getText().toString();
            mRoomName = roomNameEditText.getText().toString();
            mRoomFragmentViewModel.fetchAccessToken(mUserName, mRoomName);
            mReconnectingProgressBar.setVisibility(View.VISIBLE);
            observerLiveData();
        };
    }

    private void observerLiveData() {
        mRoomFragmentViewModel.mAccessTokenMutableLiveData.observe(getViewLifecycleOwner(), new Observer<AccessTokenState>() {
            @Override
            public void onChanged(AccessTokenState accessTokenState) {
                if (accessTokenState.getAccessToken() != null && mRoomName != null) {
                    connectToRoom(mRoomName, accessTokenState.getAccessToken());
                    mReconnectingProgressBar.setVisibility(View.INVISIBLE);
                } else {
                    handleError(accessTokenState.getThrowable());
                }
            }
        });
    }

    private void handleError(Throwable throwable) {
        Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void connectToRoom(String roomName, String accessToken) {
      //  mAudioSwitch.activate();
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
        setDisconnectVideoCallAction();
    }

    private void setDisconnectVideoCallAction() {
        mVideoCallButton.setImageDrawable(ContextCompat.getDrawable(mContext,
                R.drawable.ic_call_end_white_24px));
        mVideoCallButton.setVisibility(View.VISIBLE);
        mVideoCallButton.setOnClickListener(disconnectClickListener());
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            if (mRoom != null) {
                mRoom.disconnect();
            }
            initializeUI();
        };
    }

    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                mLocalParticipant = room.getLocalParticipant();
                // setTitle(room.getName());
                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    addRemoteParticipant(remoteParticipant);
                    break;
                }
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
                mReconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                mReconnectingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                mAudioSwitch.deactivate();
                initializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                mLocalParticipant = null;
                mReconnectingProgressBar.setVisibility(View.GONE);
                mRoom = null;

                if (!mDisconnectedFromOnDestroy) {
//                    mAudioSwitch.deactivate();
                    initializeUI();
                    moveLocalVideoToPrimaryView();
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
                addRemoteParticipant(remoteParticipant);

            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
                removeRemoteParticipant(remoteParticipant);
            }

            @Override
            public void onRecordingStarted(Room room) {

                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {

                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
            return;
        }
        mRemoteParticipantIdentity = remoteParticipant.getIdentity();

        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                addRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        remoteParticipant.setListener(remoteParticipantListener());
    }

    @SuppressLint("SetTextI18n")
    private RemoteParticipant.Listener remoteParticipantListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackPublished(RemoteParticipant remoteParticipant,
                                             RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackUnpublished(RemoteParticipant remoteParticipant,
                                               RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteAudioTrackPublication remoteAudioTrackPublication,
                                               RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                 RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onAudioTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onDataTrackSubscribed(RemoteParticipant remoteParticipant,
                                              RemoteDataTrackPublication remoteDataTrackPublication,
                                              RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                RemoteDataTrackPublication remoteDataTrackPublication,
                                                RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                      RemoteDataTrackPublication remoteDataTrackPublication,
                                                      TwilioException twilioException) {
                Log.i(TAG, String.format("onDataTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onVideoTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteVideoTrackPublication remoteVideoTrackPublication,
                                               RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                addRemoteParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                 RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                removeParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onVideoTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
                Snackbar.make(mVideoCallButton,
                        String.format("Failed to subscribe to %s video track",
                                remoteParticipant.getIdentity()),
                        Snackbar.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onAudioTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteAudioTrackPublication remoteAudioTrackPublication) {
            }

            @Override
            public void onAudioTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteAudioTrackPublication remoteAudioTrackPublication) {
            }

            @Override
            public void onVideoTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteVideoTrackPublication remoteVideoTrackPublication) {
            }

            @Override
            public void onVideoTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteVideoTrackPublication remoteVideoTrackPublication) {
            }
        };
    }

    private void addRemoteParticipantVideo(VideoTrack videoTrack) {
        moveLocalVideoToThumbnailView();
        mPrimaryVideoView.setMirror(false);
        videoTrack.addRenderer(mPrimaryVideoView);
    }

    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (!remoteParticipant.getIdentity().equals(mRemoteParticipantIdentity)) {
            return;
        }

        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
        moveLocalVideoToPrimaryView();
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeRenderer(mPrimaryVideoView);
    }

    private void moveLocalVideoToThumbnailView() {
        if (mParticipantThumbNailView.getVisibility() == View.GONE) {
            mParticipantThumbNailView.setVisibility(View.VISIBLE);
            mLocalVideoTrack.removeRenderer(mPrimaryVideoView);
            mLocalVideoTrack.addRenderer(mParticipantThumbNailView);
            mLocalVideoView = mParticipantThumbNailView;
            mParticipantThumbNailView.setMirror(mCameraCapturerCompat.getCameraSource() ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
    }

    private void moveLocalVideoToPrimaryView() {
        if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
            mParticipantThumbNailView.setVisibility(View.GONE);
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.removeRenderer(mParticipantThumbNailView);
                mLocalVideoTrack.addRenderer(mPrimaryVideoView);
            }
            mLocalVideoView = mPrimaryVideoView;
            mPrimaryVideoView.setMirror(mCameraCapturerCompat.getCameraSource() ==
                    CameraCapturer.CameraSource.FRONT_CAMERA);
        }
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
                if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
                    mParticipantThumbNailView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
                } else {
                    mPrimaryVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
                }
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

    private void startScreenCapture() {
        mScreenVideoTrack = LocalVideoTrack.create(mContext, true, mScreenCapturer);

        if (mScreenVideoTrack != null) {
            mScreenCaptureMenuItem.setIcon(R.drawable.ic_stop_screen_share_white_24dp);
            mScreenCaptureMenuItem.setTitle(R.string.stop_screen_share);

            if (mLocalParticipant != null) {
                mLocalParticipant.publishTrack(mScreenVideoTrack);
            }
        } else {
            Snackbar.make(
                    mPrimaryVideoView,
                    R.string.failed_to_add_screen_video_track,
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .show();
        }
    }

    private void stopScreenCapture() {
        if (mScreenVideoTrack != null) {
            if (mLocalParticipant != null) {
                mLocalParticipant.unpublishTrack(mScreenVideoTrack);
            }
            mScreenVideoTrack.release();
            mScreenVideoTrack = null;
            mScreenCaptureMenuItem.setIcon(R.drawable.ic_screen_share_white_24dp);
            mScreenCaptureMenuItem.setTitle(R.string.share_screen);
        }
    }


//    private void addParticipant(RemoteParticipant remoteParticipant, boolean renderAsPrimary) {
//        boolean muted =
//                remoteParticipant.getRemoteAudioTracks().size() <= 0
//                        || !remoteParticipant.getRemoteAudioTracks().get(0).isTrackEnabled();
//        List<RemoteVideoTrackPublication> remoteVideoTrackPublications =
//                remoteParticipant.getRemoteVideoTracks();
//
//        if (remoteVideoTrackPublications.isEmpty()) {
//            addParticipantVideoTrack(remoteParticipant, muted, null, renderAsPrimary);
//        } else {
//            for (RemoteVideoTrackPublication remoteVideoTrackPublication :
//                    remoteVideoTrackPublications) {
//                addParticipantVideoTrack(
//                        remoteParticipant,
//                        muted,
//                        remoteVideoTrackPublication.getRemoteVideoTrack(),
//                        renderAsPrimary);
//                renderAsPrimary = false;
//            }
//        }
//    }

//    private void addParticipantVideoTrack(
//            RemoteParticipant remoteParticipant,
//            boolean muted,
//            RemoteVideoTrack remoteVideoTrack,
//            boolean renderAsPrimary) {
//        if (renderAsPrimary) {
//            ParticipantPrimaryView primaryView = mParticipantController.getPrimaryView();
//
//            renderItemAsPrimary(
//                    new ParticipantController.Item(
//                            remoteParticipant.getSid(),
//                            remoteParticipant.getIdentity(),
//                            remoteVideoTrack,
//                            muted,
//                            false));
//            RemoteParticipantListener listener =
//                    new RemoteParticipantListener(primaryView, remoteParticipant.getSid());
//            remoteParticipant.setListener(listener);
//        } else {
//            mParticipantController.addThumb(
//                    remoteParticipant.getSid(),
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrack,
//                    muted,
//                    false);
//
//            RemoteParticipantListener listener =
//                    new RemoteParticipantListener(
//                            mParticipantController.getThumb(
//                                    remoteParticipant.getSid(), remoteVideoTrack),
//                            remoteParticipant.getSid());
//            remoteParticipant.setListener(listener);
//        }
//    }
//
//    /**
//     * Sets new item to render as primary view and moves existing primary view item to thumbs view.
//     *
//     * @param item New item to be rendered in primary view
//     */
//    private void renderItemAsPrimary(ParticipantController.Item item) {
//        // nothing to click while not in room
//        if (room == null) return;
//
//        // no need to renderer if same item clicked
//        ParticipantController.Item old = mParticipantController.getPrimaryItem();
//        if (old != null && item.sid.equals(old.sid) && item.videoTrack == old.videoTrack) return;
//
//        // add back old participant to thumbs
//        if (old != null) {
//
//            if (old.sid.equals(localParticipantSid)) {
//
//                // toggle local participant state
//                int state =
//                        old.videoTrack == null
//                                ? ParticipantView.State.NO_VIDEO
//                                : ParticipantView.State.VIDEO;
//                mParticipantController.updateThumb(old.sid, old.videoTrack, state);
//                mParticipantController.updateThumb(old.sid, old.videoTrack, old.mirror);
//
//            } else {
//
//                // add thumb for remote participant
//                RemoteParticipant remoteParticipant = getRemoteParticipant(old);
//                if (remoteParticipant != null) {
//                    mParticipantController.addThumb(
//                            old.sid, old.identity, old.videoTrack, old.muted, old.mirror);
//                    RemoteParticipantListener listener =
//                            new RemoteParticipantListener(
//                                    mParticipantController.getThumb(old.sid, old.videoTrack),
//                                    remoteParticipant.getSid());
//                    remoteParticipant.setListener(listener);
//                }
//            }
//        }
//
//        // handle new primary participant click
//        mParticipantController.renderAsPrimary(item);
//
//        RemoteParticipant remoteParticipant = getRemoteParticipant(item);
//        if (remoteParticipant != null) {
//            ParticipantPrimaryView primaryView = mParticipantController.getPrimaryView();
//            RemoteParticipantListener listener =
//                    new RemoteParticipantListener(primaryView, remoteParticipant.getSid());
//            remoteParticipant.setListener(listener);
//        }
//
//        if (item.sid.equals(localParticipantSid)) {
//
//            // toggle local participant state and hide his badge
//            mParticipantController.updateThumb(
//                    item.sid, item.videoTrack, ParticipantView.State.SELECTED);
//            mParticipantController.getPrimaryView().showIdentityBadge(false);
//        } else {
//
//            // remove remote participant thumb
//            mParticipantController.removeThumb(item);
//        }
//    }
//
//    private @Nullable RemoteParticipant getRemoteParticipant(ParticipantController.Item item) {
//        RemoteParticipant remoteParticipant = null;
//
//        for (RemoteParticipant temp : room.getRemoteParticipants()) {
//            if (temp.getSid().equals(item.sid)) remoteParticipant = temp;
//        }
//
//        return remoteParticipant;
//    }
//
//    /** Removes all participant thumbs and push local camera as primary with empty sid. */
//    private void removeAllParticipants() {
//        if (room != null) {
//            mParticipantController.removeAllThumbs();
//            mParticipantController.removePrimary();
//
//            renderLocalParticipantStub();
//        }
//    }
//
//    /**
//     * Remove single remoteParticipant thumbs and all it associated thumbs. If rendered as primary
//     * remoteParticipant, primary view switches to local video track.
//     *
//     * @param remoteParticipant recently disconnected remoteParticipant.¬
//     */
//    private void removeParticipant(RemoteParticipant remoteParticipant) {
//
//        if (mParticipantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {
//
//            // render local video if primary remoteParticipant has gone
//            mParticipantController.getThumb(localParticipantSid, cameraVideoTrack).callOnClick();
//        }
//
//        mParticipantController.removeThumbs(remoteParticipant.getSid());
//    }
//
//    /**
//     * Remove the video track and mark the track to be restored when going to the settings screen or
//     * going to the background
//     */
//    private void removeCameraTrack() {
//        if (cameraVideoTrack != null) {
//            if (localParticipant != null) {
//                localParticipant.unpublishTrack(cameraVideoTrack);
//            }
//            cameraVideoTrack.release();
//            restoreLocalVideoCameraTrack = true;
//            cameraVideoTrack = null;
//        }
//    }
//
//    /** Try to restore camera video track after going to the settings screen or background */
//    private void restoreCameraTrack() {
//        if (restoreLocalVideoCameraTrack) {
//            obtainVideoConstraints();
//            setupLocalVideoTrack();
//            renderLocalParticipantStub();
//            restoreLocalVideoCameraTrack = false;
//        }
//    }
}