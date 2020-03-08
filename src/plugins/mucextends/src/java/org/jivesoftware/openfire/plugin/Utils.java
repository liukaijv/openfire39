package org.jivesoftware.openfire.plugin;

import org.dom4j.Element;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.jivesoftware.openfire.session.Session;
import org.json.JSONObject;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class Utils {

    public static Message notificationToMessage(MucNotification notification) {
        org.xmpp.packet.Message message = new Message();
        message.setFrom(notification.getFrom());
        message.setTo(notification.getTo());
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

    public static boolean pushNotificationToUser(JID to, MucNotification notification) {
        Session session = SessionManager.getInstance().getSession(to);
        if (Utils.isAuthenticatedSession(session)) {
            Message message = Utils.notificationToMessage(notification);
            session.process(message);
            return true;
        } else {
            try {
                XMPPServer.getInstance().getMessageRouter().route(Utils.notificationToMessage(notification));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
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

}
