package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.SessionManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import java.util.Collection;

public class MUCAddMemberHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCAddMemberHandler.class);

    private IQHandlerInfo info;

    public MUCAddMemberHandler() {
        super("add member");
        info = new IQHandlerInfo("query", Const.IM_NS_ADD_MEMBER);
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

            if (IQ.Type.get.equals(packet.getType())) {
                throw new RuntimeException("no interface to get it ");
            } else if (IQ.Type.set.equals(packet.getType())) {

                Element packetElement = packet.getElement();
                Element query = packetElement.element("query");
                Element roomJIDEle = query.element("roomJID");
                Element userJIDEle = query.element("userJID");
                if (roomJIDEle == null
                        || userJIDEle == null
                        || StringUtils.isEmpty(roomJIDEle.getText())
                        || StringUtils.isEmpty(roomJIDEle.getText())) {
                    reply.setError(PacketError.Condition.bad_request);
                    return reply;
                }

                WebManager webManager = new WebManager();
                JID roomJID = new JID(roomJIDEle.getText());
                String roomName = roomJID.getNode();
                MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

                if (room == null) {
                    LOGGER.info("房间不存在：" + roomJID.toBareJID());
                    reply.setError(PacketError.Condition.item_not_found);
                    return reply;
                }

                boolean isOwner = false;
                for (JID jid : room.getOwners()) {
                    LOGGER.info("房间拥有人：" + jid.toBareJID());
                    if (jid.toBareJID().equals(packet.getFrom().toBareJID())) {
                        isOwner = true;
                        break;
                    }
                }
                if (!isOwner) {
                    LOGGER.info("你没有权限：" + packet.getFrom().toBareJID());
                    reply.setError(PacketError.Condition.not_acceptable);
                    return reply;
                }

//                IQ mucIq = new IQ(IQ.Type.set);
//                Element frag = mucIq.setChildElement("query", "http://jabber.org/protocol/muc#admin");
//                Element item = frag.addElement("item");
//                item.addAttribute("affiliation", "member");
//                item.addAttribute("jid", userJIDEle.getText());
//                // Send the IQ packet that will modify the room's
//                // configuration
//                room.getIQAdminHandler().handleIQ(mucIq, room.getRole());

//                // 加成员客户端不能聊天，直接加成管理员
//                IQ iq = new IQ(IQ.Type.set);
//                Element frag = iq.setChildElement("query", "http://jabber.org/protocol/muc#owner");
//                Element item = frag.addElement("item");
//                item.addAttribute("affiliation", "admin");
//                item.addAttribute("jid", userJIDEle.getText());
//                // Send the IQ packet that will modify the room's configuration
//                room.getIQOwnerHandler().handleIQ(iq, room.getRole());

                String userJIDEleText = userJIDEle.getText();
                JID userJID = new JID(userJIDEleText);

                // 已经加入了
                if (Utils.hasJoinedRoom(room, userJID)) {
                    LOGGER.info("用户已经加入了：" + userJID.toBareJID());
                    reply.setError(PacketError.Condition.conflict);
                    return reply;
                }

                MucNotification notification = NotificationDao.getNotification(userJID.getNode(), roomJID.toBareJID(), NotificationType.INVITE.getValue());
                if (notification != null) {
                    if (notification.getStatus() == NotificationStatus.DEFAULT.getValue()) {
                        LOGGER.info("用户有申请未处理：" + userJID.toBareJID());
                        reply.setError(PacketError.Condition.gone);
                        return reply;
                    } else {
                        LOGGER.info("删除旧记录：" + notification.getId());
//                        NotificationDao.deleteNotification(notification.getId());
                    }
                }

                notification = new MucNotification();
                notification.setRoomJID(roomJID.toBareJID());
                notification.setType(NotificationType.INVITE.getValue());
                notification.setFrom(packet.getFrom().toBareJID());
                notification.setTo(userJID.toBareJID());
                notification.setUsername(userJID.getNode());

                LOGGER.info("数据库插入申请：" + userJID.getNode());
                MucNotification savedNotification = NotificationDao.saveNotification(notification);

                // 推送给用户
                Utils.pushNotificationToUser(userJID, savedNotification);

            }

        } catch (Exception e) {
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

}