package gaya.pe.kr.recipe.conversation;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.events.CookAddedEvent;
import gaya.pe.kr.core.events.CookStartedEvent;
import gaya.pe.kr.player.data.PlayerPersistent;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.scheduler.CookTimeBossBar;
import org.bukkit.Bukkit;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipeMakePrompt
        extends StringPrompt {
    Recipe recipe;
    private static final int MAX_AMOUNT = 10000;

    public RecipeMakePrompt(Recipe recipe) {
        this.recipe = recipe;
    }

    @NotNull
    public String getPromptText(@NotNull ConversationContext context) {
        return String.format("%s §a제작하실 요리의 개수를 입력해주세요", RecipePlugin.getPREFIX());
    }

    @Nullable
    public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        if (!(context.getForWhom() instanceof Player)) {
            return null;
        }

        Player player = (Player) context.getForWhom();
        try {
            PlayerPersistent playerPersistent = PlayerCauldronManager.getInstance().getPlayerCauldron(player);
            int maxcraft = this.recipe.getMaxCraftable(playerPersistent);
            if (maxcraft == 0) {
                sendMessage(player, "§c아이템이 부족합니다. [/요리 재료] 에 아이템을 넣어주세요.");
                return null;
            }

            int amount = parseAmount(input);
            if (amount == -1) {
                startCooking(player, maxcraft, playerPersistent, maxcraft);
            } else if (amount > 0 && amount < MAX_AMOUNT) {
                startCooking(player, amount, playerPersistent, maxcraft);
            } else if (amount >= MAX_AMOUNT) {
                sendMessage(player, "§c개수를 10000개 미만으로 제한합니다");
            } else {
                sendMessage(player, "§c개수는 0보다 커야합니다");
            }
        } catch (NumberFormatException e) {
            sendMessage(player, "§c숫자를 입력해주세요");
        } finally {
            RecipeServiceManager.getInstance().removeNowUsingRecipePlayer(player);
        }
        return null;
    }

    private void startCooking(Player player, int amount, PlayerPersistent playerPersistent, int maxcraft) {
        sendMessage(player, String.format("§a%s: %d개가 입력되었습니다.", this.recipe.getRecipeName(), amount));
        CookStartedEvent cookStartedEvent = new CookStartedEvent(player.getUniqueId(), recipe, playerPersistent.toString(), maxcraft);
        Bukkit.getPluginManager().callEvent(cookStartedEvent);
        CookTimeBossBar cookTimeBossBar = new CookTimeBossBar(player, this.recipe, amount,
                RecipeServiceManager.getInstance().getBossBarTitle());
        cookTimeBossBar.start();
    }

    private void sendMessage(Player player, String message) {
        player.sendRawMessage(RecipePlugin.getPREFIX() + " " + message);
    }

    private int parseAmount(@Nullable String input) throws NumberFormatException {
        if (input == null) {
            throw new NumberFormatException("Input is null");
        }
        return Integer.parseInt(input);
    }
}

