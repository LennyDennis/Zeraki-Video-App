package com.lennydennis.zerakiapp.ui.views

import android.app.Application
import android.content.SharedPreferences
import com.lennydennis.zerakiapp.ApplicationModule
import com.lennydennis.zerakiapp.ApplicationScope
import com.lennydennis.zerakiapp.model.DataModule
import com.twilio.video.app.ui.room.RoomManager
import dagger.Module
import dagger.Provides

@Module(includes = [
    ApplicationModule::class,
    DataModule::class])
class RoomManagerModule {

    @Provides
    @ApplicationScope
    fun providesRoomManager(
        application: Application
    ): RoomManager {
        return RoomManager(application)
    }
}