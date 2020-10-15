//package com.lennydennis.zerakiapp.ui.viewmodels;
//
//import android.app.Application;
//
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.MutableLiveData;
//
//import com.lennydennis.zerakiapp.model.AccessTokenState;
//import com.lennydennis.zerakiapp.repositories.AccessTokenRepo;
//import com.lennydennis.zerakiapp.repositories.AccessTokenRepoImpl;
//
//public class RoomFragmentViewModel2 extends AndroidViewModel {
//
//    private AccessTokenRepo mAccessTokenRepo = new AccessTokenRepoImpl();
//    public MutableLiveData<AccessTokenState> mAccessTokenMutableLiveData = new MutableLiveData<>();
//
//    public RoomFragmentViewModel2(@NonNull Application application) {
//        super(application);
//    }
//
//    public MutableLiveData<AccessTokenState> fetchAccessToken(String userName, String roomName){
//        return  mAccessTokenMutableLiveData = mAccessTokenRepo.fetchAccessToken(userName,roomName);
//    }
//
//}
//
////class RoomViewModel(private val roomManager: RoomManager) : ViewModel() {
////
////        // TODO Build ViewStates for UI to consume instead of just passing events
////        val roomEvents: LiveData<RoomEvent?> = roomManager.viewEvents
////
////        // TODO Add single point of entry function from UI, something like "processInput"
////
////        fun connectToRoom(
////        identity: String,
////        roomName: String,
////        isNetworkQualityEnabled: Boolean
////        ) =
////        viewModelScope.launch {
////        roomManager.connectToRoom(
////        identity,
////        roomName,
////        isNetworkQualityEnabled)
////        }
////
////        fun disconnect() {
////        roomManager.disconnect()
////        }
////
////class RoomViewModelFactory(private val roomManager: RoomManager) : ViewModelProvider.Factory {
////
////        override fun <T : ViewModel> create(modelClass: Class<T>): T {
////        return RoomViewModel(roomManager) as T
////        }
////        }
