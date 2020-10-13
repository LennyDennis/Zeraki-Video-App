package com.lennydennis.zerakiapp.ui.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.lennydennis.zerakiapp.ui.viewmodels.RoomFragmentViewModel;
import com.lennydennis.zerakiapp.util.CameraCapturerCompat;
import com.twilio.audioswitch.AudioSwitch;
import com.twilio.video.AspectRatio;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalAudioTrackPublication;
import com.twilio.video.LocalDataTrack;
import com.twilio.video.LocalDataTrackPublication;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.LocalVideoTrackPublication;
import com.twilio.video.NetworkQualityLevel;
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
import com.twilio.video.VideoConstraints;
import com.twilio.video.VideoDimensions;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.twilio.video.AspectRatio.ASPECT_RATIO_11_9;
import static com.twilio.video.AspectRatio.ASPECT_RATIO_16_9;
import static com.twilio.video.AspectRatio.ASPECT_RATIO_4_3;

public class RoomFragment extends Fragment {

    private static final String TAG = "RoomFragment";

    private static final String LOCAL_AUDIO_TRACK_NAME = "microphone";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";
    private static final String SCREEN_TRACK_NAME = "screen";

    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final String IS_AUDIO_MUTED = "IS_AUDIO_MUTED";
    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 101;
    private static final int STATS_DELAY = 1000; // milliseconds
    private static final String LOCAL_PARTICIPANT_STUB_SID = "";

    private FragmentRoomBinding mFragmentRoomBinding;
    private RoomFragmentViewModel mRoomFragmentViewModel;
    private Context mContext;
    private ParticipantPrimaryView mPrimaryVideoView;
    private LocalAudioTrack mLocalAudioTrack;
    private CameraCapturerCompat mCameraCapturerCompat;
    private LocalVideoTrack mLocalVideoTrack;
    private VideoRenderer mLocalVideoView;
    private ImageButton mLocalVideoButton;
    private ImageButton mLocalMicButton;
    private ImageButton mVideoCallButton;
    private AlertDialog mConnectDialog;
    private Room mRoom;
    private LocalParticipant mLocalParticipant;
    private String mUserName;
    private String mRoomName;
    private MenuItem mScreenCaptureMenuItem;
    private String mRemoteParticipantIdentity;
    private Boolean mDisconnectedFromOnDestroy;
    private int mSavedVolumeControlStream;
    private AudioSwitch mAudioSwitch;
    private LocalVideoTrack mScreenVideoTrack;
    private ParticipantController mParticipantController;
    private ImageButton mEndCallButton;
    private com.lennydennis.zerakiapp.databinding.ContentRoomBinding mIncludePrimaryView;
    private LinearLayout mThumbNailLinearLayout;
    private FrameLayout mPrimaryVideoContainer;
    private ConstraintLayout mJoinMessageLayout;
    private TextView mJoinStatus;
    private TextView mJoinRoomName;

    private AudioManager audioManager;
    private int savedAudioMode = AudioManager.MODE_INVALID;
    private int savedVolumeControlStream;
    private boolean savedIsMicrophoneMute = false;
    private boolean savedIsSpeakerPhoneOn = false;

