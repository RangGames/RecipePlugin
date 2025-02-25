package gaya.pe.kr.recipe.manager;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.manager.ConfigurationManager;
import gaya.pe.kr.core.util.SchedulerUtil;
import gaya.pe.kr.core.util.method.ItemModifier;
import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.recipe.command.RecipeCommand;
import gaya.pe.kr.recipe.command.tab.RecipeTab;
import gaya.pe.kr.recipe.conversation.RecipeMakePrompt;
import gaya.pe.kr.recipe.exception.InsufficientIngredientException;
import gaya.pe.kr.recipe.exception.InsufficientRecipeDataException;
import gaya.pe.kr.recipe.listener.RecipeGUI;
import gaya.pe.kr.recipe.listener.RecipeUsing;
import gaya.pe.kr.recipe.obj.Ingredient;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.obj.RecipeContainer;
import gaya.pe.kr.recipe.obj.RecipeGUIIndex;
import gaya.pe.kr.recipe.scheduler.RecipeDataBackupTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RecipeServiceManager {
    RecipeContainer recipeContainer = new RecipeContainer();
    ConfigurationManager configurationManager;
    FileConfiguration configuration;
    ConversationFactory conversationFactory;
    final String RELATIVE_PATH = "Recipe/data.yml";
    ItemStack beforeItem;
    ItemStack afterItem;
    ItemStack craftItem;
    String recipeGUICheckWord;
    String recipeGUITitle;
    String bossBarTitle;
    String cookEquipmentTitle;
    List<UUID> nowUsingRecipePlayers = new ArrayList<UUID>();
    int recipeBackUpTaskId = -1;

    public static synchronized RecipeServiceManager getInstance() {
        return RecipeServiceManager.SingleTon.RECIPE_SERVICE_MANAGER;
    }

    public void init() {
        this.configurationManager = ConfigurationManager.getInstance();
        this.conversationFactory = new ConversationFactory(RecipePlugin.getPlugin());
        this.configuration = this.configurationManager.getConfiguration("Recipe/data.yml", "Recipe/data.yml");
        this.loadConfiguration();
        RecipePlugin.registerCommand("요리", new RecipeCommand());
        RecipePlugin.registerCommandTab("요리", new RecipeTab());
        RecipePlugin.registerEvent(new RecipeGUI());
        RecipePlugin.registerEvent(new RecipeUsing());
        int cycle = 12000;
        this.recipeBackUpTaskId = SchedulerUtil.scheduleRepeatingTask(new RecipeDataBackupTask(), cycle, cycle);
    }

    public void loadConfiguration() {
        String path;
        ConfigurationSection recipeList = this.configuration.getConfigurationSection("recipe");
        if (recipeList != null) {
            for (String recipeName : recipeList.getKeys(false)) {
                String recipeItem;
                Recipe.RecipeBuilder recipeBuilder = new Recipe.RecipeBuilder(recipeName);
                path = "recipe." + recipeName;
                String resultStr = this.configuration.getString(path + ".result");
                if (resultStr != null) {
                    recipeBuilder.setResult((ItemStack) ObjectConverter.getObject(resultStr));
                }
                if ((recipeItem = this.configuration.getString(path + ".recipe_item")) != null) {
                    recipeBuilder.setRecipeItem((ItemStack) ObjectConverter.getObject(recipeItem));
                }
                try {
                    Ingredient ingredient = new Ingredient(this.configuration.getStringList(path + ".ingredient"));
                    recipeBuilder.setIngredient(ingredient);
                    recipeBuilder.setMakeTime(this.configuration.getInt(path + ".make_time", 30));
                    recipeBuilder.setsortOrder(this.configuration.getInt(path + ".sortorder", 0));
                    try {
                        Recipe recipe = recipeBuilder.build();
                        this.recipeContainer.addRecipe(recipe);
                    } catch (InsufficientRecipeDataException e) {
                        e.printStackTrace();
                    }
                } catch (InsufficientIngredientException e) {
                    RecipePlugin.log(String.format("§c%s 아이템의 재료가 지정되지 않았습니다", recipeName));
                }
            }
        } else {
            RecipePlugin.log("요리 데이터가 존재하지 않습니다");
        }
        FileConfiguration optionConfiguration = this.configurationManager.getConfiguration("Option/config.yml", "Option/config.yml");
        for (String itemName : optionConfiguration.getConfigurationSection("item").getKeys(false)) {
            path = "item." + itemName;
            Material material = Material.valueOf((String) optionConfiguration.getString(path + ".material").toUpperCase(Locale.ROOT));
            int customModelData = optionConfiguration.getInt(path + ".custom_model_data");
            String displayName = optionConfiguration.getString(path + ".display_name").replace("&", "§");
            List lore = optionConfiguration.getStringList(path + ".lore");
            if (!lore.isEmpty()) {
                lore.replaceAll(s -> ChatColor.translateAlternateColorCodes((char) '&', (String) s));
            }
            ItemStack itemStack = ItemModifier.createItemStack(material, customModelData, displayName, lore);
            switch (itemName) {
                case "before_item": {
                    this.beforeItem = itemStack;
                    break;
                }
                case "after_item": {
                    this.afterItem = itemStack;
                    break;
                }
                case "craft_item": {
                    this.craftItem = itemStack;
                }
            }
        }
        this.recipeGUICheckWord = optionConfiguration.getString("gui_title.check_word", "요리").replace("&", "§");
        this.recipeGUITitle = optionConfiguration.getString("gui_title.title", "&f%check_word%/%page%").replace("&", "§");
        this.bossBarTitle = optionConfiguration.getString("bossBar.title", "&f요리 완성 까지 남은 시간 %time% 입니다").replace("&", "§");
        this.cookEquipmentTitle = optionConfiguration.getString("gui_title.cook_equipment", "요리 장비1").replace("&", "§");
    }

    public void saveData() {
        RecipeContainer recipeContainer = this.getRecipeContainer();
        HashMap<String, Recipe> recipeHashMap = recipeContainer.getRecipeNameAsRecipe();
        if (!recipeHashMap.isEmpty()) {
            for (Recipe recipe : recipeHashMap.values()) {
                String recipeName = recipe.getRecipeName();
                String path = "recipe." + recipeName;
                Ingredient ingredient = recipe.getIngredient();
                List<ItemStack> list = ingredient.getIngredientList();
                if (!list.isEmpty()) {
                    List ingredientItemData = list.stream().map(ObjectConverter::getObjectAsString).collect(Collectors.toList());
                    this.configuration.set(path + ".ingredient", ingredientItemData);
                }
                ItemStack result = recipe.getResult();
                ItemStack recipeItem = recipe.getRecipeItem();
                int makeTime = recipe.getMakeTime();
                this.configuration.set(path + ".result", (Object) ObjectConverter.getObjectAsString(result));
                this.configuration.set(path + ".recipe_item", (Object) ObjectConverter.getObjectAsString(recipeItem));
                this.configuration.set(path + ".make_time", (Object) makeTime);
            }
            this.configurationManager.saveConfiguration(this.configuration, "Recipe/data.yml");
        }
    }

    public void close() {
        if (this.recipeBackUpTaskId != -1) {
            SchedulerUtil.cancel(this.recipeBackUpTaskId);
        }
    }

    public RecipeContainer getRecipeContainer() {
        return this.recipeContainer;
    }

    public Integer getCustomModelDataArrange(ItemMeta i) {
        if (i.hasCustomModelData()) {
            return i.getCustomModelData();
        } else {
            return 0;
        }
    }

    public List<Recipe> getAllowRecipe(Player player) {
        RecipeContainer recipeContainer = this.getRecipeContainer();

        return (List) recipeContainer.getRecipeList().stream().filter((recipe) -> {
            return player.hasPermission("recipe." + recipe.getRecipeName());
        }).sorted(Comparator.comparing((recipe) -> {
            return getCustomModelDataArrange(((ItemMeta) Objects.requireNonNull(recipe.getRecipeItem().getItemMeta())));
        })).collect(Collectors.toList());

    }

    public void startRecipe(Player player, Recipe recipe) {
        if (!this.nowUsingRecipePlayers.contains(player.getUniqueId())) {
            Conversation conversation = this.conversationFactory.withFirstPrompt((Prompt) new RecipeMakePrompt(recipe)).withLocalEcho(false).buildConversation((Conversable) player);
            conversation.begin();
            this.addNowUsingRecipePlayer(player);
        } else {
            player.sendRawMessage(String.format("%s §c이미 요리를 진행중입니다", RecipePlugin.getPREFIX()));
        }
        player.closeInventory();
    }

    public void removeNowUsingRecipePlayer(Player player) {
        this.nowUsingRecipePlayers.remove(player.getUniqueId());
    }

    public void addNowUsingRecipePlayer(Player player) {
        this.nowUsingRecipePlayers.add(player.getUniqueId());
    }

    public void openRecipeBook(Player player, int page) {
        List<Recipe> recipes = this.getAllowRecipe(player);
        if (recipes.isEmpty()) {
            RecipePlugin.msg(player, "&c제작할 수 있는 요리가 없습니다");
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, (int) 54, (String) this.getRecipeGUITitle().replace("%check_word%", this.getRecipeGUICheckWord()).replace("%page%", Integer.toString(page)).replace("&", "§"));
        int firstIndex = (page - 1) * 9;
        int recipeSize = recipes.size();
        if (firstIndex > recipeSize || firstIndex < 0) {
            RecipePlugin.msg(player, "&c접근 할 수 없는 경로 입니다");
            return;
        }
        for (int index = 0; index <= 8; ++index) {
            int inputIndex = firstIndex + index;
            if (recipeSize <= inputIndex) continue;
            Recipe recipe = recipes.get(inputIndex);
            inventory.setItem(Ingredient.getInventoryIndex(index), recipe.getResult());
        }
        inventory.setItem(RecipeGUIIndex.CRAFT.getIndex(), this.craftItem);
        inventory.setItem(RecipeGUIIndex.BEFORE.getIndex(), this.beforeItem);
        inventory.setItem(RecipeGUIIndex.AFTER.getIndex(), this.afterItem);
        player.openInventory(inventory);
    }

    public void controlRecipe(Player player, String recipeName, int makeTime) {
        String title = String.format("[RECIPE] %s/%d", recipeName, makeTime);
        Inventory inventory = Bukkit.createInventory(null, (int) 45, (String) title);
        RecipeContainer recipeContainer = this.getRecipeContainer();
        if (recipeContainer.existRecipeName(recipeName)) {
            Recipe recipe = recipeContainer.getRecipe(recipeName);
            Ingredient ingredient = recipe.getIngredient();
            ItemStack recipeItem = recipe.getRecipeItem();
            ItemStack result = recipe.getResult();
            inventory.setItem(14, recipeItem);
            inventory.setItem(32, result);
            List<ItemStack> ingredientList = ingredient.getIngredientList();
            int size = ingredientList.size();
            for (int i = 0; i < size; ++i) {
                inventory.setItem(Ingredient.getInventoryIndex(i), ingredientList.get(i));
            }
        } else {
            ItemStack recipeItemGuideLine = ItemModifier.createItemStack(Material.RED_DYE, 0, "§f이곳에 레시피 아이템을 넣어주세요", null);
            ItemStack resultItemGuideLine = ItemModifier.createItemStack(Material.RED_DYE, 0, "§f이곳에 결과물 아이템을 넣어주세요", null);
            inventory.setItem(14, recipeItemGuideLine);
            inventory.setItem(32, resultItemGuideLine);
        }
        ItemStack guideLineItem = ItemModifier.createItemStack(Material.RED_WOOL, 0, "§f재료 위치 파악을 위해 설치한 아이템 입니다 안쪽에 설치하시면 됩니다", null);
        for (int a = 0; a <= 4; ++a) {
            int anotherIndex = a + 36;
            inventory.setItem(a, guideLineItem);
            inventory.setItem(anotherIndex, guideLineItem);
            if (a != 4 && a != 0) continue;
            for (int row = 0; row <= 4; ++row) {
                inventory.setItem(a + row * 9, guideLineItem);
            }
        }
        ItemStack saveItem = ItemModifier.createItemStack(Material.PAPER, 0, "§6저장", Collections.singletonList("§f클릭 시, 현재 상태를 저장합니다"));
        ;
        inventory.setItem(23, saveItem);
        player.openInventory(inventory);
    }

    public String getRecipeGUICheckWord() {
        return this.recipeGUICheckWord;
    }

    public String getRecipeGUITitle() {
        return this.recipeGUITitle;
    }

    public String getBossBarTitle() {
        return this.bossBarTitle;
    }

    public String getCookEquipmentTitle() {
        return this.cookEquipmentTitle;
    }

    private static class SingleTon {
        private static final RecipeServiceManager RECIPE_SERVICE_MANAGER = new RecipeServiceManager();

        private SingleTon() {
        }
    }
}

