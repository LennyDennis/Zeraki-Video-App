package com.twilio.video.app.ui.room

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lennydennis.zerakiapp.ui.rooms.RoomEvent
import com.lennydennis.zerakiapp.ui.rooms.RoomEvent.*
import com.twilio.video.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class RoomManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    val VIDEO_CODEC = "pref_video_codecs"
    var room: Room? = null
    private val mutableViewEvents: MutableLiveData<RoomEvent?> = MutableLiveData()

    val viewEvents: LiveData<RoomEvent?> = mutableViewEvents

    private val roomListener = RoomListener()

    fun disconnect() {
        room?.disconnect()
    }

    suspend fun connectToRoom(
        roomName: String,
        accessToken:String
    ) {
        coroutineScope.launch {
            try {
                mutableViewEvents.postValue(Connecting)

                val preferedVideoCodec: VideoCodec = getVideoCodecPreference(VIDEO_CODEC)

                val preferredAudioCodec: AudioCodec = getAudioCodecPreference()

                val configuration = NetworkQualityConfiguration(
                        NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL,
                        NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL)

                val connectOptionsBuilder = ConnectOptions.Builder(accessToken)
                        .roomName(roomName)
                        .enableAutomaticSubscription(true)
                        .enableDominantSpeaker(true)
                        .enableInsights(true)
                        .enableNetworkQuality(true)
                        .networkQualityConfiguration(configuration)

                val maxVideoBitrate:Int = 0;
                val maxAudioBitrate:Int = 0;
                val encodingParameters = EncodingParameters(maxAudioBitrate, maxVideoBitrate)
                connectOptionsBuilder.preferVideoCodecs(listOf(preferedVideoCodec))
                connectOptionsBuilder.preferAudioCodecs(listOf(preferredAudioCodec))
                connectOptionsBuilder.encodingParameters(encodingParameters)

                val room = Video.connect(
                        context,
                        connectOptionsBuilder.build(),
                        roomListener)
                this@RoomManager.room = room
            } catch (e: Exception) {
                Timber.e(e, "Error")
            }
        }
    }

    private fun getVideoCodecPreference(key: String): VideoCodec {
        return (Vp8Codec.NAME)?.let { videoCodecName ->
                when (videoCodecName) {
                    Vp8Codec.NAME -> {
                        val simulcast:Boolean = false
                        Vp8Codec(simulcast)
                    }
                    H264Codec.NAME -> H264Codec()
                    Vp9Codec.NAME -> Vp9Codec()
                    else -> Vp8Codec()
                }
            } ?: Vp8Codec()
    }

    private fun getAudioCodecPreference(): AudioCodec {

        return (OpusCodec.NAME)?.let { audioCodecName ->
            when (audioCodecName) {
                IsacCodec.NAME -> IsacCodec()
                PcmaCodec.NAME -> PcmaCodec()
                PcmuCodec.NAME -> PcmuCodec()
                G722Codec.NAME -> G722Codec()
                else -> OpusCodec()
            }
        } ?: OpusCodec()
    }

    inner class RoomListener : Room.Listener {
        override fun onConnected(room: Room) {
            Timber.i("onConnected -> room sid: %s",
                    room.sid)

            // Reset the speakerphone
            mutableViewEvents.value = RoomState(room)
        }

        override fun onDisconnected(room: Room, twilioException: TwilioException?) {
            Timber.i("Disconnected from room -> sid: %s, state: %s",
                    room.sid, room.state)

            mutableViewEvents.value = RoomState(room)
        }

        override fun onConnectFailure(room: Room, twilioException: TwilioException) {
            Timber.e(
                    "Failed to connect to room -> sid: %s, state: %s, code: %d, error: %s",
                    room.sid,
                    room.state,
                    twilioException.code,
                    twilioException.message)
            mutableViewEvents.value = ConnectFailure(room)
        }

        override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
            Timber.i("RemoteParticipant connected -> room sid: %s, remoteParticipant: %s",
                    room.sid, remoteParticipant.sid)
            mutableViewEvents.value = ParticipantConnected(room, remoteParticipant)
        }

        override fun onParticipantDisconnected(room: Room, remoteParticipant: RemoteParticipant) {
            Timber.i("RemoteParticipant disconnected -> room sid: %s, remoteParticipant: %s",
                    room.sid, remoteParticipant.sid)
            mutableViewEvents.value = ParticipantDisconnected(room, remoteParticipant)
        }

        override fun onDominantSpeakerChanged(room: Room, remoteParticipant: RemoteParticipant?) {
            Timber.i("DominantSpeakerChanged -> room sid: %s, remoteParticipant: %s",
                    room.sid, remoteParticipant?.sid)
            mutableViewEvents.value = DominantSpeakerChanged(room, remoteParticipant)
        }

        override fun onRecordingStarted(room: Room) {}

        override fun onReconnected(room: Room) {
            Timber.i("onReconnected: %s", room.name)
        }

        override fun onReconnecting(room: Room, twilioException: TwilioException) {
            Timber.i("onReconnecting: %s", room.name)
        }

        override fun onRecordingStopped(room: Room) {}
    }
}