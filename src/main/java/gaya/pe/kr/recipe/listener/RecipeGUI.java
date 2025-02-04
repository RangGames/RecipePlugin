package gaya.pe.kr.recipe.listener;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.core.util.method.UtilMethod;
import gaya.pe.kr.recipe.exception.InsufficientRecipeDataException;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Ingredient;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.obj.RecipeContainer;
import gaya.pe.kr.recipe.obj.RecipeGUIIndex;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class RecipeGUI
implements Listener {
    @EventHandler
    public void usingRecipeGUI(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        String title = event.getView().getTitle();
        Player player = (Player)event.getWhoClicked();
        PlayerInventory playerInventory = player.getInventory();
        if (title.equals("§8요리 가방")) {
            return;
        }
        if (title.equals("§7요리 재료")) {
            return;
        }
        if (title.contains(RecipeServiceManager.getInstance().getRecipeGUICheckWord())) {
            event.setCancelled(true);
            if (!clickedInventory.equals(playerInventory)) {
                int page;
                try {
                    page = Integer.parseInt(UtilMethod.removeChar(UtilMethod.removeColor(title)));
                } catch (Exception ignored) {
                    return;
                }
                RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
                RecipeContainer recipeContainer = recipeServiceManager.getRecipeContainer();
                int clickedSlot = event.getSlot();
                Inventory inventory = event.getInventory();
                int beforeIndex = RecipeGUIIndex.BEFORE.getIndex();
                int craftIndex = RecipeGUIIndex.CRAFT.getIndex();
                int afterIndex = RecipeGUIIndex.AFTER.getIndex();
                if (clickedSlot == beforeIndex) {
                    recipeServiceManager.openRecipeBook(player, page - 1);
                } else if (clickedSlot == afterIndex) {
                    recipeServiceManager.openRecipeBook(player, page + 1);
                } else if (clickedSlot == craftIndex) {
                    ItemStack recipeItem = inventory.getItem(RecipeGUIIndex.RECIPE_ITEM.getIndex());
                    if (!Filter.isNullOrAirItem(recipeItem)) {
                        Recipe recipe = recipeContainer.getRecipe(recipeItem);
                        recipeServiceManager.startRecipe(player, recipe);
                    } else {
                        RecipePlugin.msg(player,  "&c레시피를 클릭해 주세요!");
                        player.closeInventory();
                    }
                } else {
                    ItemStack clickedItem = event.getCurrentItem();
                    if (recipeContainer.isResultItem(clickedItem)) {
                        Recipe recipe = recipeContainer.getRecipeFromResult(clickedItem);
                        Ingredient ingredient = recipe.getIngredient();
                        List<ItemStack> ingredientItemList = ingredient.getIngredientList();
                        if (!ingredientItemList.isEmpty()) {
                            ItemStack air = new ItemStack(Material.AIR);
                            for (int a = 5; a <= 7; ++a) {
                                int index = a + 9;
                                inventory.setItem(index, air);
                                inventory.setItem(index + 9, air);
                                inventory.setItem(index + 18, air);
                            }
                            for (int i = 0; i < ingredientItemList.size(); ++i) {
                                inventory.setItem(Ingredient.getInventoryIngredientIndex(i), ingredientItemList.get(i));
                            }
                            inventory.setItem(RecipeGUIIndex.RECIPE_ITEM.getIndex(), recipe.getRecipeItem());
                            inventory.setItem(RecipeGUIIndex.RESULT.getIndex(), recipe.getResult());
                        } else {
                            RecipePlugin.msg(player, "&c해당 레시피의 필요 재료가 저장되지 않았습니다. 관리자에게 문의하세요");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void saveRecipe(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        String title = event.getView().getTitle();
        Player player = (Player)event.getWhoClicked();
        if (title.contains("[RECIPE]")) {
            int clickedIndex = event.getSlot();
            String[] titleData = title.replace("[RECIPE]", "").trim().split("/");
            String recipeName = titleData[0];
            int makeTime = Integer.parseInt(titleData[1]);
            if (clickedIndex == 23) {
                event.setCancelled(true);
                Inventory recipeInventory = event.getInventory();
                ItemStack recipeItem = recipeInventory.getItem(14);
                ItemStack resultItem = recipeInventory.getItem(32);
                if (Filter.isNullOrAirItem(recipeItem) || Filter.isNullOrAirItem(resultItem)) {
                    RecipePlugin.msg(player, "&c레시피 아이템 혹은 결과물을 지정해주세요");
                } else {
                    recipeItem = recipeItem.clone();
                    resultItem = resultItem.clone();
                    RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
                    RecipeContainer recipeContainer = recipeServiceManager.getRecipeContainer();
                    Ingredient.IngredientBuilder ingredientBuilder = new Ingredient.IngredientBuilder();
                    for (Integer integer : Ingredient.getIntegerList()) {
                        ItemStack ingredient = recipeInventory.getItem(integer.intValue());
                        if (Filter.isNullOrAirItem(ingredient)) continue;
                        ingredient = ingredient.clone();
                        switch (integer) {
                            case 10: {
                                ingredientBuilder.setFirstItemStack(ingredient);
                                break;
                            }
                            case 11: {
                                ingredientBuilder.setSecondIngredient(ingredient);
                                break;
                            }
                            case 12: {
                                ingredientBuilder.setThirdIngredient(ingredient);
                                break;
                            }
                            case 19: {
                                ingredientBuilder.setForthIngredient(ingredient);
                                break;
                            }
                            case 20: {
                                ingredientBuilder.setFifthIngredient(ingredient);
                                break;
                            }
                            case 21: {
                                ingredientBuilder.setSixthIngredient(ingredient);
                                break;
                            }
                            case 28: {
                                ingredientBuilder.setSeventhIngredient(ingredient);
                                break;
                            }
                            case 29: {
                                ingredientBuilder.setEighthIngredient(ingredient);
                                break;
                            }
                            case 30: {
                                ingredientBuilder.setNinthIngredient(ingredient);
                            }
                        }
                    }
                    Ingredient ingredient = ingredientBuilder.build();
                    if (recipeContainer.existRecipeName(recipeName)) {
                        Recipe recipe = recipeServiceManager.getRecipeContainer().getRecipe(recipeName);
                        recipe.setMakeTime(makeTime);
                        recipe.setRecipeItem(recipeItem);
                        recipe.setResult(resultItem);
                        recipe.setIngredient(ingredient);
                    } else {
                        try {
                            Recipe recipe = new Recipe.RecipeBuilder(recipeName).setMakeTime(makeTime).setRecipeItem(recipeItem).setResult(resultItem).setIngredient(ingredient).build();
                            recipeContainer.addRecipe(recipe);
                        } catch (InsufficientRecipeDataException e) {
                            RecipePlugin.msg(player, "&c" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    player.closeInventory();
                    RecipePlugin.msg(player, String.format("&b%s 레시피 아이템이 정상 저장 되었습니다", recipeName));
                }
            }
        }
    }
}

