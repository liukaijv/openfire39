package org.jivesoftware.openfire.plugin.listener;

import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.plugin.Utils;
import org.jivesoftware.openfire.plugin.dao.NotificationDao;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;

import java.util.List;
import java.util.TimerTask;

public class SessionEventListenerImpl implements SessionEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(SessionEventListenerImpl.class);

    private MessageRouter router;

    public SessionEventListenerImpl(MessageRouter router) {
        this.router = router;
    }

    @Override
    public void sessionCreated(Session session) {

        LOGGER.info("用户已连接");

        String username = session.getAddress().getNode();
        final List<MucNotification> notifications = NotificationDao.getNotifications(username);

        LOGGER.info("群组申请消息条数：" + notifications.size());
        if (notifications.size() > 0) {

            TimerTask messageTask = new TimerTask() {
                @Override
                public void run() {
                    for (MucNotification notification : notifications) {
                        Message message = Utils.notificationToMessage(notification);
                        router.route(message);
                    }
                }
            };

            TaskEngine.getInstance().schedule(messageTask, 5000);
        }

    }

    @Override
    public void sessionDestroyed(Session session) {

    }

    @Override
    public void anonymousSessionCreated(Session session) {

    }

    @Override
    public void anonymousSessionDestroyed(Session session) {

    }

    @Override
    public void resourceBound(Session session) {

    }

}
