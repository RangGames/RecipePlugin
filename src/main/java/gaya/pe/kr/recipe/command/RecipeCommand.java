package gaya.pe.kr.recipe.command;

import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.player.reactor.CookEquipment;
import gaya.pe.kr.player.reactor.PlayerPersistentInventoryReact;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.RecipeContainer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RecipeCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = ((Player)sender).getPlayer();
            boolean add = player.hasPermission("cook.add");
            boolean edit = player.hasPermission("cook.edit");
            boolean delete = player.hasPermission("cook.delete");
            boolean item = player.hasPermission("cook.item");
            boolean cauldron = player.hasPermission("cook.chest");
            RecipeServiceManager recipeServiceManager = RecipeServiceManager.getInstance();
            RecipeContainer recipeContainer = recipeServiceManager.getRecipeContainer();
            if (args.length > 0) {
                try {
                    String category;
                    switch (category = args[0]) {
                        case "가방": {
                            PlayerCauldronManager playerCauldronManager = PlayerCauldronManager.getInstance();
                            PlayerPersistentInventoryReact playerPersistentInventoryReact = new PlayerPersistentInventoryReact(player, playerCauldronManager.getPlayerCauldron(player), true);
                            playerPersistentInventoryReact.open();
                            return true;
                        }
                        case "장비": {
                            if (item) {
                                CookEquipment cookEquipment = new CookEquipment(player);
                                cookEquipment.start();
                            }
                            return true;
                        }
                        case "재료":
                        case "가마솥": {
                            if (cauldron) {
                                PlayerCauldronManager playerCauldronManager = PlayerCauldronManager.getInstance();
                                PlayerPersistentInventoryReact playerPersistentInventoryReact = new PlayerPersistentInventoryReact(player, playerCauldronManager.getPlayerCauldron(player), false);
                                playerPersistentInventoryReact.open();
                            }
                            return true;
                        }
                    }
                    String recipeName = args[1];
                    switch (category) {
                        case "추가": {
                            if (!add) break;
                            if (!recipeContainer.existRecipeName(recipeName)) {
                                try {
                                    int makeTime = Integer.parseInt(args[2]);
                                    recipeServiceManager.controlRecipe(player, recipeName, makeTime);
                                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                                    RecipePlugin.msg(player, "&c요리 시간을 정확하게 입력해주세요");
                                }
                                break;
                            }
                            RecipePlugin.msg(player, String.format("&c%s 요리는 이미 존재하는 요리명 입니다", recipeName));
                            break;
                        }
                        case "편집": {
                            if (!edit) break;
                            if (recipeContainer.existRecipeName(recipeName)) {
                                try {
                                    int makeTime = Integer.parseInt(args[2]);
                                    recipeServiceManager.controlRecipe(player, recipeName, makeTime);
                                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                                    RecipePlugin.msg(player, "&c요리 시간을 정확하게 입력해주세요");
                                }
                                break;
                            }
                            RecipePlugin.msg(player, String.format("&c%s 요리는 존재하지 않습니다", recipeName));
                            break;
                        }
                        case "제거": {
                            if (!delete) break;
                            if (recipeContainer.existRecipeName(recipeName)) {
                                recipeContainer.deleteRecipe(recipeName);
                                RecipePlugin.msg(player, "&c성공적으로 레시피를 제거했습니다");
                                break;
                            }
                            RecipePlugin.msg(player, String.format("&c%s 요리는 존재하지 않습니다", recipeName));
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    RecipePlugin.msg(player, "&c요리명을 정확하게 입력해주세요");
                }
            } else {
                if (add) {
                    RecipePlugin.msg(player, "/요리 추가 (요리명) : 요리를 추가합니다");
                }

                if (edit) {
                    RecipePlugin.msg(player, "/요리 편집 (요리명) : 특정 요리를 편집합니다");
                }

                if (delete) {
                    RecipePlugin.msg(player, "/요리 제거 (요리명) : 특졍 요리를 제거합니다");
                }

                if (item) {
                    RecipePlugin.msg(player, "/요리 장비 : 요리 장비를 확인합니다");
                }

                if (cauldron) {
                    RecipePlugin.msg(player, "/요리 재료 : 요리 재료를 확인합니다");
                }
                recipeServiceManager.openRecipeBook(player, 1);
            }
        }
        return false;
    }
}

