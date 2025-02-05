package gaya.pe.kr.core.util;

import java.util.UUID;

public class PlayerDataLock {
    private final UUID uuid;
    private final String server;
    private final long timestamp;
    private final int timeout;
    private boolean locked;

    public PlayerDataLock(UUID uuid, String server, int timeoutSeconds) {
        this.uuid = uuid;
        this.server = server;
        this.timestamp = System.currentTimeMillis();
        this.timeout = timeoutSeconds;
        this.locked = true;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > timeout * 1000L;
    }
}