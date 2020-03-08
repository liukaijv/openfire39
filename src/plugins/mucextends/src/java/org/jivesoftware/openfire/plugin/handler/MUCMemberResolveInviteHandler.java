package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.Utils;
import org.jivesoftware.openfire.plugin.dao.NotificationDao;
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

public class MUCMemberResolveInviteHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private NotificationStore notificationStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCMemberResolveInviteHandler.class);

    public MUCMemberResolveInviteHandler() {
        super("MUCMemberResolveInviteHandler");

        info = new IQHandlerInfo("query", Const.IM_NS_MEMBER_RESOLVE_INVITE);
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
            Element statusEle = query.element("status");

            if (idEle == null || StringUtils.isEmpty(idEle.getText())) {
                LOGGER.error("ID未传：");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            if (statusEle == null || StringUtils.isEmpty(statusEle.getText())) {
                LOGGER.error("审核状态未传");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            long id = Long.valueOf(idEle.getText());

            MucNotification notification = NotificationDao.getNotificationById(id, NotificationType.INVITE.getValue());
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

            JID userJID = packet.getFrom();
            JID roomJID = new JID(notification.getRoomJID());
            String roomName = roomJID.getNode();

            WebManager webManager = new WebManager();
            MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

            if (room == null) {
                LOGGER.error("群组不存在：" + roomJID.toBareJID());
                reply.setError(PacketError.Condition.service_unavailable);
                return reply;
            }

            if (Utils.hasJoinedRoom(room, userJID)) {
                LOGGER.error("已经加入：" + userJID.toBareJID());
                reply.setError(PacketError.Condition.conflict);
                if (notification.getStatus() != NotificationStatus.AGREE.getValue()) {
                    notification.setStatus(NotificationStatus.AGREE.getValue());
                    NotificationDao.updateNotificationStatus(notification);
                }
                return reply;
            }

            NotificationStatus status;

            if ("1".equals(statusEle.getText())) {
                status = NotificationStatus.AGREE;
            } else if ("2".equals(statusEle.getText())) {
                status = NotificationStatus.REFUSE;
            } else {
                LOGGER.error("审核状态不对");
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }
            // 同意申请
            if (status == NotificationStatus.AGREE) {
                Utils.addMemberToRoom(room, userJID);
            }

            // 更新状态
            notification.setStatus(status.getValue());
            NotificationDao.updateNotificationStatus(notification);
            Utils.pushNotificationToUser(packet.getFrom(), notification);

            // 储存通知
            JID owner = Utils.getOwner(room);
            notification.setFrom(userJID.toBareJID());
            notification.setTo(owner.toBareJID());
            notification.setUsername(owner.getNode());
            notification.setType(NotificationType.INVITE_RESULT.getValue());
            NotificationDao.saveNotification(notification);

            // 推送结果给群主
            Utils.pushNotificationToUser(owner, notification);

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