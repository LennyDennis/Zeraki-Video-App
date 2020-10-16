 
/*
 * Copyright (C) 2019 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lennydennis.zerakiapp.ui.views;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.lennydennis.zerakiapp.R;
import com.lennydennis.zerakiapp.databinding.FragmentRoomBinding;
import com.lennydennis.zerakiapp.model.AccessTokenState;
import com.lennydennis.zerakiapp.model.PeerToPeerRoomState;
import com.lennydennis.zerakiapp.ui.dialog.RoomDialog;
import com.lennydennis.zerakiapp.ui.rooms.RoomEvent;
import com.lennydennis.zerakiapp.ui.viewmodels.RoomFragmentViewModel;
import com.lennydennis.zerakiapp.ui.viewmodels.RoomFragmentViewModel.RoomViewModelFactory;
import com.lennydennis.zerakiapp.util.CameraCapturerCompat;
import com.twilio.video.AspectRatio;
import com.twilio.video.CameraCapturer;
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
import com.twilio.video.VideoDimensions;
import com.twilio.video.VideoTrack;
import com.twilio.video.app.ui.room.RoomManager;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import kotlinx.coroutines.Dispatchers;
import timber.log.Timber;

import static com.twilio.video.AspectRatio.ASPECT_RATIO_11_9;
import static com.twilio.video.AspectRatio.ASPECT_RATIO_16_9;
import static com.twilio.video.AspectRatio.ASPECT_RATIO_4_3;
import static com.twilio.video.Room.State.CONNECTED;
import static kotlinx.coroutines.CoroutineScopeKt.CoroutineScope;

public class RoomFragment extends Fragment implements RoomDialog.RoomDialogListener {

    private static final String TAG = "RoomFragment";

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 101;
    private static final String MICROPHONE_TRACK_NAME = "microphone";
    private static final String CAMERA_TRACK_NAME = "camera";
    private static final String SCREEN_TRACK_NAME = "screen";
    private static final String IS_AUDIO_MUTED = "IS_AUDIO_MUTED";
    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";

    private static final String LOCAL_PARTICIPANT_STUB_SID = "";

    private AspectRatio[] aspectRatios =
            new AspectRatio[]{ASPECT_RATIO_4_3, ASPECT_RATIO_16_9, ASPECT_RATIO_11_9};

    private VideoDimensions[] videoDimensions =
            new VideoDimensions[]{
                    VideoDimensions.CIF_VIDEO_DIMENSIONS,
                    VideoDimensions.VGA_VIDEO_DIMENSIONS,
                    VideoDimensions.WVGA_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_540P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_720P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_960P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_S1080P_VIDEO_DIMENSIONS,
                    VideoDimensions.HD_1080P_VIDEO_DIMENSIONS
            };

    private FragmentRoomBinding mFragmentRoomBinding;
    private RoomFragmentViewModel mRoomFragmentViewModel;
    private Context mContext;
    private ParticipantPrimaryView mPrimaryVideoView;
    private ImageButton mLocalVideoButton;
    private ImageButton mLocalMicButton;
    private ImageButton mVideoCallButton;
    private String mUserName;
    private String mRoomName;
    private ImageButton mEndCallButton;
    private com.lennydennis.zerakiapp.databinding.ContentRoomBinding mIncludePrimaryView;
    private LinearLayout mThumbNailLinearLayout;
    //    private FrameLayout mPrimaryVideoContainer;
    private ConstraintLayout mJoinMessageLayout;
    private TextView mJoinStatus;
    private TextView mJoinRoomName;
    private String roomName;

    private MenuItem screenCaptureMenuItem;

    private AudioManager audioManager;
    private int savedAudioMode = AudioManager.MODE_NORMAL;
    private boolean savedIsMicrophoneMute = false;
    private boolean savedIsSpeakerPhoneOn = false;

    private String displayName;
    private LocalParticipant localParticipant;
    private String localParticipantSid = LOCAL_PARTICIPANT_STUB_SID;
    private Room room;
    private LocalAudioTrack mLocalAudioTrack;
    private LocalVideoTrack mCameraVideoTrack;
    private boolean restoreLocalVideoCameraTrack = false;
    private LocalVideoTrack mScreenVideoTrack;
    private CameraCapturerCompat mCapturerCompat;
    private ScreenCapturer mScreenCapturer;
    private final ScreenCapturer.Listener screenCapturerListener =
            new ScreenCapturer.Listener() {
                @Override
                public void onScreenCaptureError(@NonNull String errorDescription) {
                    Log.e(TAG, "onScreenCaptureError: " + errorDescription);
                    stopScreenCapture();
                    Snackbar.make(
                            mPrimaryVideoView,
                            R.string.screen_capture_error,
                            Snackbar.LENGTH_LONG)
                            .show();
                }

                @Override
                public void onFirstFrameAvailable() {
                    Log.e(TAG, "First frame from screen capturer available");
                }
            };

    private Map<String, String> localVideoTrackNames = new HashMap<>();
    // TODO This should be decoupled from this Activity as part of
    // https://issues.corp.twilio.com/browse/AHOYAPPS-473
    private Map<String, NetworkQualityLevel> networkQualityLevels = new HashMap<>();

    /**
     * Coordinates participant thumbs and primary participant rendering.
     */
    private ParticipantController participantController;

    //    /** Disposes {@link VideoAppService} requests when activity is destroyed. */
    private final CompositeDisposable rxDisposables = new CompositeDisposable();

    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = false;
    private String mRoomType;
    private ImageButton mSwitchCameraButton;
    private RoomManager mRoomManager;
    private ProgressBar mJoiningRoomProgressBar;

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
        mSwitchCameraButton = mFragmentRoomBinding.switchCamera;

        mJoinMessageLayout = mFragmentRoomBinding.joinStatusLayout;
        mJoinStatus = mFragmentRoomBinding.joinStatus;
        mJoinRoomName = mFragmentRoomBinding.joinRoomName;
        mJoiningRoomProgressBar = mFragmentRoomBinding.joinRoomProgressBar;

        mIncludePrimaryView = mFragmentRoomBinding.includePrimaryView;
        mPrimaryVideoView = mIncludePrimaryView.primaryVideo;
        mThumbNailLinearLayout = mIncludePrimaryView.remoteVideoThumbnails;
        //  mPrimaryVideoContainer = mIncludePrimaryView.videoContainer;

        mContext = getContext();

        if (savedInstanceState != null) {
            isAudioMuted = savedInstanceState.getBoolean(IS_AUDIO_MUTED);
            isVideoMuted = savedInstanceState.getBoolean(IS_VIDEO_MUTED);
        }

        audioManager = (AudioManager) requireActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