    private ScreenCapturer mScreenCapturer;
    private final ScreenCapturer.Listener screenCapturerListener =
            new ScreenCapturer.Listener() {
                @Override
                public void onScreenCaptureError(@NonNull String errorDescription) {
                    Log.e(TAG, "onScreenCaptureError:" + errorDescription);
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

    private AspectRatio[] aspectRatios =
            new AspectRatio[] {ASPECT_RATIO_4_3, ASPECT_RATIO_16_9, ASPECT_RATIO_11_9};

    private VideoDimensions[] videoDimensions =
            new VideoDimensions[] {
                    VideoDimensions.CIF_VIDEO_DIMENSIONS,
                    VideoDimensions.VGA_VIDEO_DIMENSIONS,
                    VideoDimensions.WVGA_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_540P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_720P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_960P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_S1080P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_1080P_VIDEO_DIMENSIONS
            };

    private Map<String, String> localVideoTrackNames = new HashMap<>();
    private Map<String, NetworkQualityLevel> networkQualityLevels = new HashMap<>();
    private ParticipantController participantController;
    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = false;
    private String mLocalParticipantSid = LOCAL_PARTICIPANT_STUB_SID;

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

        mVideoCallButton = mFragmentRoomBinding.videoCall;
        mEndCallButton = mFragmentRoomBinding.disconnect;
        mLocalVideoButton = mFragmentRoomBinding.localVideoButton;
        mLocalMicButton = mFragmentRoomBinding.localMicButton;
        mJoinMessageLayout = mFragmentRoomBinding.joinStatusLayout;
        mJoinStatus = mFragmentRoomBinding.joinStatus;
        mJoinRoomName = mFragmentRoomBinding.joinRoomName;

        mIncludePrimaryView = mFragmentRoomBinding.includePrimaryView;
        mPrimaryVideoView = mIncludePrimaryView.primaryVideo;
        mThumbNailLinearLayout = mIncludePrimaryView.remoteVideoThumbnails;
        mPrimaryVideoContainer = mIncludePrimaryView.videoContainer;

        mContext = getContext();

        if (savedInstanceState != null) {
            isAudioMuted = savedInstanceState.getBoolean(IS_AUDIO_MUTED);
            isVideoMuted = savedInstanceState.getBoolean(IS_VIDEO_MUTED);
        }

        //mDisconnectedFromOnDestroy = false;

        audioManager = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        mSavedVolumeControlStream = requireActivity().getVolumeControlStream();

        participantController = new ParticipantController(mThumbNailLinearLayout, mPrimaryVideoView);
       // participantController.setListener(participantClickListener());

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
//        mLocalVideoButton.setVisibility(View.VISIBLE);
//        mLocalVideoButton.setOnClickListener(toggleVideoClickListener());
//        mLocalMicButton.setVisibility(View.VISIBLE);
//        mLocalMicButton.setOnClickListener(toggleMicClickListener());
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

//        if (mRoom != null) {
//            mReconnectingProgressBar.setVisibility((mRoom.getState() != Room.State.RECONNECTING) ?
//                    View.GONE :
//                    View.VISIBLE);
//        }
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

    private void obtainVideoConstraints() {
        Timber.d("Collecting video constraints...");

        VideoConstraints.Builder builder = new VideoConstraints.Builder();

        // setup aspect ratio
        String aspectRatio = sharedPreferences.getString(Preferences.ASPECT_RATIO, "0");
        if (aspectRatio != null) {
            int aspectRatioIndex = Integer.parseInt(aspectRatio);
            builder.aspectRatio(aspectRatios[aspectRatioIndex]);
            Timber.d(
                    "Aspect ratio : %s",
                    getResources()
                            .getStringArray(R.array.settings_screen_aspect_ratio_array)[
                            aspectRatioIndex]);
        }

        // setup video dimensions
        int minVideoDim = sharedPreferences.getInt(Preferences.MIN_VIDEO_DIMENSIONS, 0);
        int maxVideoDim =
                sharedPreferences.getInt(
                        Preferences.MAX_VIDEO_DIMENSIONS, videoDimensions.length - 1);

        if (maxVideoDim != -1 && minVideoDim != -1) {
            builder.minVideoDimensions(videoDimensions[minVideoDim]);
            builder.maxVideoDimensions(videoDimensions[maxVideoDim]);
        }

        Timber.d(
                "Video dimensions: %s - %s",
                getResources()
                        .getStringArray(R.array.settings_screen_video_dimensions_array)[
                        minVideoDim],
                getResources()
                        .getStringArray(R.array.settings_screen_video_dimensions_array)[
                        maxVideoDim]);

        // setup fps
        int minFps = sharedPreferences.getInt(Preferences.MIN_FPS, 0);
        int maxFps = sharedPreferences.getInt(Preferences.MAX_FPS, 30);

        if (maxFps != -1 && minFps != -1) {
            builder.minFps(minFps);
            builder.maxFps(maxFps);
        }

        Timber.d("Frames per second: %d - %d", minFps, maxFps);

        videoConstraints = builder.build();
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
            //mReconnectingProgressBar.setVisibility(View.VISIBLE);
            observerLiveData();
        };
    }

    private void observerLiveData() {
        mRoomFragmentViewModel.mAccessTokenMutableLiveData.observe(getViewLifecycleOwner(), new Observer<AccessTokenState>() {
            @Override
            public void onChanged(AccessTokenState accessTokenState) {
                if (accessTokenState.getAccessToken() != null && mRoomName != null) {
                    connectToRoom(mRoomName, accessTokenState.getAccessToken());
                    //mReconnectingProgressBar.setVisibility(View.INVISIBLE);
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
               // mReconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                //mReconnectingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                mAudioSwitch.deactivate();
                initializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                mLocalParticipant = null;
              //  mReconnectingProgressBar.setVisibility(View.GONE);
                mRoom = null;

                if (!mDisconnectedFromOnDestroy) {
//                    mAudioSwitch.deactivate();
                    initializeUI();
                    //moveLocalVideoToPrimaryView();
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
//        if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
//            return;
//        }
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

    private class LocalParticipantListener implements LocalParticipant.Listener {
        private ImageView networkQualityImage;

        LocalParticipantListener(ParticipantView primaryView) {
            networkQualityImage = primaryView.networkQualityLevelImg;
        }

        @Override
        public void onAudioTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalAudioTrackPublication localAudioTrackPublication) {}

        @Override
        public void onAudioTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalAudioTrack localAudioTrack,
                @NonNull TwilioException twilioException) {}

        @Override
        public void onVideoTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalVideoTrackPublication localVideoTrackPublication) {}

        @Override
        public void onVideoTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalVideoTrack localVideoTrack,
                @NonNull TwilioException twilioException) {}

        @Override
        public void onDataTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalDataTrackPublication localDataTrackPublication) {}

        @Override
        public void onDataTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalDataTrack localDataTrack,
                @NonNull TwilioException twilioException) {}

        @Override
        public void onNetworkQualityLevelChanged(
                @NonNull LocalParticipant localParticipant,
                @NonNull NetworkQualityLevel networkQualityLevel) {
            setNetworkQualityLevelImage(
                    networkQualityImage, networkQualityLevel, localParticipant.getSid());
        }
    }

//    private class RemoteParticipantListener implements RemoteParticipant.Listener {
//
//        private ImageView networkQualityImage;
//
//        RemoteParticipantListener(ParticipantView primaryView, String sid) {
//            networkQualityImage = primaryView.networkQualityLevelImg;
//            setNetworkQualityLevelImage(networkQualityImage, networkQualityLevels.get(sid), sid);
//        }
//
//        @Override
//        public void onNetworkQualityLevelChanged(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull NetworkQualityLevel networkQualityLevel) {
//            setNetworkQualityLevelImage(
//                    networkQualityImage, networkQualityLevel, remoteParticipant.getSid());
//        }
//
//        @Override
//        public void onAudioTrackPublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
//            Timber.i(
//                    "onAudioTrackPublished: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled(),
//                    remoteAudioTrackPublication.isTrackSubscribed());
//
//            // TODO: Need design
//        }
//
//        @Override
//        public void onAudioTrackUnpublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
//            Timber.i(
//                    "onAudioTrackUnpublished: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled(),
//                    remoteAudioTrackPublication.isTrackSubscribed());
//            // TODO: Need design
//        }
//
//        @Override
//        public void onVideoTrackPublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
//            Timber.i(
//                    "onVideoTrackPublished: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled(),
//                    remoteVideoTrackPublication.isTrackSubscribed());
//            // TODO: Need design
//        }
//
//        @Override
//        public void onVideoTrackUnpublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
//            Timber.i(
//                    "onVideoTrackUnpublished: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled(),
//                    remoteVideoTrackPublication.isTrackSubscribed());
//            // TODO: Need design
//        }
//
//        @Override
//        public void onAudioTrackSubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
//                @NonNull RemoteAudioTrack remoteAudioTrack) {
//            Timber.i(
//                    "onAudioTrackSubscribed: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled(),
//                    remoteAudioTrackPublication.isTrackSubscribed());
//            boolean newAudioState = !remoteAudioTrackPublication.isTrackEnabled();
//
//            if (participantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {
//
//                // update audio state for primary view
//                participantController.getPrimaryItem().muted = newAudioState;
//                participantController.getPrimaryView().setMuted(newAudioState);
//
//            } else {
//
//                // update thumbs with audio state
//                participantController.updateThumbs(remoteParticipant.getSid(), newAudioState);
//            }
//        }
//
//        @Override
//        public void onAudioTrackSubscriptionFailed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
//                @NonNull TwilioException twilioException) {
//            Timber.w(
//                    "onAudioTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    twilioException.getMessage());
//            // TODO: Need design
//            Snackbar.make(primaryVideoView, "onAudioTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
//                    .show();
//        }
//
//        @Override
//        public void onAudioTrackUnsubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
//                @NonNull RemoteAudioTrack remoteAudioTrack) {
//            Timber.i(
//                    "onAudioTrackUnsubscribed: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled(),
//                    remoteAudioTrackPublication.isTrackSubscribed());
//
//            if (participantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {
//
//                // update audio state for primary view
//                participantController.getPrimaryItem().muted = true;
//                participantController.getPrimaryView().setMuted(true);
//
//            } else {
//
//                // update thumbs with audio state
//                participantController.updateThumbs(remoteParticipant.getSid(), true);
//            }
//        }
//
//        @Override
//        public void onVideoTrackSubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
//                @NonNull RemoteVideoTrack remoteVideoTrack) {
//            Timber.i(
//                    "onVideoTrackSubscribed: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled(),
//                    remoteVideoTrackPublication.isTrackSubscribed());
//
//            ParticipantController.Item primary = participantController.getPrimaryItem();
//
//            if (primary != null
//                    && primary.sid.equals(remoteParticipant.getSid())
//                    && primary.videoTrack == null) {
//                // no thumb needed - render as primary
//                primary.videoTrack = remoteVideoTrack;
//                participantController.renderAsPrimary(primary);
//            } else {
//                // not a primary remoteParticipant requires thumb
//                participantController.addOrUpdateThumb(
//                        remoteParticipant.getSid(),
//                        remoteParticipant.getIdentity(),
//                        null,
//                        remoteVideoTrack);
//            }
//        }
//
//        @Override
//        public void onVideoTrackSubscriptionFailed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
//                @NonNull TwilioException twilioException) {
//            Timber.w(
//                    "onVideoTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    twilioException.getMessage());
//            // TODO: Need design
//            Snackbar.make(primaryVideoView, "onVideoTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
//                    .show();
//        }
//
//        @Override
//        public void onVideoTrackUnsubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
//                @NonNull RemoteVideoTrack remoteVideoTrack) {
//            Timber.i(
//                    "onVideoTrackUnsubscribed: remoteParticipant: %s, video: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled());
//
//            ParticipantController.Item primary = participantController.getPrimaryItem();
//
//            if (primary != null
//                    && primary.sid.equals(remoteParticipant.getSid())
//                    && primary.videoTrack == remoteVideoTrack) {
//
//                // Remove primary video track
//                primary.videoTrack = null;
//
//                // Try to find another video track to render as primary
//                List<RemoteVideoTrackPublication> remoteVideoTracks =
//                        remoteParticipant.getRemoteVideoTracks();
//                for (RemoteVideoTrackPublication newRemoteVideoTrackPublication :
//                        remoteVideoTracks) {
//                    RemoteVideoTrack newRemoteVideoTrack =
//                            newRemoteVideoTrackPublication.getRemoteVideoTrack();
//                    if (newRemoteVideoTrack != remoteVideoTrack) {
//                        participantController.removeThumb(
//                                remoteParticipant.getSid(), newRemoteVideoTrack);
//                        primary.videoTrack = newRemoteVideoTrack;
//                        break;
//                    }
//                }
//                participantController.renderAsPrimary(primary);
//            } else {
//
//                // remove thumb or leave empty video thumb
//                participantController.removeOrEmptyThumb(
//                        remoteParticipant.getSid(),
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrack);
//            }
//        }
//
//        @Override
//        public void onDataTrackPublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {
//            Timber.i(
//                    "onDataTrackPublished: remoteParticipant: %s, data: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteDataTrackPublication.getTrackSid(),
//                    remoteDataTrackPublication.isTrackEnabled());
//        }
//
//        @Override
//        public void onDataTrackUnpublished(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {
//            Timber.i(
//                    "onDataTrackUnpublished: remoteParticipant: %s, data: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteDataTrackPublication.getTrackSid(),
//                    remoteDataTrackPublication.isTrackEnabled());
//        }
//
//        @Override
//        public void onDataTrackSubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
//                @NonNull RemoteDataTrack remoteDataTrack) {
//            Timber.i(
//                    "onDataTrackSubscribed: remoteParticipant: %s, data: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteDataTrackPublication.getTrackSid(),
//                    remoteDataTrackPublication.isTrackEnabled(),
//                    remoteDataTrackPublication.isTrackSubscribed());
//        }
//
//        @Override
//        public void onDataTrackSubscriptionFailed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
//                @NonNull TwilioException twilioException) {
//            Timber.w(
//                    "onDataTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
//                    remoteParticipant.getIdentity(),
//                    remoteDataTrackPublication.getTrackSid(),
//                    twilioException.getMessage());
//            // TODO: Need design
//            Snackbar.make(primaryVideoView, "onDataTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
//                    .show();
//        }
//
//        @Override
//        public void onDataTrackUnsubscribed(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
//                @NonNull RemoteDataTrack remoteDataTrack) {
//            Timber.i(
//                    "onDataTrackUnsubscribed: remoteParticipant: %s, data: %s, enabled: %b, subscribed: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteDataTrackPublication.getTrackSid(),
//                    remoteDataTrackPublication.isTrackEnabled(),
//                    remoteDataTrackPublication.isTrackSubscribed());
//        }
//
//        @Override
//        public void onAudioTrackEnabled(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
//            Timber.i(
//                    "onAudioTrackEnabled: remoteParticipant: %s, audio: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled());
//
//            // TODO: need design
//        }
//
//        @Override
//        public void onAudioTrackDisabled(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
//            Timber.i(
//                    "onAudioTrackDisabled: remoteParticipant: %s, audio: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteAudioTrackPublication.getTrackSid(),
//                    remoteAudioTrackPublication.isTrackEnabled());
//
//            // TODO: need design
//        }
//
//        @Override
//        public void onVideoTrackEnabled(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
//            Timber.i(
//                    "onVideoTrackEnabled: remoteParticipant: %s, video: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled());
//
//            // TODO: need design
//        }
//
//        @Override
//        public void onVideoTrackDisabled(
//                @NonNull RemoteParticipant remoteParticipant,
//                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
//            Timber.i(
//                    "onVideoTrackDisabled: remoteParticipant: %s, video: %s, enabled: %b",
//                    remoteParticipant.getIdentity(),
//                    remoteVideoTrackPublication.getTrackSid(),
//                    remoteVideoTrackPublication.isTrackEnabled());
//
//            // TODO: need design
//        }
//    }

