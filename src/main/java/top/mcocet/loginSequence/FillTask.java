package top.mcocet.loginSequence;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.tasks.PingOnline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class FillTask {
    // 配置文件路径常量
    private static final String CONFIG_PATH = "plugins/LoginSequence/Config.yml";
    private static final String SL_FILE_PATH = "plugins/LoginSequence/sl.txt";
    private static final String JSON_PATH = "plugins/LoginSequence/UserData.json";

    // 配置文件信息
    public static int port; // 端口号
    public static String ip; // IP地址
    public static boolean pionli; // 是否启用在线检测
    public static String server; // 服务器名称
    public static boolean sl = false; // 是否启用安全登录
    public static boolean pwl = false; // 是否启用密码登录
    public static boolean piEula = false; // 玩家数据存储开关，即pi_eula协议

    public static void initConfig(JavaPlugin plugin) {
        // 获取配置文件以及其他文件
        File configFile = new File(CONFIG_PATH);
        File slFile = new File(SL_FILE_PATH);

        // 检查并创建配置文件
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try (InputStream inputStream = plugin.getResource("Config.yml");
                 OutputStream outputStream = new FileOutputStream(configFile)) {
                if (inputStream == null) {
                    throw new IOException("无法找到资源文件 Config.yml");
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                plugin.getLogger().info("已创建默认配置文件");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "配置文件创建失败", e);
                return;
            }
        } else {
            plugin.getLogger().info("配置文件已存在，跳过初始化");
        }

        // 检查并创建安全登录文件
        if (!slFile.exists()) {
            try {
                slFile.getParentFile().mkdirs();
                // 写入内容
                String content = "§bLoginSequence\n§f我们正在分析你的连接\n§a您现在可以重新加入";
                Files.write(slFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
                plugin.getLogger().info("已创建安全登录文件");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "创建安全登录文件失败", e);
            }
        } else {
            plugin.getLogger().info("安全登录文件已存在，跳过初始化");
        }

        // 初始化JSON文件
        try {
            PlayerDataManager.load();
            plugin.getLogger().info("玩家数据文件已加载");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "玩家数据文件初始化失败", e);
        }
    }

    // 读取配置文件
    public static void readConfig(JavaPlugin plugin) {
        YamlConfiguration yaml = new YamlConfiguration();

        try {
            File configFile = new File(CONFIG_PATH);
            yaml.load(configFile);
            port = yaml.getInt("Port");
            ip = yaml.getString("ip");
            pionli = yaml.getBoolean("pionli");
            server = yaml.getString("server");
            sl = yaml.getBoolean("sl");
            pwl = yaml.getBoolean("pwl");
            piEula = yaml.getBoolean("pi_eula");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "配置文件加载异常", e); // 增强异常处理
        }
        plugin.getLogger().info("配置文件读取完成！");
        plugin.getLogger().info("远程服务器: " + ip + ":" + port);
        plugin.getLogger().info("子服务器：" + server);
        if (pionli) {
            plugin.getLogger().info("已启用在线检测");
        } else {
            sayLog(ChatColor.YELLOW + "[!] 服务器在线监测已关闭，将不会对服务器是否在线进行测试!");
        }
        if(piEula){
            sayLog(ChatColor.YELLOW + "[!] 已启用玩家数据记录功能，如果您不同意pi_EULA协议，请在配置文件中关闭该选项！");
        } else {
            plugin.getLogger().info("未启用玩家数据记录功能");
        }
    }

    // 辅助方法设置默认值
    private static void setDefault(YamlConfiguration yaml, String path, Object value) {
        if (!yaml.contains(path)) {
            yaml.set(path, value);
        }
    }

    private static void sayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }
}
