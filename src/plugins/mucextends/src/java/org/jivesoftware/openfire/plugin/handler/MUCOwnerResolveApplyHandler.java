package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.MucUtils;
import org.jivesoftware.openfire.plugin.dao.MUCNotificationDao;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.plugin.model.NotificationStatus;
import org.jivesoftware.openfire.plugin.model.NotificationType;
import org.jivesoftware.openfire.plugin.service.NotificationStore;
import org.jivesoftware.openfire.plugin.service.NotificationStoreOfflineMessageImpl;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.WebManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.Collection;

public class MUCOwnerResolveApplyHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private NotificationStore notificationStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCOwnerResolveApplyHandler.class);

    public MUCOwnerResolveApplyHandler() {
        super("MUCOwnerResolveApplyHandler");

        info = new IQHandlerInfo("query", Const.IM_NS_OWNER_RESOLVE_APPLY);
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

        try {

            // 未登录
            ClientSession session = sessionManager.getSession(packet.getFrom());
            if (session == null) {
                LOGGER.info("Error during userInfo. Session not found in " +
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
            Element statusEle = query.element("status");

            if (idEle == null || StringUtils.isEmpty(idEle.getText())) {
                LOGGER.info("ID未传");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            if (statusEle == null || StringUtils.isEmpty(statusEle.getText())) {
                LOGGER.info("审核状态未传");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            Long id = Long.valueOf(idEle.getText());

            MucNotification notification = MUCNotificationDao.getNotificationById(id);
            if (notification == null) {
                LOGGER.info("申请不存在：" + id);
                reply.setError(PacketError.Condition.item_not_found);
                return reply;
            }

            if (notification.getStatus() > 0) {
                LOGGER.info("申请已经处理：" + id);
                reply.setError(PacketError.Condition.gone);
                return reply;
            }

            JID userJID = new JID(notification.getFrom());
            JID fromJID = packet.getFrom();
            JID roomJID = new JID(notification.getRoomJID());
            String roomName = roomJID.getNode();

            WebManager webManager = new WebManager();
            MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

            if (room == null) {
                LOGGER.info("群组不存在：" + roomJID.toBareJID());
                reply.setError(PacketError.Condition.service_unavailable);
                return reply;
            }

            if (!hasPermit(room, fromJID)) {
                LOGGER.info("你没没有审核权限：" + fromJID.toBareJID());
                reply.setError(PacketError.Condition.not_allowed);
                return reply;
            }

            if (MucUtils.hasJoinedRoom(room, userJID)) {
                if (notification.getStatus() != NotificationStatus.AGREE.getValue()) {
                    notification.setStatus(NotificationStatus.AGREE.getValue());
                    MUCNotificationDao.updateNotificationStatus(notification);
                }
                LOGGER.error("已经加入：" + userJID.toBareJID());
                reply.setError(PacketError.Condition.conflict);
                return reply;
            }

            NotificationStatus status;

            if ("1".equals(statusEle.getText())) {
                status = NotificationStatus.AGREE;
            } else if ("2".equals(statusEle.getText())) {
                status = NotificationStatus.REFUSE;
            } else {
                LOGGER.info("审核状态不对：" + statusEle.getText());
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            if (status == NotificationStatus.AGREE) {
                MucUtils.addMemberToRoom(room, userJID);
            }

            // 更新状态
            MucNotification updatedNotification = notification.newBuilder().setStatus(status.getValue()).build();
            MUCNotificationDao.updateNotificationStatus(updatedNotification);
            MucUtils.pushNotificationToUser(fromJID, updatedNotification, 500);

            // 储存通知
            MucNotification userNotification = updatedNotification.newBuilder()
                    .setFrom(fromJID.toBareJID())
                    .setUsername(userJID.getNode())
                    .setTo(userJID.toBareJID())
                    .setType(NotificationType.APPLY_RESULT.getValue())
                    .setUpdateAt(System.currentTimeMillis())
                    .setStatus(status.getValue())
                    .setConfirm(false)
                    .build();
            userNotification = MUCNotificationDao.saveNotification(userNotification);

            // 通知用户
            MucUtils.pushNotificationToUser(userJID, userNotification);

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

    private boolean hasPermit(MUCRoom room, JID userJid) {
        Collection<JID> owners = room.getOwners();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(userJid.toBareJID())) {
                return true;
            }
        }
        return false;
    }

}