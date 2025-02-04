package gaya.pe.kr.recipe.scheduler;

import gaya.pe.kr.core.API.RecipeAPI;
import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.SchedulerUtil;
import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.core.util.method.EventUtil;
import gaya.pe.kr.player.data.PlayerPersistent;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.recipe.manager.CookManager;
import gaya.pe.kr.recipe.obj.Recipe;

import java.util.*;

import gaya.pe.kr.recipe.obj.TempBossbar;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class CookTimeBossBar
implements Runnable,
Listener {
    int taskId;
    UUID uuid;
    Recipe recipe;
    BossBar bossBar;
    String bossBarTitle;
    int amount;
    int nowMakeItemAmount = 0;
    int time = 0;
    int makeTime;
    Player player;
    Date date = new Date();

    public CookTimeBossBar(UUID uuid, Recipe recipe, String bossBarTitle, int amount, int nowMakeItemAmount, int time, int makeTime) {
        this.uuid = uuid;
        this.recipe = recipe;
        this.bossBar = Bukkit.createBossBar((String)bossBarTitle, (BarColor)BarColor.GREEN, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
        this.bossBar.setProgress(0.0);
        this.bossBarTitle = bossBarTitle;
        this.amount = amount;
        this.nowMakeItemAmount = nowMakeItemAmount;
        this.time = time;
        this.makeTime = makeTime;
        if (this.makeTime <= 0) {
            this.makeItem();
        }
    }

    public CookTimeBossBar(Player player, Recipe recipe, int amount, String bossBarTitle) {
        List lore;
        this.uuid = player.getUniqueId();
        this.recipe = recipe;
        this.bossBar = Bukkit.createBossBar((String)bossBarTitle, (BarColor)BarColor.BLUE, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
        this.bossBar.setProgress(0.0);
        this.bossBarTitle = bossBarTitle;
        this.amount = amount;
        this.makeTime = recipe.getMakeTime();
        ItemStack headItem = PlayerCauldronManager.getInstance().getPlayerCauldron(player).getCookEquipment();
        if (!Filter.isNullOrAirItem(headItem) && (lore = headItem.getItemMeta().getLore()) != null) {
            Iterator loreiter = lore.iterator();
            while(loreiter.hasNext()) {
                String s = (String)loreiter.next();
                s = ChatColor.stripColor(s);
                if (!s.contains("요리 시간")) continue;
                try {
                    int cookTime = Integer.parseInt(s.replaceAll("[^0-9]", "").trim());
                    this.makeTime -= cookTime;

                } catch (NumberFormatException numberFormatException) {}
            }
        }
        if (this.makeTime < 0) {
            this.makeTime = 1;
        }
        if (this.makeTime <= 0) {
            this.makeItem();
        }
    }

    @Override
    public void run() {
        if (CookManager.getInstance().isLoading(uuid)) {
            return;
        }
        double progress = 0.0;
        progress = this.time == 0 || this.makeTime == 0 ? 0.0 : (double)this.time / (double)this.makeTime;
        if (progress < 0.0) {
            progress = 0.0;
        }
        if (progress > 1.0) {
            progress = 1.0;
        }
        this.bossBar.setProgress(progress);
        this.bossBar.setTitle(this.bossBarTitle.replace("%time%", Integer.toString(this.makeTime - this.time)).replace("%recipe_name%", this.recipe.getRecipeName()).replace("%remain_amount%", Integer.toString(this.amount - this.nowMakeItemAmount)));
        if (this.time < this.makeTime) {
            ++this.time;
        } else {
            this.makeItem();
        }
    }

    private void makeItem() {
        PlayerPersistent playerPersistent = PlayerCauldronManager.getInstance().getPlayerCauldron(this.uuid);
        ArrayList<ItemStack> itemStackList = new ArrayList<ItemStack>();
        for (ItemStack itemStack : playerPersistent.getItemStacks()) {
            if (Filter.isNullOrAirItem(itemStack)) continue;
            itemStackList.add(itemStack);
        }
        if (this.recipe.makeItem(this.uuid, playerPersistent, itemStackList, 1)) {
            ++this.nowMakeItemAmount;
            this.time = 0;
            if (this.nowMakeItemAmount >= this.amount) {
                this.interrupt();
            }
        } else {
            if (this.player != null) {
                new TempBossbar(player, "§c가마솥에 재료가 부족하여 요리가 취소되었습니다.");

            }
            this.interrupt();
        }
    }

    public void interrupt() {
        this.bossBar.removeAll();
        this.bossBar = null;
        SchedulerUtil.cancel(this.taskId);
        HandlerList.unregisterAll((Listener)this);
        CookManager.getInstance().removeCook(this.uuid);
    }

    public void start() {
        CookManager.getInstance().addCook(this.uuid, this);
        this.player = Bukkit.getPlayer((UUID)this.uuid);
        if (this.player != null) {
            this.bossBar.addPlayer(this.player);
            this.player.closeInventory();
        }
        this.taskId = SchedulerUtil.scheduleRepeatingTask(this, 0, 20);
        EventUtil.register(this, RecipePlugin.getPlugin());
    }

    @EventHandler
    public void cancelThis(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(this.uuid)) {
            this.bossBar.removeAll();
        }
    }

    @EventHandler
    public void joinPlayer(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(this.uuid)) {
            this.bossBar.addPlayer(player);
        }
    }

    @EventHandler
    public void cancelCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(this.uuid)) {
            String message = event.getMessage();
            if (message.contains("/요리")) {
                if (RecipePlugin.getAPI().isDataLoading(player.getUniqueId())) {
                    RecipePlugin.msg(player, "&6데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
                    return;
                }
                if (message.equals("/요리 취소")) {
                    RecipePlugin.msg(player, "&6성공적으로 요리를 취소합니다");
                    event.setCancelled(true);
                    this.interrupt();
                } else {

                    RecipePlugin.msg(player, "&c요리 중엔 요리 명령어를 입력할 수 없습니다");
                    event.setCancelled(true);
                }
            }

        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Recipe getRecipe() {
        return this.recipe;
    }

    public int getAmount() {
        return this.amount;
    }

    public int getNowMakeItemAmount() {
        return this.nowMakeItemAmount;
    }

    public int getTime() {
        return this.time;
    }

    public int getMakeTime() {
        return this.makeTime;
    }

    public Date getDate() {
        return this.date;
    }
}

