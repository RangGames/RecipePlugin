package gaya.pe.kr.player.reactor;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.method.EventUtil;
import gaya.pe.kr.player.data.PlayerPersistent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class PlayerPersistentInventoryReact
implements Listener {
    Player player;
    PlayerPersistent playerPersistent;
    Inventory inventory;
    boolean virtualInventory;

    public PlayerPersistentInventoryReact(Player player, PlayerPersistent playerPersistent, boolean virtualInventory) {
        this.player = player;
        this.playerPersistent = playerPersistent;
        this.virtualInventory = virtualInventory;
        if (!virtualInventory) {
            this.inventory = Bukkit.createInventory(null, (int)54, (String)"§7요리 재료");
            this.inventory.setContents(playerPersistent.getItemStacks());
        } else {
            this.inventory = Bukkit.createInventory(null, (int)54, (String)"§8요리 가방");
            this.inventory.setContents(playerPersistent.getVirtualInventory());
        }
    }

    public void open() {
        EventUtil.register(this, RecipePlugin.getPlugin());
        this.player.openInventory(this.inventory);
    }

    public Player getPlayer() {
        return this.player;
    }

    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
        Inventory closedInventory;
        Player player = (Player)event.getPlayer();
        if (this.getPlayer().equals(player) && this.inventory.equals(closedInventory = event.getInventory())) {
            if (this.virtualInventory) {
                this.playerPersistent.setVirtualInventory(closedInventory.getContents());
            } else {
                this.playerPersistent.setItemStacks(closedInventory.getContents());
            }
            HandlerList.unregisterAll((Listener)this);
        }
    }

    @EventHandler
    public void clickInventory(InventoryClickEvent event) {
        if (!this.virtualInventory) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        Inventory nowInventory = event.getInventory();
        Player player = (Player)event.getWhoClicked();
        if (nowInventory.equals(this.inventory)) {
            InventoryAction inventoryAction = event.getAction();
            if (player.getInventory().equals(clickedInventory)) {
                if (!inventoryAction.name().contains("PLACE")) {
                    event.setCancelled(true);
                    event.setResult(Event.Result.DENY);
                }
            } else if (inventoryAction.equals((Object)InventoryAction.HOTBAR_SWAP)) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
            }
        }
    }
}

