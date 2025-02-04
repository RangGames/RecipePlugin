package gaya.pe.kr.recipe.obj;

import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.recipe.obj.Recipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class RecipeContainer {
    HashMap<String, Recipe> recipeNameAsRecipe = new HashMap();
    HashMap<ItemStack, Recipe> recipeItemAsRecipe = new HashMap();

    public void addRecipe(Recipe recipe) {
        this.recipeNameAsRecipe.put(recipe.getRecipeName(), recipe);
        this.recipeItemAsRecipe.put(recipe.getRecipeItem(), recipe);
    }

    public boolean existRecipeName(String name) {
        return this.recipeNameAsRecipe.containsKey(name);
    }

    public void deleteRecipe(String recipeName) {
        this.recipeNameAsRecipe.remove(recipeName);
    }

    public boolean isRecipeItem(ItemStack itemStack) {
        for (Recipe value : this.recipeItemAsRecipe.values()) {
            if (!Filter.isMatchItem(value.getRecipeItem(), itemStack)) continue;
            return true;
        }
        return false;
    }
    public boolean isResultItem(ItemStack itemStack) {
        for (Recipe value : this.recipeItemAsRecipe.values()) {
            if (!Filter.isMatchItem(value.getResult(), itemStack)) continue;
            return true;
        }
        return false;
    }

    public Recipe getRecipe(ItemStack itemStack) {
        for (Recipe value : this.recipeItemAsRecipe.values()) {
            if (!Filter.isMatchItem(value.getRecipeItem(), itemStack)) continue;
            return value;
        }
        return null;
    }
    public Recipe getRecipeFromResult(ItemStack itemStack) {
        for (Recipe value : this.recipeItemAsRecipe.values()) {
            if (!Filter.isMatchItem(value.getResult(), itemStack)) continue;
            return value;
        }
        return null;
    }

    public Recipe getRecipe(String recipeName) {
        return this.recipeNameAsRecipe.get(recipeName);
    }

    public HashMap<String, Recipe> getRecipeNameAsRecipe() {
        return this.recipeNameAsRecipe;
    }

    public List<Recipe> getRecipeList() {
        return new ArrayList<Recipe>(this.getRecipeNameAsRecipe().values());
    }

    public List<String> getRecipeNameList() {
        return new ArrayList<String>(this.getRecipeNameAsRecipe().keySet());
    }
}

