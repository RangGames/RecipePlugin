package gaya.pe.kr.core.util.method;

import gaya.pe.kr.core.util.filter.Filter;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class UtilMethod {
    public static String removeColor(String s) {
        return s.replaceAll("(§|&)[0-9A-FK-ORa-fk-or]", "");
    }

    public static String removeChar(String paramString) {
        return paramString.replaceAll("[^0-9]", "");
    }

    public static String getOneLineString(String[] args, int startIndex) {
        if (args.length - 1 >= startIndex) {
            StringBuilder stringBuilder = new StringBuilder();
            while (startIndex <= args.length - 1) {
                stringBuilder.append(args[startIndex]).append(" ");
                ++startIndex;
            }
            return stringBuilder.toString().replace("&", "§").trim();
        }
        return "";
    }

    public static int getHasPlayerItemAmount(ItemStack targetItemStack, PlayerInventory playerInventory) {
        int amount = 0;
        ItemMeta targetItemStackItemMeta = targetItemStack.getItemMeta();
        for (ItemStack playerItemStack : playerInventory.getStorageContents()) {
            if (playerItemStack == null || !playerItemStack.getType().equals((Object) targetItemStack.getType()) || playerItemStack.getDurability() != targetItemStack.getDurability())
                continue;
            amount += UtilMethod.getPlayerItemAmountAsEqualTargetItem(playerItemStack, targetItemStackItemMeta);
        }
        return amount;
    }

    public static boolean hasItem(ItemStack itemStack, PlayerInventory playerInventory) {
        for (ItemStack storageContent : playerInventory.getStorageContents()) {
            if (Filter.isNullOrAirItem(storageContent) || !itemStack.getType().equals((Object) storageContent.getType()) || itemStack.getAmount() != storageContent.getAmount() || !UtilMethod.getItemDisplay(itemStack).equals(UtilMethod.getItemDisplay(storageContent)))
                continue;
            return true;
        }
        return false;
    }

    private static int getPlayerItemAmountAsEqualTargetItem(ItemStack playerItemStack, ItemMeta targetItemStackItemMeta) {
        ItemMeta playerItemMeta = playerItemStack.getItemMeta();
        int amount = 0;
        if (targetItemStackItemMeta.hasDisplayName() && playerItemMeta.hasDisplayName()) {
            if (targetItemStackItemMeta.getDisplayName().equals(playerItemMeta.getDisplayName())) {
                amount = playerItemStack.getAmount();
            }
        } else if (!targetItemStackItemMeta.hasDisplayName() && !playerItemMeta.hasDisplayName()) {
            amount = playerItemStack.getAmount();
        }
        return amount;
    }

    public static Player getOnlinePlayer(String targetPlayerName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String onlinePlayerName;
            if (onlinePlayer == null || !onlinePlayer.isOnline() || !targetPlayerName.equals(onlinePlayerName = UtilMethod.removeColor(onlinePlayer.getName())))
                continue;
            return onlinePlayer;
        }
        return null;
    }

    public static String getItemDisplay(ItemStack targetItem) {
        String displayName = "AIR";
        if (!Filter.isNullOrAirItem(targetItem)) {
            ItemMeta itemMeta;
            displayName = targetItem.getType().name();
            if (targetItem.hasItemMeta() && (itemMeta = targetItem.getItemMeta()).hasDisplayName()) {
                displayName = itemMeta.getDisplayName();
            }
        }
        return displayName;
    }

    public static boolean giveReward(Player player, ItemStack[] rewardContents) {
        if (rewardContents != null) {
            for (ItemStack itemStack : rewardContents) {
                if (itemStack == null || itemStack.getType().equals((Object) Material.AIR)) continue;
                player.getInventory().addItem(new ItemStack[]{itemStack});
            }
            return true;
        }
        return false;
    }

    public static int deletePlayerItem(ItemStack targetItemStack, int amount, List<ItemStack> itemStackList) {
        int result = 0;
        ItemMeta targetItemMeta = targetItemStack.getItemMeta();
        for (ItemStack playerItemStack : itemStackList) {
            int hasAmount;
            if (amount <= 0) break;
            if (playerItemStack == null || !playerItemStack.getType().equals((Object) targetItemStack.getType()) || playerItemStack.getDurability() != targetItemStack.getDurability() || (hasAmount = UtilMethod.getPlayerItemAmountAsEqualTargetItem(playerItemStack, targetItemMeta)) == 0)
                continue;
            if (hasAmount >= amount) {
                playerItemStack.setAmount(hasAmount - amount);
                result += amount;
            } else {
                result += hasAmount;
                playerItemStack.setAmount(0);
            }
            amount -= hasAmount;
        }
        return result;
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

    public static Location getLocation(String locationStr) {
        String[] locationData = locationStr.split(",");
        String worldName = locationData[0];
        double x = Double.parseDouble(locationData[1]);
        double y = Double.parseDouble(locationData[2]);
        double z = Double.parseDouble(locationData[3]);
        return new Location(Bukkit.getWorld((String) worldName), x, y, z);
    }

    public static String getLocationStr(Location location) {
        return String.format("%s,%f,%f,%f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public static ItemStack[] defaultContents(int size) {
        return Bukkit.createInventory(null, (int) size, (String) "").getContents();
    }
}

