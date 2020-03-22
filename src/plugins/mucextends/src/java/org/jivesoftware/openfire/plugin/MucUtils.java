package org.jivesoftware.openfire.plugin;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.vcard.VCardManager;
import org.jivesoftware.util.TaskEngine;
import org.json.JSONObject;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.TimerTask;

public class MucUtils {

    public static Message notificationToMessage(MucNotification notification) {
        return notificationToMessage(new JID(notification.getTo()), notification);
    }

    public static Message notificationToMessage(JID to, MucNotification notification) {
        org.xmpp.packet.Message message = new Message();
        message.setFrom(notification.getFrom());
        message.setTo(to);
        message.setType(Message.Type.groupchat);
        message.setSubject(Const.MESSAGE_GROUP_NOTIFICATION);
        JSONObject jsonObject = new JSONObject(notification);
        message.setBody(jsonObject.toString());
        return message;
    }

    public static void addMemberToRoom(MUCRoom room, JID userJID) throws Exception {
        // 加入群
        IQ mucIq = new IQ(IQ.Type.set);
        Element frag = mucIq.setChildElement("query", "http://jabber.org/protocol/muc#admin");
        Element item = frag.addElement("item");
        item.addAttribute("affiliation", "member");
        item.addAttribute("jid", userJID.toBareJID());
        // Send the IQ packet that will modify the room's
        // configuration
        room.getIQAdminHandler().handleIQ(mucIq, room.getRole());
    }

    public static boolean isAuthenticatedSession(Session session) {
        return session != null && session.getStatus() == Session.STATUS_AUTHENTICATED;
    }

    public static void pushNotificationToUser(final JID to, final MucNotification notification) {
        pushNotificationToUser(to, notification, 3000);
    }

    public static void pushNotificationToUser(final JID to, final MucNotification notification, int delay) {
        TimerTask messageTask = new TimerTask() {
            @Override
            public void run() {
                Session session = SessionManager.getInstance().getSession(to);
                if (MucUtils.isAuthenticatedSession(session)) {
                    Message message = MucUtils.notificationToMessage(notification);
                    session.process(message);
                } else {
                    try {
                        XMPPServer.getInstance().getMessageRouter().route(MucUtils.notificationToMessage(to, notification));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        TaskEngine.getInstance().schedule(messageTask, delay);
    }

    public static boolean hasJoinedRoom(MUCRoom room, JID userJid) {
        for (JID jid : room.getOwners()) {
            if (jid.toBareJID().equals(userJid.toBareJID())) {
                return true;
            }
        }
        for (JID jid : room.getAdmins()) {
            if (jid.toBareJID().equals(userJid.toBareJID())) {
                return true;
            }
        }
        for (JID jid : room.getMembers()) {
            if (jid.toBareJID().equals(userJid.toBareJID())) {
                return true;
            }
        }
        for (JID jid : room.getOutcasts()) {
            if (jid.toBareJID().equals(userJid.toBareJID())) {
                return true;
            }
        }
        return false;
    }

    public static JID getOwner(MUCRoom room) {
        for (JID jid : room.getOwners()) {
            if (jid != null) {
                return jid;
            }
        }
        return null;
    }

    public static int getUserAffiliation(MUCRoom room, JID userJID) {
        for (JID jid : room.getOwners()) {
            if (jid.toBareJID().equals(userJID.toBareJID())) {
                return MUCRole.Affiliation.owner.getValue();
            }
        }
        for (JID jid : room.getAdmins()) {
            if (jid.toBareJID().equals(userJID.toBareJID())) {
                return MUCRole.Affiliation.admin.getValue();
            }
        }
        for (JID jid : room.getMembers()) {
            if (jid.toBareJID().equals(userJID.toBareJID())) {
                return MUCRole.Affiliation.member.getValue();
            }
        }
        for (JID jid : room.getOutcasts()) {
            if (jid.toBareJID().equals(userJID.toBareJID())) {
                return MUCRole.Affiliation.outcast.getValue();
            }
        }
        return MUCRole.Affiliation.none.getValue();
    }

    public static String getNickname(String userName) {

        VCardManager vCardManager = VCardManager.getInstance();
        String nickname = vCardManager.getVCardProperty(userName, "NICKNAME");

        if (StringUtils.isEmpty(nickname)) {
            try {
                User user = UserManager.getInstance().getUser(userName);
                if (user != null) {
                    if (StringUtils.isEmpty(user.getName())) {
                        nickname = user.getName();
                    } else {
                        nickname = user.getUsername();
                    }
                }
            } catch (UserNotFoundException e) {
                e.printStackTrace();
            }
            if (StringUtils.isEmpty(nickname)) {
                return userName;
            }
        }

        return nickname;
    }

}
