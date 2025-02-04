package gaya.pe.kr.core.util;

import gaya.pe.kr.core.RecipePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SchedulerUtil {
    private static final BukkitScheduler BUKKIT_SCHEDULER = Bukkit.getScheduler();

    public static void cancel(int taskId) {
        BUKKIT_SCHEDULER.cancelTask(taskId);
    }

    public static int scheduleRepeatingTask(Runnable task, int delay, int interval) {
        Plugin plugin = RecipePlugin.getPlugin();
        return BUKKIT_SCHEDULER.scheduleSyncRepeatingTask(plugin, task, (long)delay, (long)interval);
    }

    public static void runTaskLater(Runnable task, int delay) {
        Plugin plugin = RecipePlugin.getPlugin();
        BUKKIT_SCHEDULER.runTaskLater(plugin, task, (long)delay);
    }
}

