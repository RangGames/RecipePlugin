package gaya.pe.kr.core.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CookInventorySavedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private UUID uuid;
    private String type;
    private String data;

    public CookInventorySavedEvent(UUID uuid, String type, String data) {
        this.uuid = uuid;
        this.type = type;
        this.data = data;
    }

    public UUID getUuid() {
        return uuid;
    }
    public String getType() {
        return type;
    }
    public String getData() {
        return data;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return null;
    }
}