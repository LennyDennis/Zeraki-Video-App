package com.lennydennis.zerakiapp.ui.views

import com.twilio.video.RemoteParticipant
import com.twilio.video.Room

sealed class RoomEvent(val room: Room? = null) {
    object Connecting : RoomEvent()
   // class TokenError(val serviceError: AuthServiceError? = null) : RoomEvent()
    class RoomState(room: Room) : RoomEvent(room)
    class ConnectFailure(room: Room) : RoomEvent(room)
    class ParticipantConnected(room: Room, val remoteParticipant: RemoteParticipant) : RoomEvent(room)
    class ParticipantDisconnected(room: Room, val remoteParticipant: RemoteParticipant) : RoomEvent(room)
    class DominantSpeakerChanged(room: Room, val remoteParticipant: RemoteParticipant?) : RoomEvent(room)
}
