package com.lennydennis.zerakiapp.model;

import com.twilio.video.Room;

public class PeerToPeerRoomState {
    private Room room;
    private Throwable throwable;

    public PeerToPeerRoomState(Room room) {
        this.room = room;
        this.throwable = null;
    }

    public PeerToPeerRoomState(Throwable throwable) {
        this.room = null;
        this.throwable = throwable;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
