package com.lennydennis.zerakiapp.ui.viewmodels

import androidx.lifecycle.*
import com.lennydennis.zerakiapp.model.AccessTokenState
import com.lennydennis.zerakiapp.model.PeerToPeerRoomState
import com.lennydennis.zerakiapp.repositories.AccessTokenRepo
import com.lennydennis.zerakiapp.repositories.AccessTokenRepoImpl
import com.lennydennis.zerakiapp.repositories.PeerToPeerRoomRepo
import com.lennydennis.zerakiapp.repositories.PeerToPeerRoomRepoImpl
import com.lennydennis.zerakiapp.ui.rooms.RoomEvent
import com.twilio.video.app.ui.room.RoomManager
import kotlinx.coroutines.launch

class RoomFragmentViewModel(private val roomManager: RoomManager) : ViewModel() {

    val roomEvents: LiveData<RoomEvent?> = roomManager.viewEvents

    private val mAccessTokenRepo: AccessTokenRepo = AccessTokenRepoImpl()
    lateinit var mAccessTokenMutableLiveData: LiveData<AccessTokenState>

    private val mPeerToPeerRoomRepo: PeerToPeerRoomRepo = PeerToPeerRoomRepoImpl()
    lateinit var mPeerToPeerRoomMutableLiveData: LiveData<PeerToPeerRoomState>

    fun fetchAccessToken(userName: String?, roomName: String?): MutableLiveData<AccessTokenState?>? {
        return mAccessTokenRepo.fetchAccessToken(userName, roomName).also { mAccessTokenMutableLiveData = it }
    }

    fun createPeerToPeerRoom(roomName: String?): MutableLiveData<PeerToPeerRoomState?>? {
        return mPeerToPeerRoomRepo.createPeerToPeerRoom(roomName).also { mPeerToPeerRoomMutableLiveData = it }
    }

    fun connectToRoom(
            roomName: String,
            accessToken: String) =
            viewModelScope.launch {
                roomManager.connectToRoom(
                        roomName,
                        accessToken)
            }

    fun disconnect() {
        roomManager.disconnect()
    }

    class RoomViewModelFactory(private val roomManager: RoomManager) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoomFragmentViewModel(roomManager) as T
        }
    }
}
