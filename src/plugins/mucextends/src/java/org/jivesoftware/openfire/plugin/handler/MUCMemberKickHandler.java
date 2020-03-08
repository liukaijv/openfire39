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
import org.jivesoftware.openfire.plugin.Utils;
import org.jivesoftware.openfire.plugin.dao.NotificationDao;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.plugin.model.NotificationStatus;
import org.jivesoftware.openfire.plugin.model.NotificationType;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.WebManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

public class MUCMemberKickHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCMemberKickHandler.class);

    private IQHandlerInfo info;
    private XMPPServer server;
    OfflineMessageStore offlineMessageStore;

    public MUCMemberKickHandler() {
        super("member kick");
        info = new IQHandlerInfo("query", Const.IM_NS_KICK_MEMBER);
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
                Element userJIDEle = query.element("userJID");
                if (roomJIDEle == null || StringUtils.isEmpty(roomJIDEle.getText()) ||
                        userJIDEle == null || StringUtils.isEmpty(userJIDEle.getText())) {
                    reply.setError(PacketError.Condition.bad_request);
                    return reply;
                }

                WebManager webManager = new WebManager();
                JID roomJID = new JID(roomJIDEle.getText());
                JID userJID = new JID(userJIDEle.getText());

                String roomName = roomJID.getNode();
                MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

                if (!hasPermit(packet.getFrom(), userJID, room)) {
                    reply.setError(PacketError.Condition.forbidden);
                    return reply;
                }

                // Remove the user from the allowed list
                IQ iq = new IQ(IQ.Type.set);
                Element frag = iq.setChildElement("query", "http://jabber.org/protocol/muc#admin");
                Element item = frag.addElement("item");
                item.addAttribute("affiliation", "none");
                item.addAttribute("jid", userJID.toBareJID());
                room.getIQOwnerHandler().handleIQ(iq, room.getRole());

                // 发送消息给被踢人
                MucNotification notification = new MucNotification();
                notification.setRoomJID(roomJID.toBareJID());
                notification.setType(NotificationType.KICK.getValue());
                notification.setFrom(packet.getFrom().toBareJID());
                notification.setTo(userJID.toBareJID());
                notification.setUsername(userJID.getNode());
                notification.setStatus(NotificationStatus.Done.getValue());

                NotificationDao.saveNotification(notification);

                // 发消息给用户/状态为0让客户端确认
                notification.setStatus(NotificationStatus.DEFAULT.getValue());
                Utils.pushNotificationToUser(userJID, notification);

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

    private boolean hasPermit(JID operator, JID target, MUCRoom room) {
        return room.getAffiliation(operator).getValue() < room.getAffiliation(target).getValue();
    }

    private void broadcastToMembers(MUCRoom room) {

    }
}