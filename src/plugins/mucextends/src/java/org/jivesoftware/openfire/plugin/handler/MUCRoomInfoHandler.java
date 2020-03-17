package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
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
import org.jivesoftware.openfire.plugin.IdHash;
import org.jivesoftware.openfire.plugin.MucUtils;
import org.jivesoftware.openfire.plugin.dao.MUCDao;
import org.jivesoftware.openfire.plugin.dao.RoomInfo;
import org.jivesoftware.openfire.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.regex.Pattern;

import static org.jivesoftware.openfire.plugin.MucUtils.getNickname;

public class MUCRoomInfoHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCRoomInfoHandler.class);

    public MUCRoomInfoHandler() {
        super("get room info");

        info = new IQHandlerInfo("query", Const.IM_NS_GROUP_INFO);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
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

            JID userJid = packet.getFrom();
            Element packetElement = packet.getElement();
            Element query = packetElement.element("query");
            Element roomNameEle = query.element("roomName");

            if (roomNameEle == null || StringUtils.isEmpty(roomNameEle.getText())) {
                LOGGER.error("未传入roomName");
                reply.setError(PacketError.Condition.forbidden);
                return reply;
            }

            RoomInfo roomInfo = MUCDao.getUserMuc(userJid.toBareJID(), roomNameEle.getText());

            // 需求增加按id查找
            if (roomInfo == null) {
                LOGGER.info("偿试ID方式查找");
                try {
                    if (Pattern.matches("^[1-9]\\d*", roomNameEle.getText())) {
                        long id = Long.valueOf(roomNameEle.getText());
                        LOGGER.info("ID为：" + id);
                        if (id > 0) {
                            roomInfo = MUCDao.getMucById(IdHash.decode(id));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (roomInfo == null) {
                LOGGER.error("群组不存在");
                reply.setError(PacketError.Condition.item_not_found);
                return reply;
            }

            String serviceID = String.valueOf(roomInfo.getServiceID());
            MultiUserChatServiceImpl mucService = (MultiUserChatServiceImpl) server.getMultiUserChatManager().getMultiUserChatService(Long.parseLong(serviceID));
            String roomName = roomInfo.getName();
            LocalMUCRoom room = (LocalMUCRoom) mucService.getChatRoom(roomName);

            // 群信息
            Element itemEle = DocumentHelper.createElement("item");
            Namespace namespace = new Namespace("", info.getNamespace());
            itemEle.add(namespace);

            int usersCount = room.getAdmins().size() + room.getMembers().size() + room.getOwners().size() + room.getOutcasts().size();

            itemEle.addAttribute("id", String.valueOf(roomInfo.getRoomID()));
            itemEle.addAttribute("name", roomInfo.getName());
            itemEle.addAttribute("naturalName", roomInfo.getNaturalName());
            itemEle.addAttribute("jid", room.getJID().toBareJID());
            itemEle.addAttribute("description", roomInfo.getDescription());
            itemEle.addAttribute("maxUsers", String.valueOf(roomInfo.getMaxUsers()));
            itemEle.addAttribute("creationDate", roomInfo.getCreationDate());
            itemEle.addAttribute("userJid", roomInfo.getUserJid());
            itemEle.addAttribute("nickname", roomInfo.getNickname());
//            itemEle.addAttribute("affiliation", String.valueOf(roomInfo.getAffiliation()));
            int affiliation = MucUtils.getUserAffiliation(room, userJid);
            itemEle.addAttribute("affiliation", String.valueOf(affiliation));
            itemEle.addAttribute("usersCount", String.valueOf(usersCount));
            itemEle.addAttribute("cardId", String.valueOf(roomInfo.getCardId()));

            JID oneOwner = MucUtils.getOwner(room);
            itemEle.addAttribute("creator", getNickname(oneOwner.getNode()));
            itemEle.addAttribute("creatorJID", oneOwner.toBareJID());

            reply.setChildElement(itemEle);
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