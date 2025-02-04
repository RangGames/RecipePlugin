package gaya.pe.kr.recipe.obj;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TempBossbar {

    public TempBossbar(Player p, String text) {
        BossBar bossBar = Bukkit.createBossBar(text, BarColor.WHITE, BarStyle.SOLID);
        bossBar.addPlayer(p);
        SchedulerUtil.runTaskLater((Runnable) () -> bossBar.removeAll(), 60);

    }
}
