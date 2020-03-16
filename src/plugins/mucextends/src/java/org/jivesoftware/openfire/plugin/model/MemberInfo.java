package org.jivesoftware.openfire.plugin.model;

public class MemberInfo {

    String userNickName;
    String userJID;
    String roomJID;
    int affiliation;
    int myAffiliation;

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    public String getUserJID() {
        return userJID;
    }

    public void setUserJID(String userJID) {
        this.userJID = userJID;
    }

    public String getRoomJID() {
        return roomJID;
    }

    public void setRoomJID(String roomJID) {
        this.roomJID = roomJID;
    }

    public int getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(int affiliation) {
        this.affiliation = affiliation;
    }

    public int getMyAffiliation() {
        return myAffiliation;
    }

    public void setMyAffiliation(int myAffiliation) {
        this.myAffiliation = myAffiliation;
    }
}

