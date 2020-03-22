package org.jivesoftware.openfire.plugin.listener;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.openfire.muc.spi.MultiUserChatServiceImpl;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.MucUtils;
import org.jivesoftware.openfire.plugin.dao.MUCDao;
import org.jivesoftware.openfire.plugin.dao.MUCNotificationDao;
import org.jivesoftware.openfire.plugin.dao.RoomInfo;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
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
    public void sessionCreated(final Session session) {

        LOGGER.info("用户已连接");

        // 推送通知
        pushNotification(session);

        // 推送加入群
        pushRooms(session);

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

    private void pushNotification(final Session session) {

        final String username = session.getAddress().getNode();
        final List<MucNotification> notifications = MUCNotificationDao.getNotifications(username);

        LOGGER.info("群组申请消息条数：" + notifications.size());
        if (notifications.size() > 0) {

            TimerTask messageTask = new TimerTask() {
                @Override
                public void run() {
                    for (MucNotification notification : notifications) {
                        Message message = MucUtils.notificationToMessage(session.getAddress(), notification);
                        LOGGER.info("申请消息：" + message.toXML());
                        router.route(message);
                    }
                }
            };

            TaskEngine.getInstance().schedule(messageTask, 3000);
        }
    }

    private void joinRooms(final Session session) {
//        List<RoomInfo> userMucs = MUCDao.getUserMucs(session.getAddress().getNode());
//
//        if(userMucs!=null){
//            for(RoomInfo roomInfo: userMucs){
//                String serviceID = String.valueOf(roomInfo.getServiceID());
//                MultiUserChatServiceImpl mucService = (MultiUserChatServiceImpl) XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(Long.parseLong(serviceID));
//                String roomName = roomInfo.getName();
//                LocalMUCRoom room = (LocalMUCRoom) mucService.getChatRoom(roomName);
//                if(room!=null){
//                    room.joinRoom(session.getAddress().getNode(),"",null,null,null);
//                }
//            }
//        }

    }

    private void pushRooms(final Session session) {

        TimerTask messageTask = new TimerTask() {
            @Override
            public void run() {

                List<RoomInfo> userMucs = MUCDao.getUserMucs(session.getAddress().toBareJID());

                if (userMucs == null || userMucs.isEmpty()) {
                    return;
                }

                LOGGER.info("加入的群组数量：" + userMucs.size());

                JID userJid = session.getAddress();
                JID fromJid = new JID(userJid.getNode(), userJid.getDomain(), "server_custom_push");

                IQ iq = new IQ(IQ.Type.result);
                iq.setFrom(fromJid);
                iq.setTo(userJid);

                Element rootEle = DocumentHelper.createElement("items");
                Namespace namespace = new Namespace("", Const.IM_NS_MY_GROUPS);
                rootEle.add(namespace);

                for (RoomInfo roomInfo : userMucs) {

                    String serviceID = String.valueOf(roomInfo.getServiceID());
                    MultiUserChatServiceImpl mucService = (MultiUserChatServiceImpl) XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(Long.parseLong(serviceID));
                    String roomName = roomInfo.getName();
                    LocalMUCRoom room = (LocalMUCRoom) mucService.getChatRoom(roomName);

                    if (room == null) {
                        continue;
                    }

                    // 群信息
                    Element itemEle = rootEle.addElement("item");
                    itemEle.addAttribute("id", String.valueOf(roomInfo.getRoomID()));
                    itemEle.addAttribute("name", roomInfo.getName());
                    itemEle.addAttribute("naturalName", roomInfo.getNaturalName());
                    itemEle.addAttribute("jid", room.getJID().toBareJID());
                    itemEle.addAttribute("description", roomInfo.getDescription());
                    itemEle.addAttribute("maxUsers", String.valueOf(roomInfo.getMaxUsers()));
                    itemEle.addAttribute("creationDate", roomInfo.getCreationDate());
                    itemEle.addAttribute("userJid", roomInfo.getUserJid());
                    itemEle.addAttribute("nickname", roomInfo.getNickname());
                    itemEle.addAttribute("affiliation", String.valueOf(roomInfo.getAffiliation()));
                    itemEle.addAttribute("cardId", String.valueOf(roomInfo.getCardId()));

                }

                iq.setChildElement(rootEle);

                LOGGER.info("加入的群组：" + iq.toXML());

                session.process(iq);

            }
        };

        TaskEngine.getInstance().schedule(messageTask, 500);
    }

}
