package gaya.pe.kr.recipe.obj;

import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.recipe.exception.InsufficientIngredientException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class Ingredient {
    List<ItemStack> ingredientList = new ArrayList<ItemStack>();
    static List<Integer> integerList = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);

    public Ingredient(List<?> ingredientList) throws InsufficientIngredientException {
        for (Object o : ingredientList) {
            if (o instanceof String) {
                this.addIngredient((ItemStack)ObjectConverter.getObject((String)o));
                continue;
            }
            if (!(o instanceof ItemStack)) continue;
            this.addIngredient((ItemStack)o);
        }
    }

    private Ingredient(IngredientBuilder ingredientBuilder) {
        this.addIngredient(ingredientBuilder.firstItemStack);
        this.addIngredient(ingredientBuilder.secondIngredient);
        this.addIngredient(ingredientBuilder.thirdIngredient);
        this.addIngredient(ingredientBuilder.forthIngredient);
        this.addIngredient(ingredientBuilder.fifthIngredient);
        this.addIngredient(ingredientBuilder.sixthIngredient);
        this.addIngredient(ingredientBuilder.seventhIngredient);
        this.addIngredient(ingredientBuilder.eighthIngredient);
        this.addIngredient(ingredientBuilder.ninthIngredient);
    }

    public List<ItemStack> getIngredientList() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (ItemStack itemStack : this.ingredientList) {
            list.add(itemStack.clone());
        }
        return list;
    }

    public static int getInventoryIndex(int ingredientIndex) {
        switch (ingredientIndex) {
            case 0: {
                return integerList.get(0);
            }
            case 1: {
                return integerList.get(1);
            }
            case 2: {
                return integerList.get(2);
            }
            case 3: {
                return integerList.get(3);
            }
            case 4: {
                return integerList.get(4);
            }
            case 5: {
                return integerList.get(5);
            }
            case 6: {
                return integerList.get(6);
            }
            case 7: {
                return integerList.get(7);
            }
            case 8: {
                return integerList.get(8);
            }
        }
        return -1;
    }

    public static int getInventoryIngredientIndex(int ingredientIndex) {
        switch (ingredientIndex) {
            case 0: {
                return 14;
            }
            case 1: {
                return 15;
            }
            case 2: {
                return 16;
            }
            case 3: {
                return 23;
            }
            case 4: {
                return 24;
            }
            case 5: {
                return 25;
            }
            case 6: {
                return 32;
            }
            case 7: {
                return 33;
            }
            case 8: {
                return 34;
            }
        }
        return -1;
    }

    private void addIngredient(ItemStack itemStack) {
        if (!Filter.isNullOrAirItem(itemStack)) {
            this.ingredientList.add(itemStack);
        }
    }

    public static List<Integer> getIntegerList() {
        return integerList;
    }

    public static class IngredientBuilder {
        static ItemStack air = new ItemStack(Material.AIR);
        ItemStack firstItemStack = air;
        ItemStack secondIngredient = air;
        ItemStack thirdIngredient = air;
        ItemStack forthIngredient = air;
        ItemStack fifthIngredient = air;
        ItemStack sixthIngredient = air;
        ItemStack seventhIngredient = air;
        ItemStack eighthIngredient = air;
        ItemStack ninthIngredient = air;

        public IngredientBuilder setFirstItemStack(ItemStack firstItemStack) {
            this.firstItemStack = firstItemStack;
            return this;
        }

        public IngredientBuilder setSecondIngredient(ItemStack secondIngredient) {
            this.secondIngredient = secondIngredient;
            return this;
        }

        public IngredientBuilder setThirdIngredient(ItemStack thirdIngredient) {
            this.thirdIngredient = thirdIngredient;
            return this;
        }

        public IngredientBuilder setForthIngredient(ItemStack forthIngredient) {
            this.forthIngredient = forthIngredient;
            return this;
        }

        public IngredientBuilder setFifthIngredient(ItemStack fifthIngredient) {
            this.fifthIngredient = fifthIngredient;
            return this;
        }

        public IngredientBuilder setSixthIngredient(ItemStack sixthIngredient) {
            this.sixthIngredient = sixthIngredient;
            return this;
        }

        public IngredientBuilder setSeventhIngredient(ItemStack seventhIngredient) {
            this.seventhIngredient = seventhIngredient;
            return this;
        }

        public IngredientBuilder setEighthIngredient(ItemStack eighthIngredient) {
            this.eighthIngredient = eighthIngredient;
            return this;
        }

        public IngredientBuilder setNinthIngredient(ItemStack ninthIngredient) {
            this.ninthIngredient = ninthIngredient;
            return this;
        }

        public Ingredient build() {
            return new Ingredient(this);
        }
    }
}

