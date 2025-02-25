package gaya.pe.kr.recipe.manager;

import gaya.pe.kr.core.manager.ConfigurationManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.obj.RecipeContainer;
import gaya.pe.kr.recipe.scheduler.CookTimeBossBar;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.FileConfiguration;

public class CookManager {
    ConfigurationManager configurationManager;
    final String cookFilePath = "data/cook.yml";
    FileConfiguration configuration;
    HashMap<UUID, CookTimeBossBar> cookTimeBossBarHashMap = new HashMap();
    private final Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();

    public void setLoading(UUID uuid, boolean loading) {
        if (loading) {
            loadingPlayers.add(uuid);
        } else {
            loadingPlayers.remove(uuid);
        }
    }

    public boolean isLoading(UUID uuid) {
        return loadingPlayers.contains(uuid);
    }

    public static CookManager getInstance() {
        return SingleTon.COOK_MANAGER;
    }

    public void cleanupPlayer(UUID uuid) {
        CookTimeBossBar cookTimeBossBar = getCook(uuid);
        if (cookTimeBossBar != null) {
            cookTimeBossBar.interrupt();
            removeCook(uuid);
        }
    }

    public void init() {
/*        this.configurationManager = ConfigurationManager.getInstance();
        this.configuration = this.configurationManager.getConfiguration("data/cook.yml", "data/cook.yml");
        RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
        RecipeContainer recipeContainer = recipeServiceManager.getRecipeContainer();
        Set<String> uuidList = this.configurationManager.getConfigurationSection(this.configuration, "data");
        if (uuidList != null && !uuidList.isEmpty()) {
            for (String uuid : uuidList) {
                String path = "data." + uuid;
                int time = this.configuration.getInt(path + ".time");
                int makeTime = this.configuration.getInt(path + ".makeTime");
                int amount = this.configuration.getInt(path + ".amount");
                int nowMakeAmount = this.configuration.getInt(path + ".nowMakeAmount");
                String recipeName = this.configuration.getString(path + ".recipeName");
                Recipe recipe = recipeContainer.getRecipe(recipeName);
                if (recipe == null) continue;
                CookTimeBossBar cookTimeBossBar = new CookTimeBossBar(UUID.fromString(uuid), recipe, RecipeServiceManager.getInstance().getBossBarTitle(), amount, nowMakeAmount, time, makeTime);
                cookTimeBossBar.start();
            }
        }*/
    }

    public void saveData() {
/*        this.configuration.set("data", null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        this.cookTimeBossBarHashMap.forEach((uuid, cookTimeBossBar) -> {
            String path = "data." + uuid.toString();
            Date startDate = cookTimeBossBar.getDate();
            String startDateStr = simpleDateFormat.format(startDate);
            int time = cookTimeBossBar.getTime();
            int makeTime = cookTimeBossBar.getMakeTime();
            String recipeName = cookTimeBossBar.getRecipe().getRecipeName();
            int nowMakeAmount = cookTimeBossBar.getNowMakeItemAmount();
            int amount = cookTimeBossBar.getAmount();
            this.configuration.set(path + ".start_date", (Object)startDateStr);
            this.configuration.set(path + ".time", (Object)time);
            this.configuration.set(path + ".makeTime", (Object)makeTime);
            this.configuration.set(path + ".recipeName", (Object)recipeName);
            this.configuration.set(path + ".nowMakeAmount", (Object)nowMakeAmount);
            this.configuration.set(path + ".amount", (Object)amount);
        });
        this.configurationManager.saveConfiguration(this.configuration, "data/cook.yml");*/
    }

    public void addCook(UUID uuid, CookTimeBossBar cookTimeBossBar) {
        this.cookTimeBossBarHashMap.put(uuid, cookTimeBossBar);
    }

    public void removeCook(UUID uuid) {
        this.cookTimeBossBarHashMap.remove(uuid);
    }

    public CookTimeBossBar getCook(UUID uuid) {
        return cookTimeBossBarHashMap.get(uuid);
    }

    private static class SingleTon {
        private static final CookManager COOK_MANAGER = new CookManager();

        private SingleTon() {
        }
    }
}

