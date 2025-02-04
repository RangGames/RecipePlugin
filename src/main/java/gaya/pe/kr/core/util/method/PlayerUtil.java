package gaya.pe.kr.core.util.method;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerUtil {
    public static UUID getPlayerUUID(String playerName) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (!offlinePlayer.getName().equals(playerName)) continue;
            return offlinePlayer.getUniqueId();
        }
        return null;
    }

    public static String getPlayerName(UUID targetPlayerUUID) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (!offlinePlayer.getUniqueId().equals(targetPlayerUUID)) continue;
            return offlinePlayer.getName();
        }
        return null;
    }

    public static Player getPlayer(Player player, String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayer((String)targetPlayerName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            return targetPlayer;
        }
        player.sendMessage("§c존재하지 않거나 접속중이지 않은 플레이어 입니다");
        return null;
    }

    public static Player getTargetPlayer(Player player, String[] args) {
        try {
            String targetPlayerName = args[1];
            if (!targetPlayerName.equalsIgnoreCase(player.getName())) {
                Player targetPlayer = PlayerUtil.getPlayer(player, targetPlayerName);
                if (targetPlayer != null) {
                    return targetPlayer;
                }
            } else {
                player.sendMessage("자기 자신은 지목할 수 없습니다");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            player.sendMessage("플레이어 닉네임을 정확하게 입력해주세요!");
        }
        return null;
    }

    public static boolean isOnline(Player player) {
        return player != null && player.isOnline();
    }
}

