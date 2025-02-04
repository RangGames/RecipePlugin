package gaya.pe.kr.core;

import gaya.pe.kr.core.API.RecipeAPI;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.recipe.manager.CookManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import java.util.logging.Level;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

public final class RecipePlugin extends JavaPlugin implements Listener {
    private static Plugin plugin;
    private static RecipeAPI api;
    private static LuckPerms luckPerms;
    RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
    PlayerCauldronManager playerCauldronManager = PlayerCauldronManager.getInstance();
    CookManager cookManager = CookManager.getInstance();
    static final String PREFIX = "§6§l|§7";

    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();

        plugin = this;
        luckPerms = (LuckPerms)this.getServer().getServicesManager().load(LuckPerms.class);
        this.recipeServiceManager.init();
        this.playerCauldronManager.init();
        this.cookManager.init();
        api = new RecipeAPI(this);
    }

    public void onDisable() {
        this.recipeServiceManager.saveData();
        this.recipeServiceManager.close();
        this.playerCauldronManager.close();
        this.cookManager.saveData();
    }

    public static void log(String message) {
        Bukkit.getLogger().log(Level.INFO, ChatColor.translateAlternateColorCodes((char)'&', (String)message));
    }

    public static void registerEvent(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        log(String.format("&f[&6&l%s&f]의 클래스가 정상적으로 이벤트 핸들러에 등록됐습니다", listener.getClass().getName()));
    }

    public static void registerCommand(String command, CommandExecutor commandExecutor) {
        Bukkit.getPluginCommand((String)command).setExecutor(commandExecutor);
        log(String.format("&f[&6&l%s&f]의 클래스가 정상적으로 커맨드 핸들러에 등록됐습니다 커맨드 : &f[&6&l%s&f]", commandExecutor.getClass().getName(), command));
    }

    public static void registerCommandTab(String command, TabCompleter tabCompleter) {
        Bukkit.getPluginCommand((String)command).setTabCompleter(tabCompleter);
        log(String.format("&f[&6&l%s&f]의 클래스가 정상적으로 Tab Handler 에 등록됐습니다 커맨드 : &f[&6&l%s&f]", tabCompleter.getClass().getName(), command));
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static RecipeAPI getAPI() {
        return api;
    }

    public static BukkitScheduler getBukkitScheduler() {
        return Bukkit.getScheduler();
    }

    public static void msg(@Nullable Player player, String msg) {
        if (player != null) {
            player.sendMessage(String.format("§6§l|§7 %s", msg).replace("&", "§"));
        }
    }

    public static String getPREFIX() {
        return PREFIX;
    }

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }
}

