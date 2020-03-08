-- 消息通知表
CREATE TABLE IF NOT EXISTS `ofMucNotification` (
  `id`       int(4)       NOT NULL auto_increment,
  `username` varchar(255) NOT NULL,
  `type`     int(4)       NOT NULL DEFAULT 0,
  `roomJID`  varchar(255) NOT NULL,
  `from`     varchar(255) NOT NULL,
  `to`       varchar(255) NOT NULL,
  `status`   int(4)       NOT NULL default 0,
  `createAt` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateAt` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `content`  text         null,
  PRIMARY KEY (`id`),
  INDEX (`username`)
);

-- 获取群组列表
SELECT ofMucRoom.serviceID,
       ofMucRoom.roomID,
       ofMucRoom.name,
       ofMucRoom.naturalName,
       ofMucRoom.description,
       ofMucRoom.maxUsers,
       ofMucRoom.creationDate,
       ofMucMember.jid AS userJid,
       ofMucMember.nickname,
       30 AS affiliation
FROM ofMucRoom
       JOIN ofMucMember ON ofMucRoom.roomID = ofMucMember.roomID
                             AND ofMucMember.jid = ?
UNION
SELECT ofMucRoom.serviceID,
       ofMucRoom.roomID,
       ofMucRoom.name,
       ofMucRoom.naturalName,
       ofMucRoom.description,
       ofMucRoom.maxUsers,
       ofMucRoom.creationDate,
       ofMucAffiliation.jid AS userJid,
       NULL AS nickname,
       ofMucAffiliation.`affiliation`
FROM ofMucRoom
       JOIN `ofMucAffiliation` ON ofMucRoom.roomID = ofMucAffiliation.roomID
                                    AND ofMucAffiliation.`jid` = ?;

-- 获取群组信息
SELECT ofMucRoom.serviceID,
       ofMucRoom.roomID,
       ofMucRoom.name,
       ofMucRoom.naturalName,
       ofMucRoom.description,
       ofMucRoom.maxUsers,
       ofMucRoom.creationDate,
       ofMucMember.jid AS userJid,
       ofMucMember.nickname,
       30 AS affiliation
FROM ofMucRoom
       JOIN ofMucMember ON ofMucRoom.roomID = ofMucMember.roomID
                             AND ofMucMember.jid = ? where ofMucRoom.name = ?
UNION
SELECT ofMucRoom.serviceID,
       ofMucRoom.roomID,
       ofMucRoom.name,
       ofMucRoom.naturalName,
       ofMucRoom.description,
       ofMucRoom.maxUsers,
       ofMucRoom.creationDate,
       ofMucAffiliation.jid AS userJid,
       NULL AS nickname,
       ofMucAffiliation.`affiliation`
FROM ofMucRoom
       JOIN `ofMucAffiliation` ON ofMucRoom.roomID = ofMucAffiliation.roomID
                                    AND ofMucAffiliation.`jid` = ? where ofMucRoom.name = ?;