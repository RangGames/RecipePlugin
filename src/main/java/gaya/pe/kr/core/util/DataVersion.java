package gaya.pe.kr.core.util;

import java.util.UUID;

public class DataVersion {
    private final long timestamp;
    private final String server;
    private final int version;
    private final UUID uuid;

    public DataVersion(UUID uuid, String server) {
        this.uuid = uuid;
        this.timestamp = System.currentTimeMillis();
        this.server = server;
        this.version = 1;
    }

    public boolean isNewer(DataVersion other) {
        return this.timestamp > other.timestamp;
    }
}