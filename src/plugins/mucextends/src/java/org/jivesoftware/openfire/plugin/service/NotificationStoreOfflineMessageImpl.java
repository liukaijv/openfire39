package org.jivesoftware.openfire.plugin.service;

import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.plugin.model.MucNotification;

import java.util.Collection;

public class NotificationStoreOfflineMessageImpl implements NotificationStore {

    OfflineMessageStore offlineMessageStore;

    public NotificationStoreOfflineMessageImpl(OfflineMessageStore offlineMessageStore) {
        this.offlineMessageStore = offlineMessageStore;
    }

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
        offlineMessageStore.deleteMessages(userName);
    }

    @Override
    public void deleteNotification(Integer primaryKey) {

    }

}
