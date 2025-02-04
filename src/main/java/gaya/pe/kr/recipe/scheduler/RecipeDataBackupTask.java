package gaya.pe.kr.recipe.scheduler;

import gaya.pe.kr.recipe.manager.CookManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RecipeDataBackupTask
implements Runnable {
    @Override
    public void run() {
        RecipeServiceManager.getInstance().saveData();
        CookManager.getInstance().saveData();
        /*for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.isOp()) continue;
            onlinePlayer.sendMessage("자동으로 요리 데이터가 저장되었습니다");
        }*/
    }
}

