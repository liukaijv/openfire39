package org.jivesoftware.openfire.plugin;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.openfire.plugin.dao.MUCNotificationDao;
import org.jivesoftware.openfire.plugin.handler.*;
import org.jivesoftware.openfire.plugin.listener.MUCEventListenerImpl;
import org.jivesoftware.openfire.plugin.listener.SessionEventListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MUCExtendsPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCExtendsPlugin.class);

    private SessionEventListener sessionEventListener;
    private MUCEventListenerImpl mucEventListener;

    IQRouter iqRouter;

    // IQHandlers
    IQHandler roomListHandler;
    IQHandler addMemberHandler;
    IQHandler mucRoomInfoHandler;
    IQHandler mucDestroyHandler;
    IQHandler mucMemberQuitHandler;
    IQHandler mucMemberApplyHandler;
    IQHandler mucMemberListHandler;
    IQHandler mucMemberKickHandler;
    IQHandler mucRoomCreateHandler;
    IQHandler mucOwnerResolveApplyHandler;
    IQHandler mucMemberResolveInviteHandler;
    IQHandler mucDeleteNotificationsHandler;
    IQHandler mucGetNotificationsHandler;
    IQHandler mucConfirmNotificationsHandler;

    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {

        LOGGER.info("群组插件运行成功!");

        MUCNotificationDao.createTable();

        iqRouter = XMPPServer.getInstance().getIQRouter();
        LOGGER.info("注册群组IQ");
        registerIQHandlers();

        if (sessionEventListener == null) {
            sessionEventListener = new SessionEventListenerImpl(XMPPServer.getInstance().getMessageRouter());
        }

        if (mucEventListener == null) {
            mucEventListener = new MUCEventListenerImpl();
        }

        MUCEventDispatcher.addListener(mucEventListener);
        SessionEventDispatcher.addListener(sessionEventListener);

    }

    @Override
    public void destroyPlugin() {
        removeIQHandlers();
        MUCEventDispatcher.removeListener(mucEventListener);
        SessionEventDispatcher.removeListener(sessionEventListener);
    }

    private void registerIQHandlers() {

        roomListHandler = new MUCRoomListHandler();
        addMemberHandler = new MUCAddMemberHandler();
        mucRoomInfoHandler = new MUCRoomInfoHandler();
        mucDestroyHandler = new MUCRoomDestroyHandler();
        mucMemberQuitHandler = new MUCMemberQuitHandler();
        mucMemberApplyHandler = new MUCMemberApplyHandler();
        mucMemberListHandler = new MUCMemberListHandler();
        mucMemberKickHandler = new MUCMemberKickHandler();
        mucRoomCreateHandler = new MUCRoomCreateHandler();
        mucOwnerResolveApplyHandler = new MUCOwnerResolveApplyHandler();
        mucMemberResolveInviteHandler = new MUCMemberResolveInviteHandler();
        mucDeleteNotificationsHandler = new MUCDeleteNotificationsHandler();
        mucGetNotificationsHandler = new MUCGetNotificationsHandler();
        mucConfirmNotificationsHandler = new MUCConfirmNotificationsHandler();

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
        iqRouter.addHandler(mucConfirmNotificationsHandler);
    }

    private void removeIQHandlers() {
        iqRouter.removeHandler(roomListHandler);
        iqRouter.removeHandler(addMemberHandler);
        iqRouter.removeHandler(mucRoomInfoHandler);
        iqRouter.removeHandler(mucDestroyHandler);
        iqRouter.removeHandler(mucMemberQuitHandler);
        iqRouter.removeHandler(mucMemberApplyHandler);
        iqRouter.removeHandler(mucMemberListHandler);
        iqRouter.removeHandler(mucMemberKickHandler);
        iqRouter.removeHandler(mucRoomCreateHandler);
        iqRouter.removeHandler(mucOwnerResolveApplyHandler);
        iqRouter.removeHandler(mucMemberResolveInviteHandler);
        iqRouter.removeHandler(mucDeleteNotificationsHandler);
        iqRouter.removeHandler(mucGetNotificationsHandler);
        iqRouter.removeHandler(mucConfirmNotificationsHandler);
    }

}
