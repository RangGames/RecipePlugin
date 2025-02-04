package gaya.pe.kr.core.util.method;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class EventUtil {
    public static void call(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static void register(Listener listener, Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
}

