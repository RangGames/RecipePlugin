package gaya.pe.kr.core.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gaya.pe.kr.core.RecipePlugin;
import gaya.pe.kr.core.events.CookLoadingEvent;
import gaya.pe.kr.core.events.CookSavedEvent;
import gaya.pe.kr.core.util.DataVersion;
import gaya.pe.kr.core.util.PlayerDataLock;
import gaya.pe.kr.core.util.method.ObjectConverter;
import gaya.pe.kr.player.data.PlayerPersistent;
import gaya.pe.kr.player.manager.PlayerCauldronManager;
import gaya.pe.kr.recipe.manager.CookManager;
import gaya.pe.kr.recipe.manager.RecipeServiceManager;
import gaya.pe.kr.recipe.obj.Recipe;
import gaya.pe.kr.recipe.scheduler.CookTimeBossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RecipeAPI {
    private final RecipePlugin plugin;
    private HikariDataSource dataSource;
    private final Map<UUID, PlayerTransferData> transferData = new ConcurrentHashMap<>();
    private static final String LAST_SERVER_KEY = "last_server";
    private static final int DATA_LOAD_TIMEOUT = 5000;
    private final Map<UUID, PlayerDataLock> dataLocks = new ConcurrentHashMap<>();
    private final Map<UUID, DataVersion> dataVersions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> retryCount = new ConcurrentHashMap<>();
    private static final int MAX_RETRY_COUNT = 3;
    private static final int LOCK_TIMEOUT_SECONDS = 30;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "RecipeAPI-Worker-" + count.getAndIncrement());
                }
            }
    );
    private final ConcurrentHashMap<UUID, Object> transferLocks = new ConcurrentHashMap<>();

    private static class PlayerTransferData {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final long startTime = System.currentTimeMillis();
        volatile boolean transferring = true;
        volatile boolean saved = false;

        boolean isExpired() {
            return System.currentTimeMillis() - startTime > 5000;
        }
    }


    public RecipeAPI(RecipePlugin plugin) {
        this.plugin = plugin;
        setupDatabase();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                handleNetworkJoin(player.getUniqueId())
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Failed to load data for player " + player.getName() + ": " + throwable.getMessage());
                            return false;
                        });
            }
        }, 20L);

        initializeScheduledTasks();
    }

    public boolean isDataLoading(UUID uuid) {
        PlayerTransferData data = transferData.get(uuid);
        return data != null && data.transferring || CookManager.getInstance().isLoading(uuid);
    }

    private boolean acquireDataLock(UUID uuid, String server) {
        PlayerDataLock newLock = new PlayerDataLock(uuid, server, LOCK_TIMEOUT_SECONDS);
        return dataLocks.compute(uuid, (key, existingLock) -> {
            if (existingLock == null || existingLock.isExpired()) {
                return newLock;
            }
            return existingLock;
        }) == newLock;
    }

    private void releaseDataLock(UUID uuid) {
        dataLocks.remove(uuid);
    }

    private void handleTimeout(UUID uuid) {
        try {
            PlayerTransferData data = transferData.get(uuid);
            if (data != null) {
                data.transferring = false;
                data.future.complete(false);
            }
            releaseDataLock(uuid);
            setPlayerTransferring(uuid, false);
            retryCount.remove(uuid);

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling timeout for " + uuid + ": " + e.getMessage());
        }
    }

    private void recoverFromFailure(UUID uuid) {
        try {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT last_server, transfer_status FROM recipe_data WHERE uuid = ? FOR UPDATE")) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String lastServer = rs.getString("last_server");
                        boolean transferStatus = rs.getBoolean("transfer_status");

                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE recipe_data SET transfer_status = false, server_name = ? WHERE uuid = ?")) {
                            updateStmt.setString(1, lastServer);
                            updateStmt.setString(2, uuid.toString());
                            updateStmt.executeUpdate();
                        }
                    }

                    conn.commit();
                }
            }

            PlayerTransferData data = transferData.remove(uuid);
            if (data != null) {
                data.future.complete(false);
            }

            releaseDataLock(uuid);

        } catch (Exception e) {
            plugin.getLogger().severe("Error recovering from failure for " + uuid + ": " + e.getMessage());
        }
    }

    private void setupDatabase() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database.host", "localhost:3306") + "/" +
                    plugin.getConfig().getString("database.name", "recipe"));
            config.setUsername(plugin.getConfig().getString("database.user", "root"));
            config.setPassword(plugin.getConfig().getString("database.password", ""));

            config.setMaximumPoolSize(6);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(3000);
            config.setValidationTimeout(3000);
            config.setMaxLifetime(600000);
            config.setKeepaliveTime(45000);
            config.setLeakDetectionThreshold(30000);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "512");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            dataSource = new HikariDataSource(config);
            initializeDatabase();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public static class ItemStackSerializer {
        public static String serialize(ItemStack[] items) throws Exception {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 GZIPOutputStream gzip = new GZIPOutputStream(bos);
                 BukkitObjectOutputStream oos = new BukkitObjectOutputStream(gzip)) {
                oos.writeObject(items);
                oos.flush();
                return Base64.getEncoder().encodeToString(bos.toByteArray());
            }
        }

        public static ItemStack[] deserialize(String data) throws Exception {
            if (data == null || data.isEmpty()) return new ItemStack[0];
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 GZIPInputStream gzip = new GZIPInputStream(bis);
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(gzip)) {
                return (ItemStack[]) ois.readObject();
            }
        }
    }

    private void initializeDatabase() {
        String createTableSQL =
                "CREATE TABLE IF NOT EXISTS recipe_data (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "server_name VARCHAR(64) NOT NULL, " +
                        "last_server VARCHAR(64), " +
                        "cook_info TEXT, " +
                        "inventory_info TEXT, " +
                        "last_update BIGINT, " +
                        "transfer_status BOOLEAN DEFAULT FALSE, " +
                        "PRIMARY KEY (uuid)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create database table: " + e.getMessage());
        }
    }

    public void updateServerHistory(UUID uuid, String fromServer, String toServer) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE recipe_data SET last_server = ? WHERE uuid = ?")) {
                stmt.setString(1, fromServer);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update server history: " + e.getMessage());
        }
    }

    public boolean setPlayerTransferring(UUID uuid, boolean transferring) {
        try {
            if (transferring) {
                PlayerTransferData existingData = transferData.get(uuid);
                if (existingData != null && existingData.transferring) {
                    plugin.getLogger().warning("Player " + uuid + " is already being transferred");
                    return false;
                }
                transferData.put(uuid, new PlayerTransferData());
                return true;
            } else {
                handleTransferFailure(uuid);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to set player transferring status: " + e.getMessage());
            return false;
        }
    }

    public void handlePlayerQuit(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT transfer_status, last_server FROM recipe_data WHERE uuid = ? FOR UPDATE")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next() && rs.getBoolean("transfer_status")) {
                    String lastServer = rs.getString("last_server");

                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE recipe_data SET transfer_status = false WHERE uuid = ?")) {
                        updateStmt.setString(1, uuid.toString());
                        updateStmt.executeUpdate();
                    }
                    return;
                }
            }

            forceSaveOnQuit(uuid);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to handle player quit: " + e.getMessage());
        }
    }

    private void updatePlayerData(UUID uuid, String targetServer, boolean isTransferring) {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);

                String cookInfo = getPlayerCookInformation(uuid);
                String invInfo = getPlayerInventoryInformation(uuid);
                long updateTime = System.currentTimeMillis();

                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE recipe_data SET cook_info = ?, inventory_info = ?, " +
                                "last_update = ? WHERE uuid = ?")) {
                    stmt.setString(1, cookInfo);
                    stmt.setString(2, invInfo);
                    stmt.setLong(3, updateTime);
                    stmt.setString(4, uuid.toString());
                    stmt.executeUpdate();

                    conn.commit();

                    CookSavedEvent cookSavedEvent = new CookSavedEvent(uuid, cookInfo, invInfo);
                    Bukkit.getPluginManager().callEvent(cookSavedEvent);

                    PlayerTransferData data = transferData.get(uuid);
                    if (data != null) {
                        data.saved = true;
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    plugin.getLogger().severe("Failed to execute update query: " + e.getMessage());
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error during player data update: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                plugin.getLogger().severe("Unexpected error during player data update: " + e.getMessage());
                throw new RuntimeException("Failed to update player data", e);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to establish database connection: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    private void handleTransferFailure(UUID uuid) {
        PlayerTransferData data = transferData.remove(uuid);
        if (data != null) {
            asyncExecutor.execute(() -> {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE recipe_data SET transfer_status = false WHERE uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                    data.future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().severe("Transfer failure handling error: " + e.getMessage());
                    data.future.completeExceptionally(e);
                }
            });
        }
    }

    public CompletableFuture<Boolean> loadPlayerData(UUID uuid) {
        CookManager.getInstance().setLoading(uuid, true);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadDataFromDB(uuid);
            } finally {
                CookManager.getInstance().setLoading(uuid, false);
            }
        }, asyncExecutor).orTimeout(5, TimeUnit.SECONDS);
    }


    private boolean loadDataFromDB(UUID uuid) {
        final String sql = "SELECT cook_info, inventory_info FROM recipe_data WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String cookInfo = rs.getString("cook_info");
                    String invInfo = rs.getString("inventory_info");
                    return applyPlayerData(uuid, cookInfo, invInfo);
                }
                return false;
            }
        } catch (Exception e) {
            handleRetry(uuid, () -> loadDataFromDB(uuid));
            return false;
        }
    }

    public void bulkSavePlayers(Collection<UUID> uuids) {
        asyncExecutor.execute(() -> {
            final String sql = "INSERT INTO recipe_data (uuid, server_name, cook_info, inventory_info, last_update, transfer_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "server_name=VALUES(server_name), cook_info=VALUES(cook_info), " +
                    "inventory_info=VALUES(inventory_info), last_update=VALUES(last_update), transfer_status=VALUES(transfer_status)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int batchCount = 0;
                for (UUID uuid : uuids) {
                    String serverName = plugin.getConfig().getString("server-name", "unknown");
                    PlayerTransferData data = transferData.get(uuid);

                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, serverName);
                    stmt.setString(3, getPlayerCookInformation(uuid));
                    stmt.setString(4, getPlayerInventoryInformation(uuid));
                    stmt.setLong(5, System.currentTimeMillis());
                    stmt.setBoolean(6, data != null && data.transferring);
                    stmt.addBatch();

                    if (++batchCount % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
            } catch (Exception e) {
                plugin.getLogger().severe("Bulk save failed: " + e.getMessage());
            }
        });
    }

    private void initializeScheduledTasks() {
        startAutoSave();
    }

    public void savePlayerData(UUID uuid, String serverName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                String cookInfo = getPlayerCookInformation(uuid);
                String invInfo = getPlayerInventoryInformation(uuid);
                long updateTime = System.currentTimeMillis();
                PlayerTransferData data = transferData.get(uuid);
                boolean isTransferring = data != null && data.transferring;

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO recipe_data (uuid, server_name, cook_info, inventory_info, last_update, transfer_status) " +
                                "VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "server_name = ?, cook_info = ?, inventory_info = ?, last_update = ?, transfer_status = ?")) {

                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, serverName);
                    stmt.setString(3, cookInfo);
                    stmt.setString(4, invInfo);
                    stmt.setLong(5, updateTime);
                    stmt.setBoolean(6, isTransferring);

                    stmt.setString(7, serverName);
                    stmt.setString(8, cookInfo);
                    stmt.setString(9, invInfo);
                    stmt.setLong(10, updateTime);
                    stmt.setBoolean(11, isTransferring);

                    stmt.executeUpdate();
                    conn.commit();

                    if (data != null) {
                        data.saved = true;
                        if (!isTransferring) {
                            data.future.complete(true);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
            }
        });
    }

    public String getPlayerCookInformation(UUID uuid) {
        try {
            JSONObject cookData = new JSONObject();
            CookTimeBossBar cookTimeBossBar = CookManager.getInstance().getCook(uuid);

            if (cookTimeBossBar == null) {
                return "{}";
            }

            if (cookTimeBossBar != null) {
                Date startDate = cookTimeBossBar.getDate();
                cookData.put("start_date", startDate != null ? startDate.getTime() : null);
                cookData.put("time", cookTimeBossBar.getTime());
                cookData.put("makeTime", cookTimeBossBar.getMakeTime());
                Integer maxAmount = cookTimeBossBar.getMaxAmount();
                cookData.put("maxCount", maxAmount != null ? maxAmount : null);
                cookData.put("recipeName", cookTimeBossBar.getRecipe().getRecipeName());
                cookData.put("nowMakeAmount", cookTimeBossBar.getNowMakeItemAmount());
                cookData.put("amount", cookTimeBossBar.getAmount());
                cookData.put("last_sync", System.currentTimeMillis());
            }

            return cookData.toJSONString();
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting cook information for " + uuid + ": " + e.getMessage());
            return "{}";
        }
    }

    public String getPlayerInventoryInformation(UUID uuid) {
        try {
            JSONObject invData = new JSONObject();
            PlayerPersistent playerPersistent = PlayerCauldronManager.getInstance().getPlayerCauldron(uuid);

            invData.put("cauldron", ObjectConverter.getObjectAsString(playerPersistent.getItemStacks()));
            invData.put("virtual_inventory", ObjectConverter.getObjectAsString(playerPersistent.getVirtualInventory()));

            if (playerPersistent.getCookEquipment() != null) {
                invData.put("cook_equipment", ObjectConverter.getObjectAsString(playerPersistent.getCookEquipment()));
            }

            invData.put("last_sync", System.currentTimeMillis());
            return invData.toJSONString();
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting inventory information for " + uuid + ": " + e.getMessage());
            return "{}";
        }
    }

    private boolean applyPlayerData(UUID uuid, String cookInfo, String invInfo) {
        try {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (invInfo != null && !invInfo.equals("{}")) {
                        JSONObject invData = (JSONObject) new JSONParser().parse(invInfo);
                        PlayerPersistent persistent = PlayerCauldronManager.getInstance().getPlayerCauldron(uuid);

                        if (invData.containsKey("cauldron")) {
                            persistent.setItemStacks((ItemStack[]) ObjectConverter.getObject((String) invData.get("cauldron")));
                        }
                        if (invData.containsKey("virtual_inventory")) {
                            persistent.setVirtualInventory((ItemStack[]) ObjectConverter.getObject((String) invData.get("virtual_inventory")));
                        }
                        if (invData.containsKey("cook_equipment")) {
                            persistent.setCookEquipment((ItemStack) ObjectConverter.getObject((String) invData.get("cook_equipment")));
                        } else {
                            persistent.setCookEquipment(null);
                        }
                    }

                    if (cookInfo != null && !cookInfo.equals("{}")) {
                        JSONObject cookData = (JSONObject) new JSONParser().parse(cookInfo);
                        if (cookData.containsKey("recipeName")) {
                            applyCookingStatus(uuid, cookData);
                        }
                    }

                    CookLoadingEvent cookLoadingEvent = new CookLoadingEvent(uuid, cookInfo, invInfo);
                    Bukkit.getPluginManager().callEvent(cookLoadingEvent);
                    future.complete(true);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply player data: " + e.getMessage());
                    future.complete(false);
                }
            });
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply player data: " + e.getMessage());
            return false;
        }
    }

    private void applyCookingStatus(UUID uuid, JSONObject cookData) {
        try {
            CookTimeBossBar currentCook = CookManager.getInstance().getCook(uuid);
            if (currentCook != null) {
                currentCook.interrupt();
            } // Server Offline 상태에서 요리 제작 시 아이템 들어갈 수 있음 확인.

            String recipeName = (String) cookData.get("recipeName");
            Recipe recipe = RecipeServiceManager.getInstance().getRecipeContainer().getRecipe(recipeName);

            if (recipe != null) {
                int makeTime = ((Long) cookData.get("makeTime")).intValue();
                int time = ((Long) cookData.get("time")).intValue();
                int amount = ((Long) cookData.get("amount")).intValue();
                int nowMakeAmount = ((Long) cookData.get("nowMakeAmount")).intValue();
                Long maxCountLong = (Long) cookData.get("maxCount"); // 서버 적용 시 이전 사람들 적용 불가 현상 해결
                int maxCount = (maxCountLong != null) ? maxCountLong.intValue() : -1;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        CookTimeBossBar newCook = new CookTimeBossBar(
                                uuid,
                                recipe,
                                RecipeServiceManager.getInstance().getBossBarTitle(),
                                amount,
                                nowMakeAmount,
                                time,
                                makeTime,
                                maxCount
                        );
                        newCook.start();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to start CookTimeBossBar: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply cooking status for " + uuid + ": " + e.getMessage());
        }
    }

    private boolean isRecipeServer(String serverName) {
        return serverName.equals("lobby") ||
                serverName.equals("island") ||
                serverName.equals("rpg") ||
                serverName.equals("collect");
    }

    public CompletableFuture<Boolean> handleServerTransfer(UUID uuid, String targetServer) {
        return CompletableFuture.supplyAsync(() -> {
            if (!acquireDataLock(uuid, targetServer)) return false;

            CompletableFuture<String> cookDataFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                CookTimeBossBar currentCook = CookManager.getInstance().getCook(uuid);
                if (currentCook != null) {
                    cookDataFuture.complete(getPlayerCookInformation(uuid));
                } else {
                    cookDataFuture.complete("{}");
                }
            });

            String capturedCookInfo;
            try {
                capturedCookInfo = cookDataFuture.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to capture cook info before transfer: " + e.getMessage());
                capturedCookInfo = "{}";
            }

            savePlayerDataWithCapturedCookInfo(uuid, targetServer, capturedCookInfo, getPlayerInventoryInformation(uuid));

            updateServerHistory(uuid, getCurrentServer(), targetServer);
            DataVersion newVersion = new DataVersion(uuid, targetServer);
            dataVersions.put(uuid, newVersion);

            Bukkit.getScheduler().runTask(plugin, () -> {
                CookTimeBossBar currentCook = CookManager.getInstance().getCook(uuid);
                if (currentCook != null) {
                    currentCook.interrupt();
                }
            });

            releaseDataLock(uuid);
            return true;
        }, asyncExecutor);
    }

    public void savePlayerDataWithCapturedCookInfo(UUID uuid, String serverName, String capturedCookInfo, String invInfo) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                long updateTime = System.currentTimeMillis();
                PlayerTransferData data = transferData.get(uuid);
                boolean isTransferring = data != null && data.transferring;

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO recipe_data (uuid, server_name, cook_info, inventory_info, last_update, transfer_status) " +
                                "VALUES (?, ?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "server_name = ?, cook_info = ?, inventory_info = ?, last_update = ?, transfer_status = ?")) {

                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, serverName);
                    stmt.setString(3, capturedCookInfo);
                    stmt.setString(4, invInfo);
                    stmt.setLong(5, updateTime);
                    stmt.setBoolean(6, isTransferring);

                    stmt.setString(7, serverName);
                    stmt.setString(8, capturedCookInfo);
                    stmt.setString(9, invInfo);
                    stmt.setLong(10, updateTime);
                    stmt.setBoolean(11, isTransferring);

                    stmt.executeUpdate();
                    conn.commit();

                    if (data != null) {
                        data.saved = true;
                        if (!isTransferring) {
                            data.future.complete(true);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save player data with captured cook info: " + e.getMessage());
            }
        });
    }


    public boolean isPlayerTransferring(UUID uuid) {
        PlayerTransferData data = transferData.get(uuid);
        return data != null && data.transferring;
    }

    private void loadPlayerDataSync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean result = loadDataFromDB(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result) {
                        applyPlayerData(uuid,
                                getPlayerCookInformation(uuid),
                                getPlayerInventoryInformation(uuid)
                        );
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Sync load failed: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> handleNetworkQuit(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!acquireDataLock(uuid, "QUIT")) {
                throw new IllegalStateException("Unable to acquire data lock for player quit");
            }

            try {
                if (isPlayerTransferring(uuid)) {
                    handleTransferCancellation(uuid);
                }

                forceSaveOnQuit(uuid);
                handlePlayerQuit(uuid);

                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to handle network quit: " + e);
                recoverFromFailure(uuid);
                throw e;
            } finally {
                releaseDataLock(uuid);
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> handleNetworkJoin(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!acquireDataLock(uuid, getCurrentServer())) {
                throw new IllegalStateException("Failed to acquire lock for UUID: " + uuid);
            }
            try {
                boolean loaded = loadPlayerData(uuid)
                        .thenApplyAsync(result -> {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                CookLoadingEvent event = new CookLoadingEvent(
                                        uuid,
                                        getPlayerCookInformation(uuid),
                                        getPlayerInventoryInformation(uuid)
                                );
                                Bukkit.getPluginManager().callEvent(event);
                            });
                            return result;
                        }).get(15, TimeUnit.SECONDS);

                setPlayerTransferring(uuid, false);
                return loaded;
            } catch (TimeoutException e) {
                handleTimeout(uuid);
                throw new CompletionException("Data load timeout", e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                releaseDataLock(uuid);
            }
        }, asyncExecutor).exceptionally(ex -> {
            plugin.getLogger().severe("Network join failed: " + ex.getMessage());
            return false;
        });
    }

    private void waitForPreviousTransfer(UUID uuid) throws InterruptedException, TimeoutException, ExecutionException {
        PlayerTransferData data = transferData.get(uuid);
        if (data != null && data.transferring) {
            boolean completed = data.future.get(5, TimeUnit.SECONDS);
            if (!completed) {
                throw new TimeoutException("Timeout waiting for previous transfer");
            }
        }
    }

    private void handleTransferCancellation(UUID uuid) {
        try {
            PlayerTransferData data = transferData.get(uuid);
            if (data != null) {
                data.transferring = false;
                data.future.complete(false);
            }

            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE recipe_data SET transfer_status = false WHERE uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                    conn.commit();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to cancel transfer: " + e.getMessage());
        }
    }

    private void handleRetry(UUID uuid, Runnable action) {
        int currentRetry = retryCount.getOrDefault(uuid, 0);
        if (currentRetry < MAX_RETRY_COUNT) {
            retryCount.put(uuid, currentRetry + 1);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                try {
                    action.run();
                } catch (Exception e) {
                    plugin.getLogger().severe("Retry failed for " + uuid + ": " + e.getMessage());
                }
            }, 20L * (currentRetry + 1));
        } else {
            plugin.getLogger().severe("Max retry count reached for " + uuid);
            recoverFromFailure(uuid);
        }
    }

    private String getCurrentServer() {
        return plugin.getConfig().getString("server-name", "unknown");
    }

    public CompletableFuture<Boolean> verifyTransfer(UUID uuid, String targetServer) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT transfer_status, server_name FROM recipe_data WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    boolean transferStatus = rs.getBoolean("transfer_status");
                    String currentServer = rs.getString("server_name");

                    return !transferStatus && currentServer.equals(plugin.getConfig().getString("server-name"));
                }
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Transfer verification failed: " + e.getMessage());
                return false;
            }
        });
    }

    private void savePlayerDataWithInfo(UUID uuid, String serverName, String cookInfo, String invInfo) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            //Bukkit.getLogger().info(cookInfo);
            long updateTime = System.currentTimeMillis();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO recipe_data (uuid, server_name, cook_info, inventory_info, last_update, transfer_status) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "server_name = ?, cook_info = ?, inventory_info = ?, last_update = ?, transfer_status = ?")) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, serverName);
                stmt.setString(3, cookInfo);
                stmt.setString(4, invInfo);
                stmt.setLong(5, updateTime);
                stmt.setBoolean(6, true);

                stmt.setString(7, serverName);
                stmt.setString(8, cookInfo);
                stmt.setString(9, invInfo);
                stmt.setLong(10, updateTime);
                boolean isRecipeServer = serverName.equals("lobby") || serverName.equals("island") || serverName.equals("rpg") || serverName.equals("collect");
                stmt.setBoolean(11, isRecipeServer);

                stmt.executeUpdate();
                conn.commit();

                CookSavedEvent cookSavedEvent = new CookSavedEvent(uuid, cookInfo, invInfo);
                Bukkit.getPluginManager().callEvent(cookSavedEvent);

                PlayerTransferData data = transferData.get(uuid);
                if (data != null) {
                    data.saved = true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save transfer data: " + e.getMessage());
        }
    }

    public void forceDataSyncOnJoin(UUID uuid) {
        transferData.remove(uuid);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE recipe_data SET transfer_status = false WHERE uuid = ?"
             )) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clear transfer status for " + uuid + ": " + e.getMessage());
        }

        String currentServer = plugin.getConfig().getString("server-name", "lobby");
        savePlayerDataSync(uuid, currentServer);
    }

    public void forceSaveOnQuit(UUID uuid) {
        String serverName = plugin.getConfig().getString("server-name", "lobby");
        transferData.remove(uuid);
        savePlayerDataSync(uuid, serverName);
    }


    private void networkJoin(String uuid) {
        UUID playerUUID = UUID.fromString(uuid);
        loadPlayerData(playerUUID)
                .thenRun(() -> {
                    setPlayerTransferring(playerUUID, false);
                    forceDataSyncOnJoin(playerUUID);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("데이터 로드 실패: " + ex.getMessage());
                    return null;
                });
    }

    private int getNowCount(String uuid) {
        UUID playerUUID = UUID.fromString(uuid);
        return CookManager.getInstance().getCook(playerUUID).getNowMakeItemAmount();
    }

    private int getMaxCount(String uuid) {
        UUID playerUUID = UUID.fromString(uuid);
        return CookManager.getInstance().getCook(playerUUID).getMaxAmount();
    }

    private void savePlayerDataSync(UUID uuid, String serverName) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            String cookInfo = getPlayerCookInformation(uuid);
            String invInfo = getPlayerInventoryInformation(uuid);
            long updateTime = System.currentTimeMillis();

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO recipe_data (uuid, server_name, cook_info, inventory_info, last_update, transfer_status) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "server_name = ?, cook_info = ?, inventory_info = ?, last_update = ?, transfer_status = ?")) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, serverName);
                stmt.setString(3, cookInfo);
                stmt.setString(4, invInfo);
                stmt.setLong(5, updateTime);
                stmt.setBoolean(6, true);

                stmt.setString(7, serverName);
                stmt.setString(8, cookInfo);
                stmt.setString(9, invInfo);
                stmt.setLong(10, updateTime);
                stmt.setBoolean(11, true);

                stmt.executeUpdate();
                conn.commit();

                PlayerTransferData data = transferData.get(uuid);
                if (data != null) {
                    data.saved = true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save transfer data: " + e.getMessage());
        }
    }

    public void close() {
        asyncExecutor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void startAutoSave() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Set<UUID> onlinePlayers = new HashSet<>();

            for (UUID uuid : PlayerCauldronManager.getInstance().getActivePlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    onlinePlayers.add(uuid);
                }
            }

            Set<UUID> playersToSave = onlinePlayers.stream()
                    .filter(uuid -> {
                        PlayerTransferData data = transferData.get(uuid);
                        return data == null || !data.transferring;
                    })
                    .collect(Collectors.toSet());

            if (!playersToSave.isEmpty()) {
                bulkSavePlayers(playersToSave);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(String.format(
                            "Auto-saved data for %d players", playersToSave.size()
                    ));
                }
            }
        }, 6000L, 6000L);
    }

    private static List<String> convertItemStacksToString(ItemStack[] items) {
        List<String> result = new ArrayList<>();

        if (items == null || items.length == 0) {
            return result;
        }

        for (ItemStack item : items) {
            if (item != null) {
                String itemInfo = String.format("%s: %d개",
                        item.getType().toString(),
                        item.getAmount()
                );
                result.add(itemInfo);
            }
        }

        return result;
    }

    private static String convertItemStacksToSingleString(ItemStack[] items) {
        List<String> itemList = convertItemStacksToString(items);
        return String.join(", ", itemList);
    }
}