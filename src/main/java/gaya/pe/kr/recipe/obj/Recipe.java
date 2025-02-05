package gaya.pe.kr.recipe.obj;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.SchedulerUtil;
import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.core.util.method.UtilMethod;
import gaya.pe.kr.player.data.PlayerPersistent;
import gaya.pe.kr.recipe.exception.InsufficientRecipeDataException;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Ingredient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import util.PostPostion;

public final class Recipe {
    private final String recipeName;
    private int makeTime;
    private int sortOrder;
    private ItemStack recipeItem;
    private ItemStack result;
    private Ingredient ingredient;

    private Recipe(RecipeBuilder recipeBuilder) {
        this.recipeName = recipeBuilder.recipeName;
        this.makeTime = recipeBuilder.makeTime;
        this.recipeItem = recipeBuilder.recipeItem;
        this.result = recipeBuilder.result;
        this.ingredient = recipeBuilder.ingredient;
        this.sortOrder = recipeBuilder.sortOrder;
        RecipeServiceManager.getInstance().getRecipeContainer().addRecipe(this);
    }

    public int getMaxCraftable(PlayerPersistent playerPersistent) {
        List<ItemStack> inventoryItems = new ArrayList<>();
        for (ItemStack item : playerPersistent.getItemStacks()) {
            if (item != null && !item.getType().isAir()) {
                inventoryItems.add(item);
            }
        }
        List<ItemStack> ingredients = this.getIngredient().getIngredientList();
        int maxCraftable = Integer.MAX_VALUE;
        for (ItemStack ingredient : ingredients) {
            int requiredAmount = ingredient.getAmount();
            int availableAmount = Filter.getPlayerItemCount(inventoryItems, ingredient);
            int craftableForIngredient = availableAmount / requiredAmount;
            maxCraftable = Math.min(maxCraftable, craftableForIngredient);
        }
        return maxCraftable == Integer.MAX_VALUE ? 0 : maxCraftable;
    }

    public boolean makeItem(UUID uuid, PlayerPersistent playerPersistent, List<ItemStack> itemStackList, int amount) {
        Ingredient ingredientObj = this.getIngredient();
        List<ItemStack> ingredientList = ingredientObj.getIngredientList();
        ArrayList<ItemStack> rollbackItemList = new ArrayList<ItemStack>();
        Player player = Bukkit.getPlayer((UUID)uuid);
        Inventory inventory = Bukkit.createInventory(null, (int)54);
        inventory.setContents(playerPersistent.getVirtualInventory());
        if (UtilMethod.getPlayerRemainInventory(inventory.getContents()) < 1) {
            return false;
        }
        for (int a = 1; a <= amount; ++a) {
            boolean rollback = false;
            for (ItemStack ingredient : ingredientList) {
                int removeIngredientAmount = ingredient.getAmount();
                int removeAmount = UtilMethod.deletePlayerItem(ingredient, removeIngredientAmount, itemStackList);
                ItemStack cloneIngredient = ingredient.clone();
                cloneIngredient.setAmount(removeAmount);
                String errorMsg = String.format("%s&7"+ PostPostion.postpostion(UtilMethod.getItemDisplay(ingredient),2) +" %d개 부족합니다", UtilMethod.getItemDisplay(ingredient), removeIngredientAmount - removeAmount);
                if (removeAmount == 0) {
                    RecipePlugin.msg(player, errorMsg);
                    rollback = true;
                    continue;
                }
                if (removeAmount != removeIngredientAmount) {
                    rollbackItemList.add(cloneIngredient);
                    RecipePlugin.msg(player, errorMsg);
                    rollback = true;
                    continue;
                }
                rollbackItemList.add(cloneIngredient);
            }
            if (rollback) {
                if (!rollbackItemList.isEmpty()) {
                    for (ItemStack item : rollbackItemList) {
                        this.giveItem(playerPersistent, player, item, false);
                        RecipePlugin.msg(player, String.format("%s&7" + PostPostion.postpostion(UtilMethod.getItemDisplay(item),2) +" %d개 반환되었습니다", UtilMethod.getItemDisplay(item), item.getAmount()));
                    }
                }
                return false;
            }
            this.giveItem(playerPersistent, player, this.getResult(), true);
            rollbackItemList.clear();
        }
        return true;
    }

    private void giveItem(PlayerPersistent playerPersistent, Player player, ItemStack itemStack, boolean giveMsg) {
        boolean playerNull;
        boolean bl = playerNull = player == null;
        if (!playerPersistent.addItem(itemStack)) {
            if (!playerNull) {
                Location playerLocation = player.getLocation();
                playerLocation.getWorld().dropItemNaturally(playerLocation, itemStack);
                if (giveMsg) {
                    new TempBossbar(player, "§c공간 부족으로 요리가 바닥에 떨어졌습니다");
                }
            }
        } else if (!playerNull && giveMsg) {
            new TempBossbar(player, String.format("%s§f"+ PostPostion.postpostion(this.recipeName,2) +" 제작되어 요리 가방으로 이동되었습니다", this.recipeName));
        }
    }

    public void setMakeTime(int makeTime) {
        this.makeTime = makeTime;
    }

    public void setRecipeItem(ItemStack recipeItem) {
        this.recipeItem = recipeItem;
    }

    public void setResult(ItemStack result) {
        this.result = result;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public String getRecipeName() {
        return this.recipeName;
    }

    public int getMakeTime() {
        return this.makeTime;
    }
    public int getSortOrder() {
        return this.sortOrder;
    }

    public ItemStack getRecipeItem() {
        return this.recipeItem.clone();
    }

    public ItemStack getGUIItem() {
        return this.result.clone();
    }

    public ItemStack getResult() {
        return this.result.clone();
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public static class RecipeBuilder {
        String recipeName;
        int makeTime = -1;
        int sortOrder = 0;
        ItemStack recipeItem;
        ItemStack result;
        Ingredient ingredient;

        public RecipeBuilder(String recipeName) {
            this.recipeName = recipeName;
        }

        public RecipeBuilder setMakeTime(int makeTime) {
            this.makeTime = makeTime;
            return this;
        }
        public RecipeBuilder setsortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public RecipeBuilder setRecipeItem(ItemStack recipeItem) {
            this.recipeItem = recipeItem;
            return this;
        }

        public RecipeBuilder setResult(ItemStack result) {
            this.result = result;
            return this;
        }

        public RecipeBuilder setIngredient(Ingredient ingredient) {
            this.ingredient = ingredient;
            return this;
        }

        public Recipe build() throws InsufficientRecipeDataException {
            if (this.makeTime == -1) {
                throw new InsufficientRecipeDataException(this.recipeName + "의 요리 시간이 지정되지 않았습니다");
            }
            if (this.recipeItem == null) {
                throw new InsufficientRecipeDataException(this.recipeName + "의 레시피 아이템이 지정되지 않았습니다");
            }
            if (this.result == null) {
                throw new InsufficientRecipeDataException(this.recipeName + "의 요리 결과가 지정되지 않았습니다");
            }
            if (this.ingredient == null) {
                throw new InsufficientRecipeDataException(this.recipeName + "의 재료가 지정되지 않았습니다");
            }
            return new Recipe(this);
        }
    }
}

