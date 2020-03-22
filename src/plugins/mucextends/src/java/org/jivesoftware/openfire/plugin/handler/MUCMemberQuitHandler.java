package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.OfflineMessageStore;
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
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.WebManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

public class MUCMemberQuitHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCMemberQuitHandler.class);

    private IQHandlerInfo info;
    private XMPPServer server;
    OfflineMessageStore offlineMessageStore;

    public MUCMemberQuitHandler() {
        super("member quit");
        info = new IQHandlerInfo("query", Const.IM_NS_MEMBER_QUIT);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
        offlineMessageStore = server.getOfflineMessageStore();

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

            if (IQ.Type.get.equals(packet.getType())) {
                throw new RuntimeException("no interface to get it ");
            } else if (IQ.Type.set.equals(packet.getType())) {

                Element packetElement = packet.getElement();
                Element query = packetElement.element("query");
                Element roomJIDEle = query.element("roomJID");
                if (roomJIDEle == null || StringUtils.isEmpty(roomJIDEle.getText())) {
                    reply.setError(PacketError.Condition.bad_request);
                    return reply;
                }

                JID userJID = packet.getFrom();

                WebManager webManager = new WebManager();
                JID roomJID = new JID(roomJIDEle.getText());
                String roomName = roomJID.getNode();
                MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

                if (!hasPermit(userJID, room)) {
                    reply.setError(PacketError.Condition.forbidden);
                    return reply;
                }

                // Remove the user from the allowed list
                IQ iq = new IQ(IQ.Type.set);
                Element frag = iq.setChildElement("query", "http://jabber.org/protocol/muc#admin");
                Element item = frag.addElement("item");
                item.addAttribute("affiliation", "none");
                item.addAttribute("jid", packet.getFrom().toBareJID());
                room.getIQOwnerHandler().handleIQ(iq, room.getRole());

                JID ownerJID = MucUtils.getOwner(room);

                MucNotification notification = new MucNotification();
                notification.setRoomJID(roomJID.toBareJID());
                notification.setType(NotificationType.QUIT.getValue());
                notification.setStatus(NotificationStatus.Done.getValue());
                notification.setFrom(userJID.toBareJID());
                notification.setTo(ownerJID.toBareJID());
                notification.setUsername(ownerJID.getNode());
                notification.setUpdateAt(System.currentTimeMillis());

                MUCNotificationDao.saveNotification(notification);

                // 发消息给群主
                MucUtils.pushNotificationToUser(ownerJID, notification);

                // 离开房间
                if (room.hasOccupant(userJID.getNode())) {
                    room.leaveRoom(room.getOccupantByFullJID(userJID));
                }

                // 通知在线成员
                broadcastToMembers(room);

            }

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

    // 成员才有权限
    private boolean hasPermit(JID operator, MUCRoom room) {
        for (JID jid : room.getAdmins()) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return true;
            }
        }

        for (JID jid : room.getMembers()) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return true;
            }
        }

        for (JID jid : room.getOutcasts()) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return true;
            }
        }
        return false;
    }

    private void broadcastToMembers(MUCRoom room) {

    }
}