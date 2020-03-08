package org.jivesoftware.openfire.plugin.handler;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.dao.NotificationDao;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.plugin.service.NotificationStore;
import org.jivesoftware.openfire.plugin.service.NotificationStoreOfflineMessageImpl;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.Collection;
import java.util.List;

public class MUCGetNotificationsHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private NotificationStore notificationStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCGetNotificationsHandler.class);

    public MUCGetNotificationsHandler() {
        super("MUCGetNotificationsHandler");

        info = new IQHandlerInfo("query", Const.IM_NS_GET_NOTIFICATIONS);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
        if (notificationStore == null) {
            notificationStore = new NotificationStoreOfflineMessageImpl(server.getOfflineMessageStore());
        }

        IQ reply = IQ.createResultIQ(packet);
        reply.setFrom(packet.getFrom());

        Element rootEle = DocumentHelper.createElement("items");
        Namespace namespace = new Namespace("", info.getNamespace());
        rootEle.add(namespace);

        try {

            // 未登录
            ClientSession session = sessionManager.getSession(packet.getFrom());
            if (session == null) {
                LOGGER.error("Error during userInfo. Session not found in " +
                        sessionManager.getPreAuthenticatedKeys() +
                        " for key " +
                        packet.getFrom());
                // This error packet will probably won't make it through
                reply.setError(PacketError.Condition.not_authorized);
                return reply;
            }

            JID userJid = packet.getFrom();

            List<MucNotification> notifications = NotificationDao.getNotifications(userJid.getNode());

            for (MucNotification notification : notifications) {

                Element itemEle = rootEle.addElement("item");
                itemEle.addAttribute("id", String.valueOf(notification.getId()));
                itemEle.addAttribute("username", notification.getUsername());
                itemEle.addAttribute("from", notification.getFrom());
                itemEle.addAttribute("to", notification.getTo());
                itemEle.addAttribute("type", String.valueOf(notification.getType()));
                itemEle.addAttribute("status", String.valueOf(notification.getType()));
                itemEle.addAttribute("createAt", String.valueOf(notification.getCreateAt()));
                itemEle.addAttribute("updateAt", String.valueOf(notification.getUpdateAt()));
            }

            reply.setChildElement(rootEle);
            LOGGER.info("发送packet：" + reply.toXML());
            return reply;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            reply.setError(PacketError.Condition.internal_server_error);
            return reply;
        }

    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

}