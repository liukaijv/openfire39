package org.jivesoftware.openfire.plugin.handler;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.MucUtils;
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
import org.xmpp.packet.PacketError;

import java.util.Collection;

public class MUCMemberApplyHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCMemberApplyHandler.class);

    private IQHandlerInfo info;

    public MUCMemberApplyHandler() {
        super("member apply");
        info = new IQHandlerInfo("query", Const.IM_NS_MEMBER_APPLY);
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
                if (roomJIDEle == null) {
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

                JID userJID = packet.getFrom();
                if (!hasPermit(packet.getFrom(), room)) {
                    LOGGER.info("已经加入了：" + userJID.toBareJID());
                    reply.setError(PacketError.Condition.conflict);
                    return reply;
                }

//                IQ mucIq = new IQ(IQ.Type.set);
//                Element frag = mucIq.setChildElement("query", "http://jabber.org/protocol/muc#admin");
//                Element item = frag.addElement("item");
//                item.addAttribute("affiliation", "member");
//                item.addAttribute("jid", packet.getFrom().toBareJID());
//                // Send the IQ packet that will modify the room's
//                // configuration
//                room.getIQAdminHandler().handleIQ(mucIq, room.getRole());

                // 加成员客户端不能聊天，直接加成管理员
//                IQ iq = new IQ(IQ.Type.set);
//                Element frag = iq.setChildElement("query", "http://jabber.org/protocol/muc#owner");
//                Element item = frag.addElement("item");
//                item.addAttribute("affiliation", "admin");
//                item.addAttribute("jid", packet.getFrom().toBareJID());
//                // Send the IQ packet that will modify the room's configuration
//                room.getIQOwnerHandler().handleIQ(iq, room.getRole());

                JID ownerJID = MucUtils.getOwner(room);
                LOGGER.info("房间拥有人：" + ownerJID.toBareJID());

                MucNotification notification = NotificationDao.getNotification(ownerJID.getNode(), roomJID.toBareJID(), NotificationType.APPLY.getValue());
                if (notification != null) {
                    if (notification.getStatus() == NotificationStatus.DEFAULT.getValue()) {
                        LOGGER.info("有加入申请未处理");
                        reply.setError(PacketError.Condition.gone);
                        return reply;
                    } else {
                        LOGGER.info("删除旧记录：" + notification.getId());
//                        NotificationDao.deleteNotification(notification.getId());
                    }
                }

                notification = new MucNotification();
                notification.setRoomJID(roomJID.toBareJID());
                notification.setType(NotificationType.APPLY.getValue());
                notification.setFrom(packet.getFrom().toBareJID());
                notification.setTo(ownerJID.toBareJID());
                notification.setUsername(ownerJID.getNode());
                notification.setUpdateAt(System.currentTimeMillis());

                MucNotification savedNotification = NotificationDao.saveNotification(notification);

                LOGGER.info("推送申请给群主：" + ownerJID.toBareJID());
                MucUtils.pushNotificationToUser(ownerJID, savedNotification);

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

    // 没加入的人
    private boolean hasPermit(JID operator, MUCRoom room) {
        Collection<JID> owners = room.getMembers();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return false;
            }
        }
        owners = room.getOwners();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return false;
            }
        }
        owners = room.getAdmins();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return false;
            }
        }
        owners = room.getOutcasts();
        for (JID jid : owners) {
            if (jid.toBareJID().equals(operator.toBareJID())) {
                return false;
            }
        }
        return true;
    }

}