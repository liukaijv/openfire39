package org.jivesoftware.openfire.plugin.model;

public enum NotificationStatus {

    DEFAULT(0, "缺省"),
    AGREE(1, "同意"),
    REFUSE(2, "拒绝"),
    Done(3, "完成");

    int value;
    String label;

    NotificationStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }

    public static NotificationStatus valueOf(int value) {
        switch (value) {
            case 1:
                return AGREE;
            case 2:
                return REFUSE;
            case 3:
                return Done;
            default:
                return DEFAULT;
        }
    }

}
