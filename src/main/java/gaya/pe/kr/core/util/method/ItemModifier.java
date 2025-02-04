package gaya.pe.kr.core.util.method;

import gaya.pe.kr.core.util.filter.Filter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemModifier {
    public static ItemStack createItemStack(Material material, int customModelData, String displayName, List<String> lore) {
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName.replace("&", "§"));

        itemMeta.setCustomModelData(customModelData);
        if (lore != null) {
            itemMeta.setLore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static void addLore(ItemStack itemStack, List<String> loreStr) {
        if (!Filter.isNullOrAirItem(itemStack)) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            List lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList();
            for (String s : loreStr) {
                lore.add(s.replace("&", "§"));
            }
            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
        }
    }
}

