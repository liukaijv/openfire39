package org.jivesoftware.openfire.plugin.handler;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.forms.DataForm;
import org.jivesoftware.openfire.forms.FormField;
import org.jivesoftware.openfire.forms.spi.XDataFormImpl;
import org.jivesoftware.openfire.forms.spi.XFormFieldImpl;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.jivesoftware.openfire.muc.NotAllowedException;
import org.jivesoftware.openfire.plugin.Const;
import org.jivesoftware.openfire.plugin.dao.MUCDao;
import org.jivesoftware.openfire.plugin.dao.RoomInfo;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.WebManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import java.util.ArrayList;
import java.util.List;

public class MUCRoomCreateHandler extends IQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCRoomCreateHandler.class);

    private IQHandlerInfo info;

    public MUCRoomCreateHandler() {
        super("room create");
        info = new IQHandlerInfo("query", Const.IM_NS_CREATE_GROUP);
    }

    @Override
    public IQ handleIQ(IQ packet) throws UnauthorizedException {

        LOGGER.info("收到packet：" + packet.toXML());

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

            if (IQ.Type.get.equals(packet.getType())) {
                throw new RuntimeException("no interface to get it ");
            } else if (IQ.Type.set.equals(packet.getType())) {

                Element packetElement = packet.getElement();
                Element query = packetElement.element("query");
                Element roomNameEle = query.element("roomName");
                if (roomNameEle == null || StringUtils.isEmpty(roomNameEle.getText())
                ) {
                    reply.setError(PacketError.Condition.bad_request);
                    return reply;
                }

                JID userJid = packet.getFrom();
                List<RoomInfo> userMucs = MUCDao.getUserMucs(userJid.toBareJID());

                List<RoomInfo> ownMucs = new ArrayList<RoomInfo>();
                for (RoomInfo roomInfo : userMucs) {
                    if (roomInfo.getAffiliation() == MUCRole.Affiliation.owner.getValue()) {
                        ownMucs.add(roomInfo);
                    }
                }

                LOGGER.info("拥有房间数量: " + ownMucs.size());
                if (ownMucs.size() >= Const.MAX_ROOMS_ALLOW) {
                    //创建到达上限
                    reply.setError(PacketError.Condition.not_allowed);
                    return reply;
                }

                WebManager webManager = new WebManager();

                String roomName = roomNameEle.getText();
                String mucName = "";

                List<MultiUserChatService> services = webManager.getMultiUserChatManager().getMultiUserChatServices();
                for (MultiUserChatService service : services) {
                    if (service.isHidden()) {
                        // Private and hidden, skip it.
                        continue;
                    }
                    mucName = org.jivesoftware.util.StringUtils.escapeForXML(service.getServiceDomain());
                }

                LOGGER.info("services size: " + services.size());
                if (StringUtils.isEmpty(mucName)) {
                    // 服务未来开启
                    LOGGER.info("服务未来开启mucName: " + mucName);
                    reply.setError(PacketError.Condition.service_unavailable);
                    return reply;
                }

                JID roomJID = new JID(roomName, mucName, null);

                MUCRoom room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName);
                if (room != null) {
                    // 已经存在
                    reply.setError(PacketError.Condition.conflict);
                    return reply;
                } else {
                    // Try to create a new room
                    LOGGER.info("Try to create a new room ：" + roomJID.toBareJID());
                    JID address = packet.getFrom();
                    try {
                        room = webManager.getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomName, address);
                        if (room == null) {
                            LOGGER.info("房间创建出错了");
                            reply.setError(PacketError.Condition.internal_server_error);
                            return reply;
                        }
                        // Check if the room was created concurrently by another user
                        if (!room.getOwners().contains(address.asBareJID())) {
                            // 已经存在
                            reply.setError(PacketError.Condition.conflict);
                            return reply;
                        }
                    } catch (NotAllowedException e) {
                        // 权限不足
                        reply.setError(PacketError.Condition.not_allowed);
                        return reply;
                    }
                }

                LOGGER.info("房间创建成功：" + room.getName());

                String maxUsers = String.valueOf(Const.MAX_MEMBERS_ALLOW);
                Boolean broadcastModerator = true;
                Boolean broadcastParticipant = true;
                Boolean broadcastVisitor = true;
                String whois = "moderator";
                Boolean publicRoom = true;
                // Rooms created from the admin console are always persistent
                Boolean persistentRoom = true;
                Boolean canChangeNick = true;
                Boolean registrationEnabled = true;

                String naturalName = room.getNaturalLanguageName();
                String description = room.getDescription();
                String roomSubject = room.getSubject();
                String password = "";
                Boolean moderatedRoom = true;
                Boolean membersOnly = true;
                Boolean allowInvites = true;
                Boolean changeSubject = true;
                Boolean enableLog = true;
                Boolean reservedNick = true;

                // Set the new configuration sending an IQ packet with an dataform
                FormField field;
                XDataFormImpl dataForm = new XDataFormImpl(DataForm.TYPE_SUBMIT);

                field = new XFormFieldImpl("FORM_TYPE");
                field.setType(FormField.TYPE_HIDDEN);
                field.addValue("http://jabber.org/protocol/muc#roomconfig");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_roomname");
                field.addValue(naturalName);
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_roomdesc");
                field.addValue(description);
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_changesubject");
                field.addValue(changeSubject ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_maxusers");
                field.addValue(maxUsers);
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_presencebroadcast");
                if (broadcastModerator) {
                    field.addValue("moderator");
                }
                if (broadcastParticipant) {
                    field.addValue("participant");
                }
                if (broadcastVisitor) {
                    field.addValue("visitor");
                }
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_publicroom");
                field.addValue(publicRoom ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_persistentroom");
                field.addValue(persistentRoom ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_moderatedroom");
                field.addValue(moderatedRoom ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_membersonly");
                field.addValue(membersOnly ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_allowinvites");
                field.addValue(allowInvites ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_passwordprotectedroom");
                field.addValue(StringUtils.isEmpty(password) ? "0" : "1");
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_roomsecret");
                field.addValue(password);
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_whois");
                field.addValue(whois);
                dataForm.addField(field);

                field = new XFormFieldImpl("muc#roomconfig_enablelogging");
                field.addValue(enableLog ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("x-muc#roomconfig_reservednick");
                field.addValue(reservedNick ? "0" : "1");
                dataForm.addField(field);

                field = new XFormFieldImpl("x-muc#roomconfig_canchangenick");
                field.addValue(canChangeNick ? "1" : "0");
                dataForm.addField(field);

                field = new XFormFieldImpl("x-muc#roomconfig_registration");
                field.addValue(registrationEnabled ? "1" : "0");
                dataForm.addField(field);

                // Keep the existing list of admins
                field = new XFormFieldImpl("muc#roomconfig_roomadmins");
                for (JID jid : room.getAdmins()) {
                    field.addValue(jid.toString());
                }
                dataForm.addField(field);

                // Keep the existing list of owners
                field = new XFormFieldImpl("muc#roomconfig_roomowners");
                for (JID jid : room.getOwners()) {
                    field.addValue(packet.getFrom().toBareJID());
                }
                dataForm.addField(field);

                // update subject before sending IQ (to include subject with cluster update)
                if (!StringUtils.isEmpty(roomSubject)) {
                    // Change the subject of the room by sending a new message
                    Message message = new Message();
                    message.setType(Message.Type.groupchat);
                    message.setSubject(roomSubject);
                    message.setFrom(room.getRole().getRoleAddress());
                    message.setTo(room.getRole().getRoleAddress());
                    message.setID("local-only");
                    room.changeSubject(message, room.getRole());
                }

                // Create an IQ packet and set the dataform as the main fragment
                IQ iq = new IQ(IQ.Type.set);
                Element element = iq.setChildElement("query", "http://jabber.org/protocol/muc#owner");
                element.add(dataForm.asXMLElement());
                // Send the IQ packet that will modify the room's configuration

                room.getIQOwnerHandler().handleIQ(iq, room.getRole());

            }

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
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