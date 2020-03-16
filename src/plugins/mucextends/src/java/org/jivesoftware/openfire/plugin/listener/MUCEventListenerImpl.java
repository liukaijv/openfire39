package org.jivesoftware.openfire.plugin.listener;

import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.openfire.plugin.dao.MUCDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class MUCEventListenerImpl implements MUCEventListener {

    private static final Logger Log = LoggerFactory.getLogger(MUCDao.class);

    @Override
    public void roomCreated(JID roomJID) {

    }

    @Override
    public void roomDestroyed(JID roomJID) {

    }

    @Override
    public void occupantJoined(JID roomJID, JID userJID, String nickname) {
        // todo 推送离线记录
    }

    @Override
    public void occupantLeft(JID roomJID, JID user) {

    }

    @Override
    public void nicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) {

    }

    @Override
    public void messageReceived(JID roomJID, JID user, String nickname, Message message) {

    }

    @Override
    public void privateMessageRecieved(JID toJID, JID fromJID, Message message) {

    }

    @Override
    public void roomSubjectChanged(JID roomJID, JID user, String newSubject) {

    }

}
