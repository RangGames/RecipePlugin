package gaya.pe.kr.core.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CookSavedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private UUID uuid;
    private String data1;
    private String data2;

    public CookSavedEvent(UUID uuid, String data1, String data2) {
        this.uuid = uuid;
        this.data1 = data1;
        this.data2 = data2;
    }

    public UUID getUuid() {
        return uuid;
    }
    public String getData1() {
        return data1;
    }
    public String getData2() {
        return data2;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return null;
    }
}
