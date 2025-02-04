package gaya.pe.kr.recipe.conversation;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.scheduler.CookTimeBossBar;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipeMakePrompt
extends StringPrompt {
    Recipe recipe;

    public RecipeMakePrompt(Recipe recipe) {
        this.recipe = recipe;
    }

    @NotNull
    public String getPromptText(@NotNull ConversationContext context) {
        return String.format("%s §a제작하실 요리의 개수를 입력해주세요", RecipePlugin.getPREFIX());
    }

    @Nullable
    public Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        if (context.getForWhom() instanceof Player) {
            Player player = (Player)context.getForWhom();
            try {
                int amount = Integer.parseInt(input);
                if (amount > 0) {
                    if (amount < 10000) {
                        player.sendRawMessage(String.format("%s §a%s %d개가 입력되었습니다.", RecipePlugin.getPREFIX(), this.recipe.getRecipeName(), amount));
                        CookTimeBossBar cookTimeBossBar = new CookTimeBossBar(player, this.recipe, amount, RecipeServiceManager.getInstance().getBossBarTitle());
                        cookTimeBossBar.start();
                    } else {
                        player.sendRawMessage(String.format("%s §c개수를 10000개 미만으로 제한합니다", RecipePlugin.getPREFIX()));
                    }
                } else {
                    player.sendRawMessage(String.format("%s §c개수는 0보다 커야합니다", RecipePlugin.getPREFIX()));
                }
            } catch (NullPointerException | NumberFormatException e) {
                player.sendRawMessage(String.format("%s §c숫자를 입력해주세요", RecipePlugin.getPREFIX()));
            }
            RecipeServiceManager.getInstance().removeNowUsingRecipePlayer(player);
        }
        return null;
    }
}

