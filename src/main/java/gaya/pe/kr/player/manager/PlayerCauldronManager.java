package gaya.pe.kr.player.manager;

import gaya.pe.kr.core.manager.ConfigurationManager;
import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.player.data.PlayerPersistent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerCauldronManager {
    FileConfiguration configuration;
    ConfigurationManager configurationManager;
    final String RELATIVE_PATH = "player/cauldron_data.yml";
    HashMap<UUID, PlayerPersistent> playerCauldronHashMap = new HashMap();

    public static PlayerCauldronManager getInstance() {
        return SingleTon.PLAYER_CAULDRON_MANAGER;
    }

    public void init() {
/*        this.configurationManager = ConfigurationManager.getInstance();
        this.configuration = this.configurationManager.getConfiguration("player/cauldron_data.yml", "player/cauldron_data.yml");
        try {
            for (String playerUUIDStr : this.configuration.getConfigurationSection("players").getKeys(false)) {
                UUID playerUUID = UUID.fromString(playerUUIDStr);
                String path = "players." + playerUUIDStr;
                ItemStack[] cauldronContents = (ItemStack[])ObjectConverter.getObject(this.configuration.getString(path + ".cauldron"));
                ItemStack[] virtualInventoryContents = (ItemStack[])ObjectConverter.getObject(this.configuration.getString(path + ".virtual_inventory"));
                PlayerPersistent playerPersistent = new PlayerPersistent(playerUUID);
                if (this.configuration.getString(path + ".cook_equipment", null) != null) {
                    ItemStack cookEquipment = (ItemStack)ObjectConverter.getObject(this.configuration.getString(path + ".cook_equipment"));
                    playerPersistent.setCookEquipment(cookEquipment);
                } else {
                    playerPersistent.setCookEquipment(null);
                }
                playerPersistent.setVirtualInventory(virtualInventoryContents);
                playerPersistent.setItemStacks(cauldronContents);
                this.playerCauldronHashMap.put(playerUUID, playerPersistent);
            }
        } catch (NullPointerException nullPointerException) {
        }*/
    }

    public void close() {
/*        for (PlayerPersistent value : this.playerCauldronHashMap.values()) {
            String path = "players." + value.getUuid().toString();
            this.configuration.set(path + ".cauldron", (Object)ObjectConverter.getObjectAsString(value.getItemStacks()));
            this.configuration.set(path + ".virtual_inventory", (Object)ObjectConverter.getObjectAsString(value.getVirtualInventory()));
            if (value.getCookEquipment() != null) {
                this.configuration.set(path + ".cook_equipment", (Object)ObjectConverter.getObjectAsString(value.getCookEquipment()));
                continue;
            }
            this.configuration.set(path + ".cook_equipment", null);
        }
        this.configurationManager.saveConfiguration(this.configuration, "player/cauldron_data.yml"); */
    }

    public PlayerPersistent getPlayerCauldron(Player player) {
        PlayerPersistent playerPersistent;
        UUID playerUUID = player.getUniqueId();
        if (this.playerCauldronHashMap.containsKey(playerUUID)) {
            playerPersistent = this.playerCauldronHashMap.get(playerUUID);
        } else {
            playerPersistent = new PlayerPersistent(playerUUID);
            this.playerCauldronHashMap.put(playerUUID, playerPersistent);
        }
        return playerPersistent;
    }

    public PlayerPersistent getPlayerCauldron(UUID playerUUID) {
        PlayerPersistent playerPersistent;
        if (this.playerCauldronHashMap.containsKey(playerUUID)) {
            playerPersistent = this.playerCauldronHashMap.get(playerUUID);
        } else {
            playerPersistent = new PlayerPersistent(playerUUID);
            this.playerCauldronHashMap.put(playerUUID, playerPersistent);
        }
        return playerPersistent;
    }
    public Set<UUID> getActivePlayers() {
        return new HashSet<>(playerCauldronHashMap.keySet());
    }
    private static class SingleTon {
        private static final PlayerCauldronManager PLAYER_CAULDRON_MANAGER = new PlayerCauldronManager();

        private SingleTon() {
        }
    }
}

