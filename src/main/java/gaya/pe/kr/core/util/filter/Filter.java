package gaya.pe.kr.core.util.filter;

import gaya.pe.kr.core.util.method.UtilMethod;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public class Filter {
    static Random random = new Random();

    public static boolean isArmorItem(ItemStack itemStack) {
        if (itemStack != null) {
            String handItemMaterialName = itemStack.getType().name().toLowerCase();
            return handItemMaterialName.contains("helmet") || handItemMaterialName.contains("chestplate") || handItemMaterialName.contains("leggings") || handItemMaterialName.contains("boots") || handItemMaterialName.contains("cap") || handItemMaterialName.contains("tunic") || handItemMaterialName.contains("pants");
        }
        return false;
    }

    public static boolean isWeapon(ItemStack itemStack) {
        String handItemMaterialName = itemStack.getType().name().toLowerCase();
        return handItemMaterialName.contains("sword") || handItemMaterialName.contains("bow");
    }

    public static boolean isBow(ItemStack itemStack) {
        String handItemMaterialName = itemStack.getType().name().toLowerCase();
        return handItemMaterialName.contains("bow");
    }

    public static boolean isSword(ItemStack itemStack) {
        String handItemMaterialName = itemStack.getType().name().toLowerCase();
        return handItemMaterialName.contains("sword");
    }

    public static boolean isSuccessRandom(int probability) {
        return random.nextInt(100) < probability;
    }

    public static boolean isSuccessRandom(double probability, int multiply) {
        return (double)random.nextInt(100 * multiply) < probability * (double)multiply;
    }

    public static boolean isMatchItem(ItemStack itemStack, ItemStack targetItem) {
        Material targetItemMaterial;
        Material material;
        if (itemStack != null && targetItem != null && (material = itemStack.getType()).equals((Object)(targetItemMaterial = targetItem.getType()))) {
            return UtilMethod.getItemDisplay(itemStack).equals(UtilMethod.getItemDisplay(targetItem));
        }
        return false;
    }

    public static boolean isWarTime() {
        Calendar nowCal = Calendar.getInstance();
        int nowHour = nowCal.get(11);
        return nowHour >= 15 && nowHour <= 18 || nowHour >= 21;
    }

    public static boolean canChangeNickName(String nickName) {
        return !Pattern.matches("^[가-힣]*$", UtilMethod.removeColor(nickName));
    }

    public static boolean isRightAction(Action action) {
        return action.equals((Object)Action.RIGHT_CLICK_AIR) || action.equals((Object)Action.RIGHT_CLICK_BLOCK);
    }

    public static boolean isNullOrAirItem(ItemStack itemStack) {
        if (itemStack == null) {
            return true;
        }
        return itemStack.getType().equals((Object)Material.AIR);
    }
}

