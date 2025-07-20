package top.mcocet.loginSequenceBC;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class FillTask {

    public int port; // UDP监听端口

    private static final String CONFIG_FILE_NAME = "Config.yml";
    private final Plugin plugin;
    private final File dataFolder;
    private final File configFile;
    private final Map<String, Object> configData = new HashMap<>();
    private final Yaml yaml;

    public FillTask(Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.configFile = new File(dataFolder, CONFIG_FILE_NAME);

        // 设置 YAML 输出选项，确保输出标准格式
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);

        loadConfig();
    }

    public void loadConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        if (!configFile.exists()) {
            plugin.getLogger().warning("配置文件不存在，正在创建...");

            try {
                configFile.createNewFile();
                plugin.getLogger().info(ChatColor.GREEN + "配置文件创建成功");

                // 设置默认值
                configData.put("port", 1254);

                saveConfig();
            } catch (IOException e) {
                plugin.getLogger().severe(ChatColor.RED + "创建配置文件时出错");
                e.printStackTrace();
            }
        }
        readConfig();
    }

    private void readConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            Map<String, Object> data = yaml.load(reader);
            if (data != null) {
                configData.putAll(data);
                // 读取字段
                this.port = (int) data.getOrDefault("port", 1254);
                plugin.getLogger().info(ChatColor.GREEN + "配置文件读取完成");
            }
        } catch (IOException e) {
            plugin.getLogger().severe(ChatColor.RED + "读取配置文件时出错");
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            // 使用标准 YAML 格式保存
            writer.write(yaml.dump(configData));
        } catch (IOException e) {
            plugin.getLogger().severe(ChatColor.RED + "保存配置文件时出错");
            e.printStackTrace();
        }
    }

    public Map<String, Object> getConfig() {
        return configData;
    }
}
