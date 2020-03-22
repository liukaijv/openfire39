package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.MucUtils;
import org.jivesoftware.openfire.plugin.dao.MUCNotificationDao;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.plugin.model.NotificationStatus;
import org.jivesoftware.openfire.plugin.model.NotificationType;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.WebManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

public class MUCRoomDestroyHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCRoomDestroyHandler.class);

    private IQHandlerInfo info;

    public MUCRoomDestroyHandler() {
        super("destroy room");
        info = new IQHandlerInfo("query", Const.IM_NS_DELETE_GROUP);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        IQ reply = IQ.createResultIQ(packet);
        reply.setChildElement(packet.getChildElement().createCopy());

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
            Element roomJIDEle = query.element("roomJID");
            Element reasonEle = query.element("reason");
            if (roomJIDEle == null || StringUtils.isEmpty(roomJIDEle.getText())) {
                reply.setError(PacketError.Condition.bad_request);
                return reply;
            }

            WebManager webManager = new WebManager();
            JID roomJID = new JID(roomJIDEle.getText());
            String roomName = roomJID.getNode();
            MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

            if (!hasPermit(packet.getFrom(), room)) {
                reply.setError(PacketError.Condition.forbidden);
                return reply;
            }

            // Delete the room
            if (room == null) {
                reply.setError(PacketError.Condition.item_not_found);
                return reply;
            }

            // 删除之前的成员信息
            List<JID> members = new ArrayList<JID>();
            for (JID jid : room.getMembers()) {
                members.add(new JID(jid.toBareJID()));
            }

            String alternateJIDString = packet.getFrom().toBareJID();
            String reason = reasonEle != null ? reasonEle.getText() : "";
            JID alternateJID = null;
            if (alternateJIDString != null && alternateJIDString.trim().length() > 0) {
                // OF-526: Ignore invalid alternative JIDs.
                try {
                    alternateJID = new JID(alternateJIDString.trim());
                    if (alternateJID.getNode() == null) {
                        alternateJID = null;
                    }
                } catch (IllegalArgumentException ex) {
                    alternateJID = null;
                }
            } else {
                alternateJID = null;
            }
            // If the room still exists then destroy it
            room.destroyRoom(alternateJID, reason);

            //  推送给成员
            broadcastToMembers(members, packet.getFrom(), roomJID);


        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            reply.setError(PacketError.Condition.internal_server_error);
            return reply;
        }

        LOGGER.info("发送packet：" + reply.toXML());
        return reply;
    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

    // 拥有者才有权限
    private boolean hasPermit(JID operator, MUCRoom room) {
        Collection<JID> owners = room.getOwners();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return true;
            }
        }
        return false;
    }

    private void broadcastToMembers(final List<JID> members, final JID from, final JID roomJID) {

        TimerTask messageTask = new TimerTask() {
            @Override
            public void run() {
                for (JID jid : members) {
                    MucNotification notification = new MucNotification();
                    notification.setRoomJID(roomJID.toBareJID());
                    notification.setType(NotificationType.DESTROY.getValue());
                    notification.setFrom(from.toBareJID());
                    notification.setTo(jid.toBareJID());
                    notification.setUsername(jid.getNode());
                    notification.setStatus(NotificationStatus.Done.getValue());
                    notification.setUpdateAt(System.currentTimeMillis());
                    MUCNotificationDao.saveNotification(notification);

                    //状态为0让用户处理
                    notification.setStatus(NotificationStatus.DEFAULT.getValue());
                    MucUtils.pushNotificationToUser(jid, notification);
                }
            }
        };

        TaskEngine.getInstance().schedule(messageTask, 3000);


    }
}