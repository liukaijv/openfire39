package org.jivesoftware.openfire.plugin;

// 把id映射为6位以上的
public class IdHash {

    private final static long OFFSET = 50000;

    public static long encode(long id) {
        return Long.valueOf(Long.toOctalString(OFFSET + id));
    }

    public static long decode(long ocId) {
        return Long.parseLong("" + ocId, 8) - OFFSET;
    }

}
