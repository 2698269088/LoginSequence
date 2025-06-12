package top.mcocet.loginSequence;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class FillTask {
    // 配置文件路径常量
    private static final String CONFIG_PATH = "plugins/LoginSequence/Config.yml";

    public static void initConfig(JavaPlugin plugin) {
        File configFile = new File(CONFIG_PATH);

        // 创建配置文件目录结构
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
                plugin.getLogger().info("已创建默认配置文件");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "配置文件创建失败", e);
                return;
            }
        }

        // 初始化默认配置
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        setDefault(yaml, "Port", 1234);
        setDefault(yaml, "ip", "127.0.0.1");
        setDefault(yaml, "pionli", false);
        setDefault(yaml, "server", "lobby");
        setDefault(yaml, "ontime", 30);
        setDefault(yaml, "nottime", 15);

        try {
            yaml.save(configFile);
            plugin.getLogger().info("配置文件初始化完成");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "配置文件保存失败", e);
        }
    }

    // 辅助方法设置默认值
    private static void setDefault(YamlConfiguration yaml, String path, Object value) {
        if (!yaml.contains(path)) {
            yaml.set(path, value);
        }
    }
}
