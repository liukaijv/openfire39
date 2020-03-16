package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.model.MemberInfo;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.WebManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.util.ArrayList;
import java.util.List;

import static org.jivesoftware.openfire.plugin.MucUtils.getNickname;

public class MUCMemberListHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private UserManager userManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCMemberListHandler.class);

    public MUCMemberListHandler() {
        super("get room members");

        info = new IQHandlerInfo("query", Const.IM_NS_GROUP_MEMBERS);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

        server = XMPPServer.getInstance();
        userManager = server.getUserManager();
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
            Element packetElement = packet.getElement();
            Element query = packetElement.element("query");
            Element roomJIDEle = query.element("roomJID");

            if (roomJIDEle == null || StringUtils.isEmpty(roomJIDEle.getText())) {
                LOGGER.error("未传入roomJID");
                reply.setError(PacketError.Condition.forbidden);
                return reply;
            }

            WebManager webManager = new WebManager();
            JID roomJID = new JID(roomJIDEle.getText());
            String roomName = roomJID.getNode();
            MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);

            if (room == null) {
                LOGGER.error("房间不存在");
                reply.setError(PacketError.Condition.item_not_found);
                return reply;
            }


            List<MemberInfo> memberInfos = getRoomMembers(room, userJid);

            for (MemberInfo memberInfo : memberInfos) {

                // 成员信息
                Element itemEle = rootEle.addElement("item");

                itemEle.addAttribute("roomJID", memberInfo.getRoomJID());
                itemEle.addAttribute("userJID", memberInfo.getUserJID());
                itemEle.addAttribute("userNickName", memberInfo.getUserNickName());
                itemEle.addAttribute("affiliation", String.valueOf(memberInfo.getAffiliation()));
                itemEle.addAttribute("myAffiliation", String.valueOf(memberInfo.getMyAffiliation()));

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

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }

    public List<MemberInfo> getRoomMembers(MUCRoom room, JID myJID) {

        List<MemberInfo> members = new ArrayList<MemberInfo>();

        for (JID jid : room.getMembers()) {
            MemberInfo memberInfo = new MemberInfo();
            memberInfo.setRoomJID(room.getJID().toBareJID());
            memberInfo.setUserJID(jid.toBareJID());
            memberInfo.setAffiliation(MUCRole.Affiliation.member.getValue());
            memberInfo.setMyAffiliation(room.getAffiliation(myJID).getValue());
            memberInfo.setUserNickName(getNickname(jid.getNode()));
            members.add(memberInfo);
        }

        for (JID jid : room.getAdmins()) {
            MemberInfo memberInfo = new MemberInfo();
            memberInfo.setRoomJID(room.getJID().toBareJID());
            memberInfo.setUserJID(jid.toBareJID());
            memberInfo.setAffiliation(MUCRole.Affiliation.admin.getValue());
            memberInfo.setMyAffiliation(room.getAffiliation(myJID).getValue());
            memberInfo.setUserNickName(getNickname(jid.getNode()));
            members.add(memberInfo);
        }

        for (JID jid : room.getOwners()) {
            MemberInfo memberInfo = new MemberInfo();
            memberInfo.setRoomJID(room.getJID().toBareJID());
            memberInfo.setUserJID(jid.toBareJID());
            memberInfo.setAffiliation(MUCRole.Affiliation.owner.getValue());
            memberInfo.setMyAffiliation(room.getAffiliation(myJID).getValue());
            memberInfo.setUserNickName(getNickname(jid.getNode()));
            members.add(memberInfo);
        }

        for (JID jid : room.getOutcasts()) {
            MemberInfo memberInfo = new MemberInfo();
            memberInfo.setRoomJID(room.getJID().toBareJID());
            memberInfo.setUserJID(jid.toBareJID());
            memberInfo.setAffiliation(MUCRole.Affiliation.outcast.getValue());
            memberInfo.setMyAffiliation(room.getAffiliation(myJID).getValue());
            memberInfo.setUserNickName(getNickname(jid.getNode()));
            members.add(memberInfo);
        }

        return members;
    }

    // 去找个昵称出来
    private User getUser(String userName) {
        try {
            return userManager.getUser(userName);
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}