package org.jivesoftware.openfire.plugin.model;

public enum NotificationType {

    APPLY(1, "申请通知"),
    INVITE(2, "邀请通知"),
    QUIT(3, "退出通知"),
    KICK(4, "踢出通知"),
    APPLY_RESULT(5, "申请回执通知"),
    INVITE_RESULT(6, "邀请回执通知"),
    DESTROY(7, "群解散通知");

    int value;
    String label;

    NotificationType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }
}
