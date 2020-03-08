package org.jivesoftware.openfire.plugin;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.plugin.dao.NotificationDao;
import org.jivesoftware.openfire.plugin.handler.*;
import org.jivesoftware.openfire.plugin.listener.SessionEventListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MUCExtendsPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCExtendsPlugin.class);

    private SessionEventListener sessionEventListener;

    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {

        LOGGER.info("用户房间转群插件运行成功!");

        NotificationDao.createTable();

        IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();

        IQHandler roomListHandler = new MUCRoomListHandler();
        IQHandler addMemberHandler = new MUCAddMemberHandler();
        IQHandler mucRoomInfoHandler = new MUCRoomInfoHandler();
        IQHandler mucDestroyHandler = new MUCRoomDestroyHandler();
        IQHandler mucMemberQuitHandler = new MUCMemberQuitHandler();
        IQHandler mucMemberApplyHandler = new MUCMemberApplyHandler();
        IQHandler mucMemberListHandler = new MUCMemberListHandler();
        IQHandler mucMemberKickHandler = new MUCMemberKickHandler();
        IQHandler mucRoomCreateHandler = new MUCRoomCreateHandler();
        IQHandler mucOwnerResolveApplyHandler = new MUCOwnerResolveApplyHandler();
        IQHandler mucMemberResolveInviteHandler = new MUCMemberResolveInviteHandler();
        IQHandler mucDeleteNotificationsHandler = new MUCDeleteNotificationsHandler();
        IQHandler mucGetNotificationsHandler = new MUCGetNotificationsHandler();

        iqRouter.addHandler(roomListHandler);
        iqRouter.addHandler(addMemberHandler);
        iqRouter.addHandler(mucRoomInfoHandler);
        iqRouter.addHandler(mucDestroyHandler);
        iqRouter.addHandler(mucMemberQuitHandler);
        iqRouter.addHandler(mucMemberApplyHandler);
        iqRouter.addHandler(mucMemberListHandler);
        iqRouter.addHandler(mucMemberKickHandler);
        iqRouter.addHandler(mucRoomCreateHandler);
        iqRouter.addHandler(mucOwnerResolveApplyHandler);
        iqRouter.addHandler(mucMemberResolveInviteHandler);
        iqRouter.addHandler(mucDeleteNotificationsHandler);
        iqRouter.addHandler(mucGetNotificationsHandler);

        if (sessionEventListener == null) {
            sessionEventListener = new SessionEventListenerImpl(XMPPServer.getInstance().getMessageRouter());
        }

//        MUCEventDispatcher.addListener(new MUCEventListenerImpl());
        SessionEventDispatcher.addListener(sessionEventListener);

    }

    @Override
    public void destroyPlugin() {
        SessionEventDispatcher.removeListener(sessionEventListener);
    }

}
