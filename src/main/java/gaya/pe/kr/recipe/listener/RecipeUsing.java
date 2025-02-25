package gaya.pe.kr.recipe.listener;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.util.filter.Filter;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.obj.RecipeContainer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.ScopedNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeUsing
        implements Listener {
    LuckPerms luckPerms = RecipePlugin.getLuckPerms();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);


    @EventHandler
    public void usingRecipe(PlayerInteractEvent event) {
        Action action;
        Player player = event.getPlayer();
        if (event.hasItem() && Filter.isRightAction(action = event.getAction())) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            int itemAmount = handItem.getAmount();
            RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
            RecipeContainer recipeContainer = recipeServiceManager.getRecipeContainer();

            if (recipeContainer.isRecipeItem(handItem)) {
                Recipe recipe = recipeContainer.getRecipe(handItem);
                String recipeName = recipe.getRecipeName();
                ScopedNode node = Node.builder(String.format("recipe.%s", recipeName))
                        .build();

                luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
                    if (user.getCachedData().getPermissionData().checkPermission(node.getKey()).asBoolean()) {
                        Bukkit.getScheduler().runTask(RecipePlugin.getPlugin(), () -> {
                            RecipePlugin.msg(player, "&c이미 해당 레시피를 보유하고 있습니다");
                        });
                        return;
                    }

                    DataMutateResult result = user.data().add(node);

                    Bukkit.getScheduler().runTask(RecipePlugin.getPlugin(), () -> {
                        if (result.wasSuccessful()) {
                            handItem.setAmount(itemAmount - 1);
                            RecipePlugin.msg(player, String.format("%s 레시피 사용 권한을 성공적으로 획득 했습니다", recipeName));

                            luckPerms.getUserManager().saveUser(user);
                        } else {
                            RecipePlugin.msg(player, "&c이미 해당 레시피를 보유하고 있습니다");
                        }
                    });
                });
            }
        }
    }

    @EventHandler
    public void quitPlayer(PlayerQuitEvent event) {
        RecipeServiceManager.getInstance().removeNowUsingRecipePlayer(event.getPlayer());
    }

}

