package org.jivesoftware.openfire.plugin.model;

public class MucNotification {

    private Long id;

    private int type;

    private String roomJID;

    private String from;

    private String to;

    private int status;

    private String username;

    private long createAt;

    private long updateAt;

    private boolean confirm;

    private String fromNickname;

    private String toNickName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRoomJID() {
        return roomJID;
    }

    public void setRoomJID(String roomJID) {
        this.roomJID = roomJID;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(long updateAt) {
        this.updateAt = updateAt;
    }

    public boolean isConfirm() {
        return confirm;
    }

    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }

    public String getFromNickname() {
        return fromNickname;
    }

    public void setFromNickname(String fromNickname) {
        this.fromNickname = fromNickname;
    }

    public String getToNickName() {
        return toNickName;
    }

    public void setToNickName(String toNickName) {
        this.toNickName = toNickName;
    }

    public MucNotification() {

    }

    MucNotification(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.roomJID = builder.roomJID;
        this.from = builder.from;
        this.to = builder.to;
        this.status = builder.status;
        this.username = builder.username;
        this.createAt = builder.createAt;
        this.updateAt = builder.updateAt;
        this.confirm = builder.confirm;
        this.fromNickname = builder.fromNickname;
        this.toNickName = builder.toNickName;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {

        private Long id;

        private int type;

        private String roomJID;

        private String from;

        private String to;

        private int status;

        private String username;

        private long createAt;

        private long updateAt;

        private boolean confirm;

        private String fromNickname;

        private String toNickName;

        public Builder() {

        }

        Builder(MucNotification notification) {
            this.id = notification.id;
            this.type = notification.type;
            this.roomJID = notification.roomJID;
            this.from = notification.from;
            this.to = notification.to;
            this.status = notification.status;
            this.username = notification.username;
            this.createAt = notification.createAt;
            this.updateAt = notification.updateAt;
            this.confirm = notification.confirm;
            this.fromNickname = notification.fromNickname;
            this.toNickName = notification.toNickName;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setRoomJID(String roomJID) {
            this.roomJID = roomJID;
            return this;
        }

        public Builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder setTo(String to) {
            this.to = to;
            return this;
        }

        public Builder setStatus(int status) {
            this.status = status;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setCreateAt(long createAt) {
            this.createAt = createAt;
            return this;
        }

        public Builder setUpdateAt(long updateAt) {
            this.updateAt = updateAt;
            return this;
        }

        public Builder setConfirm(boolean confirm) {
            this.confirm = confirm;
            return this;
        }

        public Builder setFromNickname(String fromNickname) {
            this.fromNickname = fromNickname;
            return this;
        }

        public Builder setToNickName(String toNickName) {
            this.toNickName = toNickName;
            return this;
        }

        public MucNotification build() {
            return new MucNotification(this);
        }

    }
}
