package gaya.pe.kr.recipe.command.tab;

import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipeTab
implements TabCompleter {
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = ((Player)sender).getPlayer();
            boolean add = player.hasPermission("cook.add");
            boolean edit = player.hasPermission("cook.edit");
            boolean delete = player.hasPermission("cook.delete");
            boolean item = player.hasPermission("cook.item");
            boolean cauldron = player.hasPermission("cook.chest");
            if (args.length == 1) {
                ArrayList<String> commands = new ArrayList<String>();
                if (add) {
                    commands.add("추가");
                }

                if (edit) {
                    commands.add("편집");
                }

                if (delete) {
                    commands.add("제거");
                }

                if (item) {
                    commands.add("장비");
                }

                if (cauldron) {
                    commands.add("재료");
                    commands.add("가마솥");
                }

                commands.add("취소");
                commands.add("가방");
                return commands;
            }
            if (args.length == 2) {
                String firstArg = args[0];
                if ((firstArg.equals("편집") || firstArg.equals("제거")) && (edit || delete)) {
                    return RecipeServiceManager.getInstance().getRecipeContainer().getRecipeNameList();
                }
                if (firstArg.equals("추가")) {
                    return Collections.singletonList("이곳에 요리 이름을 입력해주세요");
                }
            }
        }
        return null;
    }
}

