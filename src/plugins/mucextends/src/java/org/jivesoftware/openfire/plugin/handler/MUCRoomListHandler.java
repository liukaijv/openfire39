package org.jivesoftware.openfire.plugin.handler;

import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.spi.LocalMUCRoom;
import org.jivesoftware.openfire.muc.spi.MultiUserChatServiceImpl;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.dao.MUCDao;
import org.jivesoftware.openfire.plugin.dao.RoomInfo;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

public class MUCRoomListHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCRoomInfoHandler.class);

    public MUCRoomListHandler() {
        super("group Roster Handler");
        server = XMPPServer.getInstance();
        // 自定义的xmmp iq查询协议
        info = new IQHandlerInfo("query", Const.IM_NS_MY_GROUPS);
    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
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
            List<RoomInfo> userMucs = MUCDao.getUserMucs(userJid.toBareJID());

            // 数据为空
            if (userMucs == null || userMucs.isEmpty()) {
                reply.setChildElement(rootEle);
                return reply;
            }

            for (RoomInfo roomInfo : userMucs) {

                String serviceID = String.valueOf(roomInfo.getServiceID());
                MultiUserChatServiceImpl mucService = (MultiUserChatServiceImpl) server.getMultiUserChatManager().getMultiUserChatService(Long.parseLong(serviceID));
                String roomName = roomInfo.getName();
                LocalMUCRoom room = (LocalMUCRoom) mucService.getChatRoom(roomName);

                if (room == null) {
                    continue;
                }

//            map.put("serviceID", resultSet.getString(1));
//            map.put("roomID", resultSet.getString(2));
//            map.put("name", resultSet.getString(3));
//            map.put("naturalName", resultSet.getString(4));
//            map.put("description", resultSet.getString(5));
//            map.put("maxUsers", resultSet.getString(6));
//            map.put("creationDate", resultSet.getString(7));
//            map.put("userJid", resultSet.getString(8));
//            map.put("nickname", resultSet.getString(9));
//            map.put("affiliation", resultSet.getString(10));

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

            reply.setChildElement(rootEle);
            LOGGER.info("发送packet：" + reply.toXML());
            return reply;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            reply.setError(PacketError.Condition.internal_server_error);
            return reply;
        }
    }

}
