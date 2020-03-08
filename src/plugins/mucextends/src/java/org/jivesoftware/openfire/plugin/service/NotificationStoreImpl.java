package org.jivesoftware.openfire.plugin.service;

import org.jivesoftware.openfire.plugin.model.MucNotification;

import java.util.Collection;

public class NotificationStoreImpl implements NotificationStore {

    @Override
    public Collection<MucNotification> getNotifications(String userName) {
        return null;
    }

    @Override
    public MucNotification getNotification(Integer primaryKey) {
        return null;
    }

    @Override
    public void deleteNotifications(String userName) {

    }

    @Override
    public void deleteNotification(Integer primaryKey) {

    }
}
