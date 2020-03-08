package org.jivesoftware.openfire.plugin.service;

import org.jivesoftware.openfire.plugin.model.MucNotification;

import java.util.Collection;

public interface NotificationStore {

    Collection<MucNotification> getNotifications(String userName);

    MucNotification getNotification(Integer primaryKey);

    void deleteNotifications(String userName);

    void deleteNotification(Integer primaryKey);

}
