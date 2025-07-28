package top.mcocet.loginSequence.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import top.mcocet.loginSequence.LoginSequence;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SecureLogin {
    private final LoginSequence plugin;
    private final HashMap<UUID, Long> playerJoinTimes = new HashMap<>();
    private final HashMap<UUID, BukkitTask> pendingTasks = new HashMap<>();
    private final Set<UUID> processingPlayers = new HashSet<>();
    private final Set<UUID> verifiedPlayers = new HashSet<>();

    // 添加文件路径常量
    private static final String CONFIG_PATH = "plugins/LoginSequence/Config.yml";
    private static final String SL_FILE_PATH = "plugins/LoginSequence/sl.txt";

    public static String sl_Text = null; // 安全登录提示文本
    boolean sl; // 是否启用安全登录
    private boolean initialized = false; // 是否已经初始化

    public SecureLogin(LoginSequence plugin) {
        this.plugin = plugin;
        initFill(plugin); // 确保在构造函数中初始化配置
        initialized = true; // 标记为已初始化

        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                SecureLogin.this.onPlayerJoin(event);
            }
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                UUID playerId = event.getPlayer().getUniqueId();
                // 玩家退出，清理安全登录记录
                if (playerJoinTimes.containsKey(playerId)) {
                    playerJoinTimes.remove(playerId);
                }
                // 退出时取消安全登录定时器
                if (pendingTasks.containsKey(playerId)) {
                    pendingTasks.get(playerId).cancel();
                    pendingTasks.remove(playerId);
                }
            }
        }, plugin);

    }


    // 检查sl配置并加载文件内容
    public void initFill(JavaPlugin plugin) {
        // 加载配置文件
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            plugin.getLogger().warning("配置文件不存在，跳过安全登录初始化");
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        this.sl = yaml.getBoolean("sl");
        // 检查sl配置是否为true
        if (sl) {
            loadSlText(plugin);
        }
    }

    // 读取sl.txt文件内容
    private static void loadSlText(JavaPlugin plugin) {
        File slFile = new File(SL_FILE_PATH);
        if (!slFile.exists()) {
            plugin.getLogger().warning("安全登录配置文件内容不存在，跳过读取");
            return;
        }

        try {
            // 读取文件内容到字符串
            byte[] fileBytes = Files.readAllBytes(slFile.toPath());
            sl_Text = new String(fileBytes, StandardCharsets.UTF_8);
            plugin.getLogger().info("已加载安全登录配置文件内容");
        } catch (Exception e) {
            plugin.getLogger().severe("读取安全登录配置文件内容失败: " + e.getMessage());
        }
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!sl) {
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 防止并发多次触发
        if (processingPlayers.contains(playerId)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 正在处理中，忽略重复事件");
            return;
        }

        processingPlayers.add(playerId);

        try {
            // 如果已经通过验证，直接放行
            if (verifiedPlayers.contains(playerId)) {
                return;
            }

            // 首次加入：踢出并设置定时器
            if (!playerJoinTimes.containsKey(playerId)) {
                event.setJoinMessage(null);
                kickSafely(player, sl_Text);
                playerJoinTimes.put(playerId, System.currentTimeMillis());
                startTimer(playerId);

            } else {
                long joinTime = playerJoinTimes.get(playerId);
                long elapsedTime = System.currentTimeMillis() - joinTime;

                if (elapsedTime < 10 * 60 * 1000) {
                    // 在有效期内重新加入，取消定时器并移除记录
                    if (pendingTasks.containsKey(playerId)) {
                        pendingTasks.get(playerId).cancel();
                        pendingTasks.remove(playerId);
                    }
                    playerJoinTimes.remove(playerId); // 清理记录
                    verifiedPlayers.add(playerId);   // 添加到已验证集合
                } else {
                    // 超过有效期，视为新一次加入
                    event.setJoinMessage(null);
                    kickSafely(player, sl_Text);
                    playerJoinTimes.put(playerId, System.currentTimeMillis());
                    startTimer(playerId);
                }
            }
        } finally {
            processingPlayers.remove(playerId); // 处理完成后释放锁
        }
    }

    private void startTimer(UUID playerId) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerJoinTimes.containsKey(playerId)) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        plugin.getLogger().info("定时器触发：踢出玩家 " + player.getName());
                        kickSafely(player, sl_Text);
                    }
                    playerJoinTimes.remove(playerId);
                    pendingTasks.remove(playerId);
                }
            }
        }.runTaskLater(plugin, 10 * 60 * 20L); // 10分钟
        pendingTasks.put(playerId, task);
    }

    private void kickSafely(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        try {
            player.kickPlayer(message);
        } catch (Exception e) {
            plugin.getLogger().severe("无法安全地踢出玩家: " + player.getName());
            e.printStackTrace();
        }
    }
/*
    public void initIfNeeded() {
        if (!initialized) {
            initFill(plugin);
            initialized = true;
        }
    }

 */
}
