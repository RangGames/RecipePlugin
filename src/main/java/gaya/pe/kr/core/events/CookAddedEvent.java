package gaya.pe.kr.core.events;

import gaya.pe.kr.recipe.obj.Recipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CookAddedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private UUID uuid;
    private Recipe recipe;
    private String data;

    public CookAddedEvent(UUID uuid, Recipe recipe, String data) {
        this.uuid = uuid;
        this.recipe = recipe;
        this.data = data;
    }

    public UUID getUuid() {
        return uuid;
    }
    public Recipe getRecipe() {
        return recipe;
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
