package gaya.pe.kr.player.reactor;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.events.CookInventorySavedEvent;
import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.core.util.method.EventUtil;
import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.player.data.PlayerPersistent;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class CookEquipment
        implements Listener {
    Inventory inventory;
    Player player;
    PlayerPersistent playerPersistent;
    final int ITEM_INDEX = 4;

    public CookEquipment(Player player) {
        this.player = player;
        this.playerPersistent = PlayerCauldronManager.getInstance().getPlayerCauldron(player);
        this.inventory = Bukkit.createInventory(null, (int) 18, (String) RecipeServiceManager.getInstance().getCookEquipmentTitle());
        ItemStack itemStack = this.playerPersistent.getCookEquipment();
        if (itemStack != null) {
            this.inventory.setItem(4, itemStack);
        }
    }

    public void start() {
        this.player.openInventory(this.inventory);
        EventUtil.register(this, RecipePlugin.getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void zClickEventory(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null) {
            Inventory nowInventory = event.getInventory();
            Player targetPlayer = (Player) event.getWhoClicked();
            PlayerInventory playerInventory = this.player.getInventory();
            if (targetPlayer.getUniqueId().equals(this.player.getUniqueId()) && event.getInventory().equals(this.inventory)) {
                if (clickedInventory.equals(this.player.getInventory())) {
                    ItemStack currentItem = event.getCurrentItem();
                    if (!Filter.isNullOrAirItem(currentItem)) {
                        ItemStack currentItemClone = currentItem.clone();
                        ItemStack skinItem = nowInventory.getItem(4);
                        if (!Filter.isNullOrAirItem(skinItem)) {
                            CookEquipment.addItem(playerInventory, this.player, skinItem.clone());
                        }
                        currentItem.setAmount(0);
                        currentItem.setType(Material.AIR);
                        nowInventory.setItem(4, currentItemClone);
                    }
                } else {
                    ItemStack skinItem;
                    int clickedSlot = event.getSlot();
                    if (clickedSlot == 4 && !Filter.isNullOrAirItem(skinItem = nowInventory.getItem(4))) {
                        CookEquipment.addItem(playerInventory, this.player, skinItem.clone());
                        nowInventory.setItem(4, new ItemStack(Material.AIR));
                    }
                }
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void zCloseInventory(InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();
        Player closerPlayer = (Player) event.getPlayer();
        if (closerPlayer.getUniqueId().equals(this.player.getUniqueId()) && closedInventory.equals(this.inventory)) {
            ItemStack targetItem = closedInventory.getItem(4);
            CookInventorySavedEvent cookInventorySavedEvent;
            if (!Filter.isNullOrAirItem(targetItem)) {
                this.playerPersistent.setCookEquipment(targetItem);
                cookInventorySavedEvent = new CookInventorySavedEvent(player.getUniqueId(), "equipment", ObjectConverter.getObjectAsString(targetItem));

            } else {
                this.playerPersistent.setCookEquipment(null);
                cookInventorySavedEvent = new CookInventorySavedEvent(player.getUniqueId(), "equipment", "");
            }
            Bukkit.getPluginManager().callEvent(cookInventorySavedEvent);
            ItemStack cursorItem = closerPlayer.getItemOnCursor();
            if (!Filter.isNullOrAirItem(cursorItem)) {
                ItemStack itemStack = cursorItem.clone();
            }
            HandlerList.unregisterAll((Listener) this);
        }
    }

    public static void addItem(PlayerInventory playerInventory, Player player, ItemStack itemStack) {
        if (!Filter.isNullOrAirItem(itemStack)) {
            if (CookEquipment.getPlayerRemainInventory(playerInventory.getStorageContents()) > 0) {
                playerInventory.addItem(new ItemStack[]{itemStack});
            } else {
                player.getLocation().getWorld().dropItemNaturally(player.getLocation(), itemStack);
                player.sendMessage("인벤토리의 빈 공간이 없어 바닥으로 드랍 되었습니다");
            }
        }
    }

    public static int getPlayerRemainInventory(ItemStack[] itemStacks) {
        int count = 0;
        for (ItemStack content : itemStacks) {
            if (content == null) {
                ++count;
                continue;
            }
            if (!content.getType().equals((Object) Material.AIR)) continue;
            ++count;
        }
        return count;
    }
}