    private void setNetworkQualityLevelImage(
            ImageView networkQualityImage, NetworkQualityLevel networkQualityLevel, String sid) {

        networkQualityLevels.put(sid, networkQualityLevel);
        if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_UNKNOWN) {
            networkQualityImage.setVisibility(View.GONE);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ZERO) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_0);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_ONE) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_1);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_TWO) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_2);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_THREE) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_3);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FOUR) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_4);
        } else if (networkQualityLevel == NetworkQualityLevel.NETWORK_QUALITY_LEVEL_FIVE) {
            networkQualityImage.setVisibility(View.VISIBLE);
            networkQualityImage.setImageResource(R.drawable.network_quality_level_5);
        }
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
        //moveLocalVideoToThumbnailView();
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
       // moveLocalVideoToPrimaryView();
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeRenderer(mPrimaryVideoView);
    }

//    private void moveLocalVideoToThumbnailView() {
//        if (mParticipantThumbNailView.getVisibility() == View.GONE) {
//            mParticipantThumbNailView.setVisibility(View.VISIBLE);
//            mLocalVideoTrack.removeRenderer(mPrimaryVideoView);
//            mLocalVideoTrack.addRenderer(mParticipantThumbNailView);
//            mLocalVideoView = mParticipantThumbNailView;
//            mParticipantThumbNailView.setMirror(mCameraCapturerCompat.getCameraSource() ==
//                    CameraCapturer.CameraSource.FRONT_CAMERA);
//        }
//    }
//
//    private void moveLocalVideoToPrimaryView() {
//        if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
//            mParticipantThumbNailView.setVisibility(View.GONE);
//            if (mLocalVideoTrack != null) {
//                mLocalVideoTrack.removeRenderer(mParticipantThumbNailView);
//                mLocalVideoTrack.addRenderer(mPrimaryVideoView);
//            }
//            mLocalVideoView = mPrimaryVideoView;
//            mPrimaryVideoView.setMirror(mCameraCapturerCompat.getCameraSource() ==
//                    CameraCapturer.CameraSource.FRONT_CAMERA);
//        }
//    }

    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
        return (dialog, which) -> {
            initializeUI();
            mConnectDialog.dismiss();
        };
    }

