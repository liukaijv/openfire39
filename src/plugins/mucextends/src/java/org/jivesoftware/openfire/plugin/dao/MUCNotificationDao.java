package org.jivesoftware.openfire.plugin.dao;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.plugin.MucUtils;
import org.jivesoftware.openfire.plugin.model.MucNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MUCNotificationDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCNotificationDao.class);

    private static final String CREATE_NOTIFICATION_TABLE = "CREATE TABLE IF NOT EXISTS `ofMucNotification` ( " +
            "  `id`      int(4)       NOT NULL auto_increment, " +
            "  `type`    int(4)       NOT NULL DEFAULT 0, " +
            "  `username` varchar(255)      NOT NULL, " +
            "  `roomJID` varchar(255) NOT NULL, " +
            "  `from`    varchar(255) NOT NULL, " +
            "  `to`      varchar(255) NOT NULL, " +
            "  `status`  int(4)       NOT NULL default 0, " +
//            "  `createAt` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
//            "  `updateAt` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  `createAt` varchar(24)    NOT NULL DEFAULT '0'," +
            "  `updateAt` varchar(24)   NOT NULL DEFAULT '0'," +
            "  `confirm` tinyint(1)   NOT NULL DEFAULT '0'," +
            "  `content`  text         null," +
            "  PRIMARY KEY (`id`), " +
            "  INDEX (`username`)" +
            ");";

    private static final String LOAD_NOTIFICATIONS = "SELECT * FROM `ofMucNotification` WHERE `username` = ? AND ( `confirm` = 0 OR `status` = 0 ) ORDER BY `updateAt` DESC LIMIT 30;";

    private static final String GET_NOTIFICATION = "SELECT * FROM `ofMucNotification` WHERE `username` = ? AND `roomJID` = ? AND `type` = ?;";

    private static final String GET_NOTIFICATION_BY_FROM = "SELECT * FROM `ofMucNotification` WHERE `from` = ? AND `roomJID` = ? AND `type` = ?;";

    private static final String GET_NOTIFICATION_BY_ID = "SELECT * FROM `ofMucNotification` WHERE `id` = ?";

    private static final String SAVE_NOTIFICATION = "INSERT INTO ofMucNotification (`roomJID`,`type`,`from`,`to`,`username`,`status`,`createAt`,`updateAt`) VALUES (?,?,?,?,?,?,?,?);";

    private static final String UPDATE_NOTIFICATION_STATUS = "UPDATE ofMucNotification SET `status` = ?, `updateAt` = ? WHERE `id` = ?;";

    private static final String DELETE_NOTIFICATION = "DELETE FROM  `ofMucNotification` WHERE `id` = ?;";

    private static final String UPDATE_NOTIFICATION_CONFIRM = "UPDATE ofMucNotification SET `confirm` = ? WHERE `id` = ?;";

    public static boolean createTable() {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(CREATE_NOTIFICATION_TABLE);
            LOGGER.info("CREATE_NOTIFICATION_TABLE: " + CREATE_NOTIFICATION_TABLE);
            return statement.execute();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return false;
    }

    public static MucNotification getNotification(String username, String roomJID, int type) {
        MucNotification notification = null;
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(GET_NOTIFICATION);
            LOGGER.info("GET_NOTIFICATION: " + GET_NOTIFICATION);
            statement.setString(1, username);
            statement.setString(2, roomJID);
            statement.setInt(3, type);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                notification = mapToMucNotification(resultSet);
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return notification;
    }

    public static MucNotification getNotificationById(Long id) {
        MucNotification notification = null;
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(GET_NOTIFICATION_BY_ID);
            LOGGER.info("GET_NOTIFICATION_BY_ID: " + GET_NOTIFICATION_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                notification = mapToMucNotification(resultSet);
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return notification;
    }

    public static MucNotification getNotificationByFrom(String fromJID, String roomJID, int type) {
        MucNotification notification = null;
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(GET_NOTIFICATION_BY_FROM);
            LOGGER.info("GET_NOTIFICATION_BY_FROM: " + GET_NOTIFICATION_BY_FROM);
            statement.setString(1, fromJID);
            statement.setString(2, roomJID);
            statement.setInt(3, type);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                notification = mapToMucNotification(resultSet);
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return notification;
    }

    public static MucNotification saveNotification(MucNotification notification) {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(SAVE_NOTIFICATION, Statement.RETURN_GENERATED_KEYS);
            LOGGER.info("SAVE_NOTIFICATION: " + SAVE_NOTIFICATION);
            statement.setString(1, notification.getRoomJID());
            statement.setInt(2, notification.getType());
            statement.setString(3, notification.getFrom());
            statement.setString(4, notification.getTo());
            statement.setString(5, notification.getUsername());
            statement.setInt(6, notification.getStatus());
            Long now = System.currentTimeMillis();
            statement.setString(7, String.valueOf(now));
            if (notification.getUpdateAt() > 0) {
                statement.setString(8, String.valueOf(notification.getUpdateAt()));
            } else {
                statement.setString(8, String.valueOf(now));
            }
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            while (generatedKeys.next()) {
                notification.setId(generatedKeys.getLong(1));
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return notification;
    }

    public static List<MucNotification> getNotifications(String username) {
        List<MucNotification> notifications = new ArrayList<MucNotification>();
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        MucNotification notification = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(LOAD_NOTIFICATIONS);
            LOGGER.info("LOAD_NOTIFICATIONS: " + LOAD_NOTIFICATIONS);
            statement.setString(1, username);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {

                notification = mapToMucNotification(resultSet);
                notifications.add(notification);

            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return notifications;
    }

    public static void deleteNotification(Long id) {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(DELETE_NOTIFICATION);
            LOGGER.info("DELETE_NOTIFICATION: " + DELETE_NOTIFICATION);
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
    }

    public static boolean updateNotificationStatus(MucNotification notification) {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(UPDATE_NOTIFICATION_STATUS, Statement.RETURN_GENERATED_KEYS);
            LOGGER.info("UPDATE_NOTIFICATION_STATUS: " + UPDATE_NOTIFICATION_STATUS);
            statement.setInt(1, notification.getStatus());
            statement.setString(2, String.valueOf(System.currentTimeMillis()));
            statement.setLong(3, notification.getId());
            statement.executeUpdate();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
            return false;
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return true;
    }

    public static boolean updateNotificationConfirm(Long id, boolean confirm) {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = DbConnectionManager.getConnection();
            statement = conn.prepareStatement(UPDATE_NOTIFICATION_CONFIRM, Statement.RETURN_GENERATED_KEYS);
            LOGGER.info("UPDATE_NOTIFICATION_CONFIRM_: " + UPDATE_NOTIFICATION_CONFIRM);
            statement.setBoolean(1, confirm);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
            return false;
        } finally {
            DbConnectionManager.closeConnection(statement, conn);
        }
        return true;
    }

    private static MucNotification mapToMucNotification(ResultSet resultSet) throws SQLException {
        MucNotification notification = new MucNotification();
        notification.setId(resultSet.getLong("id"));
        notification.setRoomJID(resultSet.getString("roomJID"));
        String fromUser = resultSet.getString("from");
        notification.setFrom(fromUser);
        notification.setTo(resultSet.getString("to"));
        notification.setStatus(resultSet.getInt("status"));
        notification.setType(resultSet.getInt("type"));
        notification.setUsername(resultSet.getString("username"));
        notification.setCreateAt(Long.valueOf(resultSet.getString("createAt")));
        notification.setUpdateAt(Long.valueOf(resultSet.getString("updateAt")));
        int confirm = resultSet.getInt("confirm");
        notification.setConfirm(confirm == 0 ? false : true);
        //昵称暂时放这里~~~
        JID fromJID = new JID(fromUser);
        notification.setFromNickname(MucUtils.getNickname(fromJID.getNode()));
//        JID toJID = new JID(resultSet.getString("to"));
//        notification.setToNickName(MucUtils.getNickname(toJID.getNode()));
        return notification;
    }

}
