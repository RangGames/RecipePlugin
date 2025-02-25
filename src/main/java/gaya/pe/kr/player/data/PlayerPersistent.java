package gaya.pe.kr.player.data;

import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.core.util.method.UtilMethod;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerPersistent {
    UUID uuid;
    ItemStack[] itemStacks = new ItemStack[54];
    ItemStack[] virtualInventory = new ItemStack[54];
    ItemStack cookEquipment;


    public PlayerPersistent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public ItemStack[] getItemStacks() {
        return this.itemStacks;
    }

    public void setItemStacks(ItemStack[] itemStacks) {
        this.itemStacks = itemStacks;
    }

    public ItemStack[] getVirtualInventory() {
        return this.virtualInventory;
    }

    public void setVirtualInventory(ItemStack[] virtualInventory) {
        this.virtualInventory = virtualInventory;
    }

    public boolean addItem(ItemStack itemStack) {
        Inventory inventory = Bukkit.createInventory(null, (int) 54);
        inventory.setContents(this.getVirtualInventory());
        if (UtilMethod.getPlayerRemainInventory(inventory.getContents()) > 0) {
            inventory.addItem(new ItemStack[]{itemStack});
            this.virtualInventory = inventory.getContents();
            return true;
        }
        return false;
    }

    public ItemStack getCookEquipment() {
        return this.cookEquipment;
    }

    public void setCookEquipment(ItemStack cookEquipment) {
        this.cookEquipment = cookEquipment;
    }

    @Override
    public String toString() {
        return "PlayerPersistent{" +
                "uuid=" + uuid +
                ", itemStacks=" + ObjectConverter.getObjectAsString(itemStacks) +
                ", virtualInventory=" + ObjectConverter.getObjectAsString(virtualInventory) +
                ", cookEquipment=" + ObjectConverter.getObjectAsString(cookEquipment) +
                '}';
    }
}