//        mSavedVolumeControlStream = requireActivity().getVolumeControlStream();

        participantController = new ParticipantController(mThumbNailLinearLayout, mPrimaryVideoView);
        participantController.setListener(participantClickListener());

        View view = mFragmentRoomBinding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(mFragmentRoomBinding.toolbar);

        initializeUI();

        // Inflate the layout for this fragment
        return view;
    }

    private void initializeUI() {

        mVideoCallButton.setVisibility(View.VISIBLE);
        mVideoCallButton.setOnClickListener(connectVideoCallClickListener());
        mLocalVideoButton.setVisibility(View.VISIBLE);
        mLocalVideoButton.setOnClickListener(toggleVideoClickListener());
        mLocalMicButton.setVisibility(View.VISIBLE);
        mLocalMicButton.setOnClickListener(toggleMicClickListener());

        mSwitchCameraButton.setOnClickListener(switchCameraClickListener());

        mEndCallButton.setOnClickListener(disconnectCallClickListener());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRoomManager = new RoomManager(mContext, CoroutineScope(Dispatchers.getIO()));
        RoomViewModelFactory factory = new RoomViewModelFactory(mRoomManager);
        mRoomFragmentViewModel = new ViewModelProvider(this, factory).get(RoomFragmentViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();

        restoreCameraTrack();

        publishLocalTracks();

        addParticipantViews();
    }

    @Override
    public void onResume() {
        super.onResume();
//        displayName = sharedPreferences.getString(Preferences.DISPLAY_NAME, null);
//        setTitle(displayName);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(IS_AUDIO_MUTED, isAudioMuted);
        outState.putBoolean(IS_VIDEO_MUTED, isVideoMuted);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        // Reset the speakerphone
        audioManager.setSpeakerphoneOn(false);
        // Teardown tracks
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.release();
            mLocalAudioTrack = null;
        }
        if (mCameraVideoTrack != null) {
            mCameraVideoTrack.release();
            mCameraVideoTrack = null;
        }
        if (mScreenVideoTrack != null) {
            mScreenVideoTrack.release();
            mScreenVideoTrack = null;
        }
        // dispose any token requests if needed
        rxDisposables.clear();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean recordAudioPermissionGranted =
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean cameraPermissionGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            boolean permissionsGranted =
                    recordAudioPermissionGranted
                            && cameraPermissionGranted;

            if (permissionsGranted) {
                setupLocalMedia();
            } else {
                Toast.makeText(mContext, R.string.permissions_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStop() {
        removeCameraTrack();
        removeAllParticipants();
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        screenCaptureMenuItem = menu.findItem(R.id.share_screen_menu_item);
        requestPermissions();
        mRoomFragmentViewModel.getRoomEvents().observe(this, this::bindRoomEvents);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Snackbar.make(
                        mPrimaryVideoView,
                        R.string.screen_capture_permission_not_granted,
                        Snackbar.LENGTH_LONG)
                        .show();
                return;
            }
            mScreenCapturer = new ScreenCapturer(mContext, resultCode, data, screenCapturerListener);
            startScreenCapture();
        }
    }

//    @OnTextChanged(
//            value = R.id.room_edit_text,
//            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED
//    )
//    void onTextChanged(CharSequence text) {
//        connect.setEnabled(!TextUtils.isEmpty(text));
//    }
//

    private View.OnClickListener toggleVideoClickListener() {
        return v -> {
            // remember old video reference for updating thumb in room
            VideoTrack oldVideo = mCameraVideoTrack;

            if (mCameraVideoTrack == null) {
                isVideoMuted = false;

                // add local camera track
                mCameraVideoTrack =
                        LocalVideoTrack.create(
                                mContext,
                                true,
                                mCapturerCompat.getVideoCapturer(),
                                CAMERA_TRACK_NAME);

                if (localParticipant != null && mCameraVideoTrack != null) {
                    localParticipant.publishTrack(mCameraVideoTrack);
                }

            } else {
                isVideoMuted = true;
                // remove local camera track
                mCameraVideoTrack.removeRenderer(mPrimaryVideoView);

                if (localParticipant != null) {
                    localParticipant.unpublishTrack(mCameraVideoTrack);
                }
                mCameraVideoTrack.release();
                mCameraVideoTrack = null;
            }

            if (room != null && room.getState() == CONNECTED) {

                // update local participant thumb
                participantController.updateThumb(localParticipantSid, oldVideo, mCameraVideoTrack);

                if (participantController.getPrimaryItem().sid.equals(localParticipantSid)) {

                    // local video was rendered as primary view - refreshing
                    participantController.renderAsPrimary(
                            localParticipantSid,
                            getString(R.string.you),
                            mCameraVideoTrack,
                            mLocalAudioTrack == null,
                            mCapturerCompat.getCameraSource()
                                    == CameraCapturer.CameraSource.FRONT_CAMERA);

//                    participantController.getPrimaryView().showIdentityBadge(false);

                    // update thumb state
                    participantController.updateThumb(
                            localParticipantSid, mCameraVideoTrack, ParticipantView.State.SELECTED);
                }

            } else {
                renderLocalParticipantStub();
            }

            // update toggle button icon
            mLocalVideoButton.setImageResource(
                    mCameraVideoTrack != null
                            ? R.drawable.ic_videocam_white_24px
                            : R.drawable.ic_baseline_videocam_off_24);
        };
    }

    private View.OnClickListener toggleMicClickListener() {
        return v -> {
            int icon;
            if (mLocalAudioTrack == null) {
                isAudioMuted = false;
                mLocalAudioTrack = LocalAudioTrack.create(mContext, true, MICROPHONE_TRACK_NAME);
                if (localParticipant != null && mLocalAudioTrack != null) {
                    localParticipant.publishTrack(mLocalAudioTrack);
                }
                icon = R.drawable.ic_mic_white_24px;
            } else {
                isAudioMuted = true;
                if (localParticipant != null) {
                    localParticipant.unpublishTrack(mLocalAudioTrack);
                }
                mLocalAudioTrack.release();
                mLocalAudioTrack = null;
                icon = R.drawable.ic_baseline_mic_off_24;
            }
            mLocalMicButton.setImageResource(icon);
        };
    }


    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (mCapturerCompat != null) {

                boolean mirror =
                        mCapturerCompat.getCameraSource() == CameraCapturer.CameraSource.BACK_CAMERA;

                mCapturerCompat.switchCamera();

                if (participantController.getPrimaryItem().sid.equals(localParticipantSid)) {
                    participantController.updatePrimaryThumb(mirror);
                } else {
                    participantController.updateThumb(localParticipantSid, mCameraVideoTrack, mirror);
                }
            }
        };
    }

    private View.OnClickListener disconnectCallClickListener() {
        return v -> {
            mRoomFragmentViewModel.disconnect();
            stopScreenCapture();
        };
    }

    private View.OnClickListener connectVideoCallClickListener() {
        return v -> showConnectDialog();
    }

    private void showConnectDialog() {
        RoomDialog roomDialog = new RoomDialog();
        roomDialog.setTargetFragment(RoomFragment.this, 1);
        roomDialog.show(getParentFragmentManager(), "Room Dialog");
    }

    @Override
    public void connectRoom(String userName, String roomName, String roomType) {
        mUserName = userName;
        mRoomName = roomName;
        mRoomType = roomType;
        mRoomFragmentViewModel.fetchAccessToken(mUserName, mRoomName);
        accessTokenObserverLiveData();
    }

    private void accessTokenObserverLiveData() {
        mRoomFragmentViewModel.mAccessTokenMutableLiveData.observe(getViewLifecycleOwner(), new Observer<AccessTokenState>() {
            @Override
            public void onChanged(AccessTokenState accessTokenState) {
                if (accessTokenState.getAccessToken() != null && mRoomName != null) {
                    String accessToken = accessTokenState.getAccessToken();
                    if (mRoomType.equals("Peer to Peer")) {
                        mRoomFragmentViewModel.createPeerToPeerRoom(mRoomName);
                        peerRoomObserverLiveData(accessToken);
                    } else {
                        mRoomFragmentViewModel.connectToRoom(mRoomName, accessToken);
                    }
                } else {
                    handleError(accessTokenState.getThrowable());
                }
            }
        });
    }

    private void peerRoomObserverLiveData(String accessToken) {
        mRoomFragmentViewModel.mPeerToPeerRoomMutableLiveData.observe(getViewLifecycleOwner(), new Observer<PeerToPeerRoomState>() {
            @Override
            public void onChanged(PeerToPeerRoomState peerToPeerRoomState) {
                if (peerToPeerRoomState.getRoom() != null) {
                    mRoomFragmentViewModel.connectToRoom(mRoomName, accessToken);
                } else {
                    handleError(peerToPeerRoomState.getThrowable());

                }
            }
        });
    }

    private void handleError(Throwable throwable) {
        Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!permissionsGranted()) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSIONS_REQUEST_CODE);
            } else {
                setupLocalMedia();
            }
        } else {
            setupLocalMedia();
        }
    }

    private boolean permissionsGranted() {
        int resultCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
        int resultStorage =
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return ((resultCamera == PackageManager.PERMISSION_GRANTED)
                && (resultMic == PackageManager.PERMISSION_GRANTED)
                && (resultStorage == PackageManager.PERMISSION_GRANTED));
    }

    /**
     * Initialize local media and provide stub participant for primary view.
     */
    private void setupLocalMedia() {
        if (mLocalAudioTrack == null && !isAudioMuted) {
            mLocalAudioTrack = LocalAudioTrack.create(mContext, true, MICROPHONE_TRACK_NAME);
            if (room != null && localParticipant != null)
                localParticipant.publishTrack(mLocalAudioTrack);
        }
        if (mCameraVideoTrack == null && !isVideoMuted) {
            setupLocalVideoTrack();
            renderLocalParticipantStub();
            if (room != null && localParticipant != null)
                localParticipant.publishTrack(mCameraVideoTrack);
        }
    }

    /**
     * Create local video track
     */
    private void setupLocalVideoTrack() {

        // initialize capturer only once if needed
        if (mCapturerCompat == null) {
            mCapturerCompat =
                    new CameraCapturerCompat(mContext, CameraCapturer.CameraSource.FRONT_CAMERA);
        }

        mCameraVideoTrack =
                LocalVideoTrack.create(
                        mContext,
                        true,
                        mCapturerCompat.getVideoCapturer(),
                        CAMERA_TRACK_NAME);
        if (mCameraVideoTrack != null) {
            localVideoTrackNames.put(
                    mCameraVideoTrack.getName(), getString(R.string.camera_video_track));
        } else {
            Snackbar.make(
                    mPrimaryVideoView,
                    R.string.failed_to_add_camera_video_track,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Render local video track.
     *
     * <p>NOTE: Stub participant is created in controller. Make sure to remove it when connected to
     * room.
     */
    private void renderLocalParticipantStub() {
        participantController.renderAsPrimary(
                localParticipantSid,
                getString(R.string.you),
                mCameraVideoTrack,
                mLocalAudioTrack == null,
                mCapturerCompat.getCameraSource() == CameraCapturer.CameraSource.FRONT_CAMERA);

        mPrimaryVideoView.showIdentityBadge(false);
    }

    private void updateUI(Room room, RoomEvent roomEvent) {
        int switchCameraButtonSate = View.GONE;
        int connectButtonState = View.VISIBLE;
        int disconnectButtonState = View.GONE;
        int joinStatusLayoutState = View.GONE;
        boolean screenCaptureMenuItemState = false;
        String toolbarTitle = getString(R.string.app_name);
        String joinStatus = "";

        if (roomEvent instanceof RoomEvent.Connecting) {
            joinStatusLayoutState = View.VISIBLE;

            if (mRoomName != null) {
                roomName = mRoomName;
            }
            joinStatus = "Joining ";
        }

        if (room != null) {
            switch (room.getState()) {
                case CONNECTED:
                    toolbarTitle = "Room: "+ mRoomName;
                    switchCameraButtonSate = View.VISIBLE;
                    connectButtonState = View.GONE;
                    disconnectButtonState = View.VISIBLE;
                    screenCaptureMenuItemState = true;

                    joinStatusLayoutState = View.GONE;
                    joinStatus = "";

                    break;
                case DISCONNECTED:
                    toolbarTitle = getString(R.string.app_name);
                    switchCameraButtonSate = View.GONE;
                    connectButtonState = View.VISIBLE;
                    disconnectButtonState = View.GONE;
                    screenCaptureMenuItemState = false;
                    break;
            }


        }

        // Check mute state
        if (isAudioMuted) {
            mLocalMicButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
        }
        if (isVideoMuted) {
            mLocalVideoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24);
        }


        mSwitchCameraButton.setVisibility(switchCameraButtonSate);
        mVideoCallButton.setVisibility(connectButtonState);
        mEndCallButton.setVisibility(disconnectButtonState);

        mJoinMessageLayout.setVisibility(joinStatusLayoutState);
        mJoinStatus.setVisibility(joinStatusLayoutState);
        mJoinRoomName.setVisibility(joinStatusLayoutState);
        mJoiningRoomProgressBar.setVisibility(joinStatusLayoutState);

        setToolbarTitle(toolbarTitle);
        mJoinStatus.setText(joinStatus);
        mJoinRoomName.setText(mRoomName);

        if (screenCaptureMenuItem != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenCaptureMenuItem.setVisible(screenCaptureMenuItemState);
        }
    }

    private void setToolbarTitle(String toolbarTitle) {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(toolbarTitle);
        }
    }

    private void setAudioFocus(boolean setFocus) {
        if (setFocus) {
            savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
            savedIsMicrophoneMute = audioManager.isMicrophoneMute();
            setMicrophoneMute();
            savedAudioMode = audioManager.getMode();
            // Request audio focus before making any device switch.
            requestAudioFocus();
            /*
             * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
             * required to be in this mode when playout and/or recording starts for
             * best possible VoIP performance.
             * Some devices have difficulties with speaker mode if this is not set.
             */
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            setVolumeControl(true);
        } else {
            audioManager.setMode(savedAudioMode);
            audioManager.abandonAudioFocus(null);
            audioManager.setMicrophoneMute(savedIsMicrophoneMute);
            audioManager.setSpeakerphoneOn(savedIsSpeakerPhoneOn);
            setVolumeControl(false);
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
            AudioFocusRequest focusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(i -> {
                            })
                            .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(
                    null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    /**
     * Sets the microphone mute state.
     */
    private void setMicrophoneMute() {
        boolean wasMuted = audioManager.isMicrophoneMute();
        if (!wasMuted) {
            return;
        }
        audioManager.setMicrophoneMute(false);
    }

    private void setVolumeControl(boolean setVolumeControl) {
        if (setVolumeControl) {
            /*
             * Enable changing the volume using the up/down keys during a conversation
             */
            getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }
    }

    @TargetApi(21)
    private void requestScreenCapturePermission() {
        Log.d(TAG, "Requesting permission to capture screen");
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // This initiates a prompt dialog for the user to confirm screen projection.
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
    }

    private void startScreenCapture() {
        mScreenVideoTrack = LocalVideoTrack.create(mContext, true, mScreenCapturer, SCREEN_TRACK_NAME);

        if (mScreenVideoTrack != null) {
            screenCaptureMenuItem.setIcon(R.drawable.ic_stop_screen_share_white_24dp);
            screenCaptureMenuItem.setTitle(R.string.stop_screen_share);
            localVideoTrackNames.put(
                    mScreenVideoTrack.getName(), getString(R.string.screen_video_track));

            if (localParticipant != null) {
                localParticipant.publishTrack(mScreenVideoTrack);
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
            if (localParticipant != null) {
                localParticipant.unpublishTrack(mScreenVideoTrack);
            }
            mScreenVideoTrack.release();
            localVideoTrackNames.remove(mScreenVideoTrack.getName());
            mScreenVideoTrack = null;
            screenCaptureMenuItem.setIcon(R.drawable.ic_screen_share_white_24dp);
            screenCaptureMenuItem.setTitle(R.string.share_screen);
        }
    }

    /**
     * Provides remoteParticipant a listener for media events and add thumb.
     *
     * @param remoteParticipant newly joined room remoteParticipant
     */
    private void addParticipant(RemoteParticipant remoteParticipant, boolean renderAsPrimary) {
        boolean muted =
                remoteParticipant.getRemoteAudioTracks().size() <= 0
                        || !remoteParticipant.getRemoteAudioTracks().get(0).isTrackEnabled();
        List<RemoteVideoTrackPublication> remoteVideoTrackPublications =
                remoteParticipant.getRemoteVideoTracks();

        if (remoteVideoTrackPublications.isEmpty()) {
            /*
             * Add placeholder UI by passing null video track for a participant that is not
             * sharing any video tracks.
             */
            addParticipantVideoTrack(remoteParticipant, muted, null, renderAsPrimary);
        } else {
            for (RemoteVideoTrackPublication remoteVideoTrackPublication :
                    remoteVideoTrackPublications) {
                addParticipantVideoTrack(
                        remoteParticipant,
                        muted,
                        remoteVideoTrackPublication.getRemoteVideoTrack(),
                        renderAsPrimary);
                renderAsPrimary = false;
            }
        }
    }

    private void addParticipantVideoTrack(
            RemoteParticipant remoteParticipant,
            boolean muted,
            RemoteVideoTrack remoteVideoTrack,
            boolean renderAsPrimary) {
        if (renderAsPrimary) {
            ParticipantPrimaryView primaryView = participantController.getPrimaryView();

            renderItemAsPrimary(
                    new ParticipantController.Item(
                            remoteParticipant.getSid(),
                            remoteParticipant.getIdentity(),
                            remoteVideoTrack,
                            muted,
                            false));
            RemoteParticipantListener listener =
                    new RemoteParticipantListener(primaryView, remoteParticipant.getSid());
            remoteParticipant.setListener(listener);
        } else {
            participantController.addThumb(
                    remoteParticipant.getSid(),
                    remoteParticipant.getIdentity(),
                    remoteVideoTrack,
                    muted,
                    false);

            RemoteParticipantListener listener =
                    new RemoteParticipantListener(
                            participantController.getThumb(
                                    remoteParticipant.getSid(), remoteVideoTrack),
                            remoteParticipant.getSid());
            remoteParticipant.setListener(listener);
        }
    }

    /**
     * Sets new item to render as primary view and moves existing primary view item to thumbs view.
     *
     * @param item New item to be rendered in primary view
     */
    private void renderItemAsPrimary(ParticipantController.Item item) {
        // nothing to click while not in room
        if (room == null) return;

        // no need to renderer if same item clicked
        ParticipantController.Item old = participantController.getPrimaryItem();
        if (old != null && item.sid.equals(old.sid) && item.videoTrack == old.videoTrack) return;

        // add back old participant to thumbs
        if (old != null) {

            if (old.sid.equals(localParticipantSid)) {

                // toggle local participant state
                int state =
                        old.videoTrack == null
                                ? ParticipantView.State.NO_VIDEO
                                : ParticipantView.State.VIDEO;
                participantController.updateThumb(old.sid, old.videoTrack, state);
                participantController.updateThumb(old.sid, old.videoTrack, old.mirror);

            } else {

                // add thumb for remote participant
                RemoteParticipant remoteParticipant = getRemoteParticipant(old);
                if (remoteParticipant != null) {
                    participantController.addThumb(
                            old.sid, old.identity, old.videoTrack, old.muted, old.mirror);
                    RemoteParticipantListener listener =
                            new RemoteParticipantListener(
                                    participantController.getThumb(old.sid, old.videoTrack),
                                    remoteParticipant.getSid());
                    remoteParticipant.setListener(listener);
                }
            }
        }

        // handle new primary participant click
        participantController.renderAsPrimary(item);

        RemoteParticipant remoteParticipant = getRemoteParticipant(item);
        if (remoteParticipant != null) {
            ParticipantPrimaryView primaryView = participantController.getPrimaryView();
            RemoteParticipantListener listener =
                    new RemoteParticipantListener(primaryView, remoteParticipant.getSid());
            remoteParticipant.setListener(listener);
        }

        if (item.sid.equals(localParticipantSid)) {

            // toggle local participant state and hide his badge
            participantController.updateThumb(
                    item.sid, item.videoTrack, ParticipantView.State.SELECTED);
            //   participantController.getPrimaryView().showIdentityBadge(false);
        } else {

            // remove remote participant thumb
            participantController.removeThumb(item);
        }
    }

    private @Nullable
    RemoteParticipant getRemoteParticipant(ParticipantController.Item item) {
        RemoteParticipant remoteParticipant = null;

        for (RemoteParticipant temp : room.getRemoteParticipants()) {
            if (temp.getSid().equals(item.sid)) remoteParticipant = temp;
        }

        return remoteParticipant;
    }

    /**
     * Removes all participant thumbs and push local camera as primary with empty sid.
     */
    private void removeAllParticipants() {
        if (room != null) {
            participantController.removeAllThumbs();
            participantController.removePrimary();

            renderLocalParticipantStub();
        }
    }

    /**
     * Remove single remoteParticipant thumbs and all it associated thumbs. If rendered as primary
     * remoteParticipant, primary view switches to local video track.
     *
     * @param remoteParticipant recently disconnected remoteParticipant.¬
     */
    private void removeParticipant(RemoteParticipant remoteParticipant) {

        if (participantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {

            // render local video if primary remoteParticipant has gone
            participantController.getThumb(localParticipantSid, mCameraVideoTrack).callOnClick();
        }

        participantController.removeThumbs(remoteParticipant.getSid());
    }

    /**
     * Remove the video track and mark the track to be restored when going to the settings screen or
     * going to the background
     */
    private void removeCameraTrack() {
        if (mCameraVideoTrack != null) {
            if (localParticipant != null) {
                localParticipant.unpublishTrack(mCameraVideoTrack);
            }
            mCameraVideoTrack.release();
            restoreLocalVideoCameraTrack = true;
            mCameraVideoTrack = null;
        }
    }

    /**
     * Try to restore camera video track after going to the settings screen or background
     */
    private void restoreCameraTrack() {
        if (restoreLocalVideoCameraTrack) {
            setupLocalVideoTrack();
            renderLocalParticipantStub();
            restoreLocalVideoCameraTrack = false;
        }
    }

    /**
     * Provides participant thumb click listener. On thumb click appropriate video track is being
     * send to primary view. If local camera track becomes primary, it should just change it state
     * to SELECTED state, if remote particpant track is going to be primary - thumb is removed.
     *
     * @return participant click listener.
     */
    private ParticipantController.ItemClickListener participantClickListener() {
        return this::renderItemAsPrimary;
    }

    private void initializeRoom() {
        if (room != null) {

            localParticipant = room.getLocalParticipant();

            publishLocalTracks();

            //setAudioFocus(true);

            addParticipantViews();
        }
    }

    private void publishLocalTracks() {
        if (localParticipant != null) {
            if (mCameraVideoTrack != null) {
                Log.d(TAG, "Camera track: " + mCameraVideoTrack);
                localParticipant.publishTrack(mCameraVideoTrack);
            }

            if (mLocalAudioTrack != null) {
                localParticipant.publishTrack(mLocalAudioTrack);
            }
        }
    }

    private void addParticipantViews() {
        if (room != null && localParticipant != null) {
            localParticipantSid = localParticipant.getSid();
            // remove primary view
            participantController.removePrimary();

            // add local thumb and "click" on it to make primary
            participantController.addThumb(
                    localParticipantSid,
                    getString(R.string.you),
                    mCameraVideoTrack,
                    mLocalAudioTrack == null,
                    mCapturerCompat.getCameraSource() == CameraCapturer.CameraSource.FRONT_CAMERA);

            localParticipant.setListener(
                    new LocalParticipantListener(
                            participantController.getThumb(localParticipantSid, mCameraVideoTrack)));
            participantController.getThumb(localParticipantSid, mCameraVideoTrack).callOnClick();

            // add existing room participants thumbs
            boolean isFirstParticipant = true;
            for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                addParticipant(remoteParticipant, isFirstParticipant);
                isFirstParticipant = false;
                if (room.getDominantSpeaker() != null) {
                    if (room.getDominantSpeaker().getSid().equals(remoteParticipant.getSid())) {
                        VideoTrack videoTrack =
                                (remoteParticipant.getRemoteVideoTracks().size() > 0)
                                        ? remoteParticipant
                                        .getRemoteVideoTracks()
                                        .get(0)
                                        .getRemoteVideoTrack()
                                        : null;
                        if (videoTrack != null) {
                            ParticipantView participantView =
                                    participantController.getThumb(
                                            remoteParticipant.getSid(), videoTrack);
                            participantController.setDominantSpeaker(participantView);
                        }
                    }
                }
            }
        }
    }

    private void bindRoomEvents(RoomEvent roomEvent) {
        if (roomEvent != null) {
            this.room = roomEvent.getRoom();
            if (room != null) {
                requestPermissions();
                if (roomEvent instanceof RoomEvent.RoomState) {
                    Room.State state = room.getState();
                    switch (state) {
                        case CONNECTED:
                            initializeRoom();
                            break;
                        case DISCONNECTED:
                            removeAllParticipants();
                            localParticipant = null;
                            room = null;
                            localParticipantSid = LOCAL_PARTICIPANT_STUB_SID;
                            setAudioFocus(false);
                            networkQualityLevels.clear();
                            break;
                    }
                }
                if (roomEvent instanceof RoomEvent.ConnectFailure) {
                    new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                            .setTitle(getString(R.string.room_screen_connection_failure_title))
                            .setMessage(getString(R.string.room_screen_connection_failure_message))
                            .setNeutralButton("OK", null)
                            .show();
                    removeAllParticipants();
                    setAudioFocus(false);
                }
                if (roomEvent instanceof RoomEvent.ParticipantConnected) {
                    boolean renderAsPrimary = room.getRemoteParticipants().size() == 1;
                    addParticipant(
                            ((RoomEvent.ParticipantConnected) roomEvent).getRemoteParticipant(),
                            renderAsPrimary);

                }
                if (roomEvent instanceof RoomEvent.ParticipantDisconnected) {
                    RemoteParticipant remoteParticipant =
                            ((RoomEvent.ParticipantDisconnected) roomEvent).getRemoteParticipant();
                    networkQualityLevels.remove(remoteParticipant.getSid());
                    removeParticipant(remoteParticipant);

                }
                if (roomEvent instanceof RoomEvent.DominantSpeakerChanged) {
                    RemoteParticipant remoteParticipant =
                            ((RoomEvent.DominantSpeakerChanged) roomEvent).getRemoteParticipant();

                    if (remoteParticipant == null) {
                        participantController.setDominantSpeaker(null);
                        return;
                    }
                    VideoTrack videoTrack =
                            (remoteParticipant.getRemoteVideoTracks().size() > 0)
                                    ? remoteParticipant
                                    .getRemoteVideoTracks()
                                    .get(0)
                                    .getRemoteVideoTrack()
                                    : null;
                    if (videoTrack != null) {
                        ParticipantView participantView =
                                participantController.getThumb(
                                        remoteParticipant.getSid(), videoTrack);
                        if (participantView != null) {
                            participantController.setDominantSpeaker(participantView);
                        } else {
                            remoteParticipant.getIdentity();
                            ParticipantPrimaryView primaryParticipantView =
                                    participantController.getPrimaryView();
                            if (primaryParticipantView.identity.equals(
                                    remoteParticipant.getIdentity())) {
                                participantController.setDominantSpeaker(
                                        participantController.getPrimaryView());
                            } else {
                                participantController.setDominantSpeaker(null);
                            }
                        }
                    }
                }
            }
            updateUI(room, roomEvent);
        }
    }


    private class LocalParticipantListener implements LocalParticipant.Listener {

        private ImageView networkQualityImage;

        LocalParticipantListener(ParticipantView primaryView) {
            networkQualityImage = primaryView.networkQualityLevelImg;
        }

        @Override
        public void onAudioTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalAudioTrackPublication localAudioTrackPublication) {
        }

        @Override
        public void onAudioTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalAudioTrack localAudioTrack,
                @NonNull TwilioException twilioException) {
        }

        @Override
        public void onVideoTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalVideoTrackPublication localVideoTrackPublication) {
        }

        @Override
        public void onVideoTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalVideoTrack localVideoTrack,
                @NonNull TwilioException twilioException) {
        }

        @Override
        public void onDataTrackPublished(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalDataTrackPublication localDataTrackPublication) {
        }

        @Override
        public void onDataTrackPublicationFailed(
                @NonNull LocalParticipant localParticipant,
                @NonNull LocalDataTrack localDataTrack,
                @NonNull TwilioException twilioException) {
        }

        @Override
        public void onNetworkQualityLevelChanged(
                @NonNull LocalParticipant localParticipant,
                @NonNull NetworkQualityLevel networkQualityLevel) {
            setNetworkQualityLevelImage(
                    networkQualityImage, networkQualityLevel, localParticipant.getSid());
        }
    }

    private class RemoteParticipantListener implements RemoteParticipant.Listener {

        private ImageView networkQualityImage;

        RemoteParticipantListener(ParticipantView primaryView, String sid) {
            networkQualityImage = primaryView.networkQualityLevelImg;
            setNetworkQualityLevelImage(networkQualityImage, networkQualityLevels.get(sid), sid);
        }

        @Override
        public void onNetworkQualityLevelChanged(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull NetworkQualityLevel networkQualityLevel) {
            setNetworkQualityLevelImage(
                    networkQualityImage, networkQualityLevel, remoteParticipant.getSid());
        }

        @Override
        public void onAudioTrackPublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            Timber.i(
                    "onAudioTrackPublished: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed());


        }

        @Override
        public void onAudioTrackUnpublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            Timber.i(
                    "onAudioTrackUnpublished: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed());

        }

        @Override
        public void onVideoTrackPublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            Timber.i(
                    "onVideoTrackPublished: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled(),
                    remoteVideoTrackPublication.isTrackSubscribed());

        }

        @Override
        public void onVideoTrackUnpublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            Timber.i(
                    "onVideoTrackUnpublished: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled(),
                    remoteVideoTrackPublication.isTrackSubscribed());

        }

        @Override
        public void onAudioTrackSubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                @NonNull RemoteAudioTrack remoteAudioTrack) {
            Timber.i(
                    "onAudioTrackSubscribed: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed());
            boolean newAudioState = !remoteAudioTrackPublication.isTrackEnabled();

            if (participantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {

                // update audio state for primary view
                participantController.getPrimaryItem().muted = newAudioState;
                participantController.getPrimaryView().setMuted(newAudioState);

            } else {

                // update thumbs with audio state
                participantController.updateThumbs(remoteParticipant.getSid(), newAudioState);
            }
        }

        @Override
        public void onAudioTrackSubscriptionFailed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                @NonNull TwilioException twilioException) {
            Timber.w(
                    "onAudioTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    twilioException.getMessage());

            Snackbar.make(mPrimaryVideoView, "onAudioTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onAudioTrackUnsubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication,
                @NonNull RemoteAudioTrack remoteAudioTrack) {
            Timber.i(
                    "onAudioTrackUnsubscribed: remoteParticipant: %s, audio: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled(),
                    remoteAudioTrackPublication.isTrackSubscribed());

            if (participantController.getPrimaryItem().sid.equals(remoteParticipant.getSid())) {

                // update audio state for primary view
                participantController.getPrimaryItem().muted = true;
                participantController.getPrimaryView().setMuted(true);

            } else {

                // update thumbs with audio state
                participantController.updateThumbs(remoteParticipant.getSid(), true);
            }
        }

        @Override
        public void onVideoTrackSubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                @NonNull RemoteVideoTrack remoteVideoTrack) {
            Timber.i(
                    "onVideoTrackSubscribed: remoteParticipant: %s, video: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled(),
                    remoteVideoTrackPublication.isTrackSubscribed());

            ParticipantController.Item primary = participantController.getPrimaryItem();

            if (primary != null
                    && primary.sid.equals(remoteParticipant.getSid())
                    && primary.videoTrack == null) {
                // no thumb needed - render as primary
                primary.videoTrack = remoteVideoTrack;
                participantController.renderAsPrimary(primary);
            } else {
                // not a primary remoteParticipant requires thumb
                participantController.addOrUpdateThumb(
                        remoteParticipant.getSid(),
                        remoteParticipant.getIdentity(),
                        null,
                        remoteVideoTrack);
            }
        }

        @Override
        public void onVideoTrackSubscriptionFailed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                @NonNull TwilioException twilioException) {
            Timber.w(
                    "onVideoTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    twilioException.getMessage());

            Snackbar.make(mPrimaryVideoView, "onVideoTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onVideoTrackUnsubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication,
                @NonNull RemoteVideoTrack remoteVideoTrack) {
            Timber.i(
                    "onVideoTrackUnsubscribed: remoteParticipant: %s, video: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled());

            ParticipantController.Item primary = participantController.getPrimaryItem();

            if (primary != null
                    && primary.sid.equals(remoteParticipant.getSid())
                    && primary.videoTrack == remoteVideoTrack) {

                // Remove primary video track
                primary.videoTrack = null;

                // Try to find another video track to render as primary
                List<RemoteVideoTrackPublication> remoteVideoTracks =
                        remoteParticipant.getRemoteVideoTracks();
                for (RemoteVideoTrackPublication newRemoteVideoTrackPublication :
                        remoteVideoTracks) {
                    RemoteVideoTrack newRemoteVideoTrack =
                            newRemoteVideoTrackPublication.getRemoteVideoTrack();
                    if (newRemoteVideoTrack != remoteVideoTrack) {
                        participantController.removeThumb(
                                remoteParticipant.getSid(), newRemoteVideoTrack);
                        primary.videoTrack = newRemoteVideoTrack;
                        break;
                    }
                }
                participantController.renderAsPrimary(primary);
            } else {

                // remove thumb or leave empty video thumb
                participantController.removeOrEmptyThumb(
                        remoteParticipant.getSid(),
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack);
            }
        }

        @Override
        public void onDataTrackPublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {
            Timber.i(
                    "onDataTrackPublished: remoteParticipant: %s, data: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled());
        }

        @Override
        public void onDataTrackUnpublished(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {
            Timber.i(
                    "onDataTrackUnpublished: remoteParticipant: %s, data: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled());
        }

        @Override
        public void onDataTrackSubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                @NonNull RemoteDataTrack remoteDataTrack) {
            Timber.i(
                    "onDataTrackSubscribed: remoteParticipant: %s, data: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled(),
                    remoteDataTrackPublication.isTrackSubscribed());
        }

        @Override
        public void onDataTrackSubscriptionFailed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                @NonNull TwilioException twilioException) {
            Timber.w(
                    "onDataTrackSubscriptionFailed: remoteParticipant: %s, video: %s, exception: %s",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    twilioException.getMessage());

            Snackbar.make(mPrimaryVideoView, "onDataTrackSubscriptionFailed", Snackbar.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onDataTrackUnsubscribed(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteDataTrackPublication remoteDataTrackPublication,
                @NonNull RemoteDataTrack remoteDataTrack) {
            Timber.i(
                    "onDataTrackUnsubscribed: remoteParticipant: %s, data: %s, enabled: %b, subscribed: %b",
                    remoteParticipant.getIdentity(),
                    remoteDataTrackPublication.getTrackSid(),
                    remoteDataTrackPublication.isTrackEnabled(),
                    remoteDataTrackPublication.isTrackSubscribed());
        }

        @Override
        public void onAudioTrackEnabled(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            Timber.i(
                    "onAudioTrackEnabled: remoteParticipant: %s, audio: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled());


        }

        @Override
        public void onAudioTrackDisabled(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            Timber.i(
                    "onAudioTrackDisabled: remoteParticipant: %s, audio: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteAudioTrackPublication.getTrackSid(),
                    remoteAudioTrackPublication.isTrackEnabled());


        }

        @Override
        public void onVideoTrackEnabled(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            Timber.i(
                    "onVideoTrackEnabled: remoteParticipant: %s, video: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled());


        }

        @Override
        public void onVideoTrackDisabled(
                @NonNull RemoteParticipant remoteParticipant,
                @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            Timber.i(
                    "onVideoTrackDisabled: remoteParticipant: %s, video: %s, enabled: %b",
                    remoteParticipant.getIdentity(),
                    remoteVideoTrackPublication.getTrackSid(),
                    remoteVideoTrackPublication.isTrackEnabled());


        }
    }

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
}
