package gaya.pe.kr.core.manager;

import gaya.pe.kr.core.RecipePlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigurationManager {
    Plugin plugin = RecipePlugin.getPlugin();
    File pluginDataFolder = RecipePlugin.getPlugin().getDataFolder();

    public static synchronized ConfigurationManager getInstance() {
        return SingleTon.CONFIGURATION_MANAGER;
    }

    public FileConfiguration getConfiguration(String relativePath, String resourcePath) {
        File file = new File(this.pluginDataFolder, relativePath);
        return file.exists() ? YamlConfiguration.loadConfiguration((File)file) : this.getDefaultConfiguration(file, resourcePath);
    }

    public void saveConfiguration(FileConfiguration configuration, String relativePath) {
        try {
            configuration.save(new File(this.pluginDataFolder, relativePath));
            //RecipePlugin.log(String.format("&f[&6%s&f]의 파일이 &e성공적&f으로 저장 되었습니다", relativePath));
        } catch (IOException e) {
            e.printStackTrace();
            RecipePlugin.log(String.format("&f[&6%s&f]의 파일의 저장을 &4실패 했습니다", relativePath));
        }
    }

    public void setUpFolder(String relativePath) {
        File file = new File(this.pluginDataFolder, relativePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private FileConfiguration getDefaultConfiguration(File path, String resourcePath) {
        try {
            InputStreamReader reader = new InputStreamReader(this.plugin.getResource(resourcePath), StandardCharsets.UTF_8);
            YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration((Reader)reader);
            fileConfiguration.save(path);
            RecipePlugin.log(String.format("&f[ &b%s &f]의&f 설정파일이 정상 &e생성&f 되었습니다", path.getName()));
            return fileConfiguration;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<String> getConfigurationSection(FileConfiguration configuration, String path) throws NullPointerException {
        try {
            return configuration.getConfigurationSection(path).getKeys(false);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private static class SingleTon {
        private static final ConfigurationManager CONFIGURATION_MANAGER = new ConfigurationManager();

        private SingleTon() {
        }
    }
}

