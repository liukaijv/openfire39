package org.jivesoftware.openfire.plugin.dao;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.plugin.IdHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MUCDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MUCDao.class);

    // 判断用户是否加入了房间，避免重复加入错误
    private static final String HAS_JOINED_ROOM = "SELECT roomID, jid FROM ofMucMember WHERE roomID=? AND jid=?";

    // 将用户加入群
    private static final String ADD_MEMBER = "INSERT INTO ofMucMember (roomID,jid,nickname) VALUES (?,?,?)";

    public static final String GET_MUC = "SELECT ofMucRoom.serviceID, " +
            "       ofMucRoom.roomID, " +
            "       ofMucRoom.name, " +
            "       ofMucRoom.naturalName, " +
            "       ofMucRoom.description, " +
            "       ofMucRoom.maxUsers, " +
            "       ofMucRoom.creationDate, " +
            "       '' AS userJid, " +
            "       '' AS nickname, " +
            "       50 AS affiliation " +
            "FROM ofMucRoom " +
            "WHERE ofMucRoom.name = ?;";

    public static final String GET_MUC_BY_ID = "SELECT ofMucRoom.serviceID, " +
            "       ofMucRoom.roomID, " +
            "       ofMucRoom.name, " +
            "       ofMucRoom.naturalName, " +
            "       ofMucRoom.description, " +
            "       ofMucRoom.maxUsers, " +
            "       ofMucRoom.creationDate, " +
            "       '' AS userJid, " +
            "       '' AS nickname, " +
            "       50 AS affiliation " +
            "FROM ofMucRoom " +
            "WHERE ofMucRoom.roomID = ?;";

    // 群信息
    private static final String GET_MY_MUC = "SELECT ofMucRoom.serviceID," +
            "       ofMucRoom.roomID," +
            "       ofMucRoom.name," +
            "       ofMucRoom.naturalName," +
            "       ofMucRoom.description," +
            "       ofMucRoom.maxUsers," +
            "       ofMucRoom.creationDate," +
            "       ofMucMember.jid AS userJid," +
            "       ofMucMember.nickname," +
            "       30 AS affiliation" +
            " FROM ofMucRoom" +
            "       JOIN ofMucMember ON ofMucRoom.roomID = ofMucMember.roomID" +
            "                             AND ofMucMember.jid = ? where ofMucRoom.name = ?" +
            " UNION" +
            " SELECT ofMucRoom.serviceID," +
            "       ofMucRoom.roomID," +
            "       ofMucRoom.name," +
            "       ofMucRoom.naturalName," +
            "       ofMucRoom.description," +
            "       ofMucRoom.maxUsers," +
            "       ofMucRoom.creationDate," +
            "       ofMucAffiliation.jid AS userJid," +
            "       NULL AS nickname," +
            "       ofMucAffiliation.`affiliation`" +
            " FROM ofMucRoom" +
            "       JOIN `ofMucAffiliation` ON ofMucRoom.roomID = ofMucAffiliation.roomID" +
            "                                    AND ofMucAffiliation.`jid` = ? where ofMucRoom.name = ?;";

    // 群列表
    private static final String GET_USER_MUCS = "SELECT ofMucRoom.serviceID," +
            "       ofMucRoom.roomID," +
            "       ofMucRoom.name," +
            "       ofMucRoom.naturalName," +
            "       ofMucRoom.description," +
            "       ofMucRoom.maxUsers," +
            "       ofMucRoom.creationDate," +
            "       ofMucMember.jid AS userJid," +
            "       ofMucMember.nickname," +
            "       30 AS affiliation" +
            " FROM ofMucRoom" +
            "       JOIN ofMucMember ON ofMucRoom.roomID = ofMucMember.roomID" +
            "                             AND ofMucMember.jid = ? " +
            " UNION" +
            " SELECT ofMucRoom.serviceID," +
            "       ofMucRoom.roomID," +
            "       ofMucRoom.name," +
            "       ofMucRoom.naturalName," +
            "       ofMucRoom.description," +
            "       ofMucRoom.maxUsers," +
            "       ofMucRoom.creationDate," +
            "       ofMucAffiliation.jid AS userJid," +
            "       NULL AS nickname," +
            "       ofMucAffiliation.`affiliation`" +
            " FROM ofMucRoom" +
            "       JOIN `ofMucAffiliation` ON ofMucRoom.roomID = ofMucAffiliation.roomID" +
            "                                    AND ofMucAffiliation.`jid` = ? ;";

    // 用户已加入的房间
    public static List<RoomInfo> getUserMucs(String userJid) {
        List<RoomInfo> list = new ArrayList<RoomInfo>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DbConnectionManager.getConnection();
            statement = connection.prepareStatement(GET_USER_MUCS);
            statement.setString(1, userJid);
            statement.setString(2, userJid);
            LOGGER.info("exec sql GET_USER_MUCS: " + GET_USER_MUCS + " userJid:" + userJid);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(mapToRoomInfo(resultSet));
            }
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            e1.printStackTrace();
        } finally {
            DbConnectionManager.closeConnection(resultSet, statement, connection);
        }
        return list;
    }

    // 取我的房间信息
    public static RoomInfo getUserMuc(String userJid, String roomName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        RoomInfo roomInfo = null;
        try {
            connection = DbConnectionManager.getConnection();
            statement = connection.prepareStatement(GET_MY_MUC);
            statement.setString(1, userJid);
            statement.setString(2, roomName);
            statement.setString(3, userJid);
            statement.setString(4, roomName);
            LOGGER.info("exec sql GET_MY_MUC: " + GET_MY_MUC + " userJid: " + userJid + " roomName: " + roomName);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                roomInfo = mapToRoomInfo(resultSet);
            }
            if (roomInfo == null) {
                roomInfo = getMuc(roomName);
            }
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            e1.printStackTrace();
        } finally {
            DbConnectionManager.closeConnection(resultSet, statement, connection);
        }
        return roomInfo;
    }

    public static RoomInfo getMuc(String roomName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        RoomInfo roomInfo = null;
        try {
            connection = DbConnectionManager.getConnection();
            statement = connection.prepareStatement(GET_MUC);
            statement.setString(1, roomName);
            LOGGER.info("exec sql GET_MUC: " + GET_MUC + " roomName: " + roomName);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                roomInfo = mapToRoomInfo(resultSet);
            }
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            e1.printStackTrace();
        } finally {
            DbConnectionManager.closeConnection(resultSet, statement, connection);
        }
        return roomInfo;
    }

    public static RoomInfo getMucById(long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        RoomInfo roomInfo = null;
        try {
            connection = DbConnectionManager.getConnection();
            statement = connection.prepareStatement(GET_MUC_BY_ID);
            statement.setLong(1, id);
            LOGGER.info("exec sql GET_MUC_BY_ID: " + GET_MUC_BY_ID + " id: " + id);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                roomInfo = mapToRoomInfo(resultSet);
            }
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            e1.printStackTrace();
        } finally {
            DbConnectionManager.closeConnection(resultSet, statement, connection);
        }
        return roomInfo;
    }

    private static RoomInfo mapToRoomInfo(ResultSet resultSet) throws SQLException {
        RoomInfo roomInfo = new RoomInfo();
        roomInfo.setServiceID(resultSet.getInt(1));
        roomInfo.setRoomID(resultSet.getLong(2));
        roomInfo.setName(resultSet.getString(3));
        roomInfo.setNaturalName(resultSet.getString(4));
        roomInfo.setDescription(resultSet.getString(5));
        roomInfo.setMaxUsers(resultSet.getInt(6));
        roomInfo.setCreationDate(resultSet.getString(7));
        roomInfo.setUserJid(resultSet.getString(8));
        roomInfo.setNickname(resultSet.getString(9));
        roomInfo.setAffiliation(resultSet.getInt(10));
        roomInfo.setCardId(IdHash.encode(resultSet.getLong(2)));
        return roomInfo;
    }

    // 将用户加入群
    public static void saveMember(MUCRoom localMUCRoom, JID bareJID, String nickname) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(ADD_MEMBER);
            pstmt.setLong(1, localMUCRoom.getID());
            pstmt.setString(2, bareJID.toBareJID());
            pstmt.setString(3, nickname);
            pstmt.executeUpdate();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }

    // 判断用户是否加入了群
    public static boolean hasJoinedRoom(MUCRoom localMUCRoom, JID bareJID) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(HAS_JOINED_ROOM);
            pstmt.setLong(1, localMUCRoom.getID());
            pstmt.setString(2, bareJID.toBareJID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage(), sqle);
        } finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return false;
    }
}
