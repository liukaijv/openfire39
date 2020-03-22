package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.dao.MUCNotificationDao;
import org.jivesoftware.openfire.plugin.service.NotificationStore;
import org.jivesoftware.openfire.plugin.service.NotificationStoreOfflineMessageImpl;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class MUCDeleteNotificationsHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private NotificationStore notificationStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCDeleteNotificationsHandler.class);

    public MUCDeleteNotificationsHandler() {
        super("MUCDeleteNotificationsHandler");

        info = new IQHandlerInfo("query", Const.IM_NS_DELETE_NOTIFICATION);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
        if (notificationStore == null) {
            notificationStore = new NotificationStoreOfflineMessageImpl(server.getOfflineMessageStore());
        }

        IQ reply = IQ.createResultIQ(packet);
        reply.setChildElement(packet.getChildElement().createCopy());
        reply.setFrom(packet.getFrom());

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

            Element packetElement = packet.getElement();
            Element query = packetElement.element("query");
            Element idEle = query.element("id");

            if (idEle == null || StringUtils.isEmpty(idEle.getText())) {
                LOGGER.error("ID未传：");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            // 删除信息
            MUCNotificationDao.deleteNotification(Long.valueOf(idEle.getText()));

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