//    private View.OnClickListener switchCameraClickListener() {
//        return v -> {
//            if (mCameraCapturerCompat != null) {
//                CameraCapturer.CameraSource cameraSource = mCameraCapturerCompat.getCameraSource();
//                mCameraCapturerCompat.switchCamera();
//                if (mParticipantThumbNailView.getVisibility() == View.VISIBLE) {
//                    mParticipantThumbNailView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
//                } else {
//                    mPrimaryVideoView.setMirror(cameraSource == CameraCapturer.CameraSource.BACK_CAMERA);
//                }
//            }
//        };
//    }
//
//    private View.OnClickListener toggleVideoClickListener() {
//        return v -> {
//            if (mLocalVideoTrack != null) {
//                boolean enable = !mLocalVideoTrack.isEnabled();
//                mLocalVideoTrack.enable(enable);
//                int icon;
//                if (enable) {
//                    icon = R.drawable.ic_videocam_white_24px;
//                    mSwitchCameraButton.setEnabled(true);
//                    mPrimaryParticipantStubImage.setVisibility(View.INVISIBLE);
//                } else {
//                    icon = R.drawable.ic_baseline_videocam_off_24;
//                    mSwitchCameraButton.setEnabled(false);
//                    mPrimaryParticipantStubImage.setVisibility(View.VISIBLE);
//                }
//                mLocalVideoButton.setImageDrawable(
//                        ContextCompat.getDrawable(mContext, icon));
//            }
//        };
//    }

    private View.OnClickListener toggleMicClickListener() {
        return v -> {
            if (mLocalAudioTrack != null) {
                boolean enable = !mLocalAudioTrack.isEnabled();
                mLocalAudioTrack.enable(enable);
                int icon = enable ?
                        R.drawable.ic_mic_white_24px : R.drawable.ic_baseline_mic_off_24;
                mLocalMicButton.setImageDrawable(ContextCompat.getDrawable(
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

//    private void renderItemAsPrimary(ParticipantController.Item item) {
//        // nothing to click while not in room
//        if (mRoom == null) return;
//
//        // no need to renderer if same item clicked
//        ParticipantController.Item old = participantController.getPrimaryItem();
//        if (old != null && item.sid.equals(old.sid) && item.videoTrack == old.videoTrack) return;
//
//        // add back old participant to thumbs
//        if (old != null) {
//
//            if (old.sid.equals(mLocalParticipantSid)) {
//
//                // toggle local participant state
//                int state =
//                        old.videoTrack == null
//                                ? ParticipantView.State.NO_VIDEO
//                                : ParticipantView.State.VIDEO;
//                participantController.updateThumb(old.sid, old.videoTrack, state);
//                participantController.updateThumb(old.sid, old.videoTrack, old.mirror);
//
//            } else {
//
//                // add thumb for remote participant
//                RemoteParticipant remoteParticipant = getRemoteParticipant(old);
//                if (remoteParticipant != null) {
//                    participantController.addThumb(
//                            old.sid, old.identity, old.videoTrack, old.muted, old.mirror);
//                    RemoteParticipantListener listener =
//                            new RemoteParticipantListener(
//                                    participantController.getThumb(old.sid, old.videoTrack),
//                                    remoteParticipant.getSid());
//                    remoteParticipant.setListener(listener);
//                }
//            }
//        }
//
//        // handle new primary participant click
//        participantController.renderAsPrimary(item);
//
//        RemoteParticipant remoteParticipant = getRemoteParticipant(item);
//        if (remoteParticipant != null) {
//            ParticipantPrimaryView primaryView = participantController.getPrimaryView();
//            RemoteParticipantListener listener =
//                    new RemoteParticipantListener(primaryView, remoteParticipant.getSid());
//            remoteParticipant.setListener(listener);
//        }
//
//        if (item.sid.equals(mLocalParticipantSid)) {
//
//            // toggle local participant state and hide his badge
//            participantController.updateThumb(
//                    item.sid, item.videoTrack, ParticipantView.State.SELECTED);
//            participantController.getPrimaryView().showIdentityBadge(false);
//        } else {
//
//            // remove remote participant thumb
//            participantController.removeThumb(item);
//        }
//    }


//    private ParticipantController.ItemClickListener participantClickListener() {
//        return this::renderItemAsPrimary;
//    }
}