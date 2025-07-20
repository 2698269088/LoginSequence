package top.mcocet.loginSequenceOnline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class LoginSequenceOnline extends JavaPlugin {
    private final Map<String, ScheduledFuture<?>> pendingPlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private UDPNet udpNet;
    private int port;
    private  int serverVersion;
    private boolean logEnabled = true;

    @Override
    public void onEnable() {
        // 初始化日志和配置
        printLogo();
        createConfig();
        logEnabled = getConfig().getBoolean("log", true);

        // 初始化网络模块
        port = getConfig().getInt("Port", 1234);
        udpNet = new UDPNet(this, port);
        udpNet.startListening(this::handleIncomingPacket);

        // 注册玩家加入事件
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        getLogger().info("LoginSequenceOnline已启用！");
        getLogger().info("LoginSequenceOnline已支持Folia！");

        if (udpNet.isFolia()){
            getLogger().info("检测到服务器为Folia类！");
            // 获取服务器版本字符串（示例格式：1.21.4-R0.1-SNAPSHOT）
            String version = Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = version.split("\\.");
            // 转换为数字版本号
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            // 检查是否低于1.21.4
            // 此API在Folia 1.21.4中添加
            if (major < 1 || (major == 1 && (minor < 21 || (minor == 21 && patch < 4)))) {
                serverVersion = 0;
                noConSayLog(ChatColor.RED + "Folia版本过低，将无法获取服务器TPS信息！");
                noConSayLog(ChatColor.YELLOW + "请升级至Folia 1.21.4+ 或使用Bukkit类服务器！");
            } else {
                serverVersion = 1;
            }
        } else {
            getLogger().info("检测到服务器为Bukkit类！");
        }
    }

    private class PlayerJoinListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            String playerName = player.getName();

            // 检查是否为OP权限
            if (player.isOp()) {
                return;
            }

            // 检查是否在缓存名单中
            if (!pendingPlayers.containsKey(playerName)) {
                // 非法加入处理
                player.kickPlayer("非法加入服务器");
                noConSayLog(ChatColor.RED + "[LoginSequence] 非法加入尝试: " + playerName
                        + " (" + player.getAddress().getHostString() + ")");
            } else {
                // 成功加入后移除缓存
                pendingPlayers.remove(playerName).cancel(false);
            }
        }
    }


    // 处理传入的包
    private void handleIncomingPacket(String message, InetAddress address, int port) {
        if (message.contains("LoginSequence-Hello")) {
            processHelloPacket(address, port);
        } else if (message.contains("LoginSequence-Info")) {
            processInfoPacket(address, port);
        } else if (message.startsWith("LoginSequence-Player ")) {
            processPlayerPacket(message, address, port);
        }
    }

    private void processHelloPacket(InetAddress address, int port) {
        sayLog("收到 “LoginSequence-Hello” 消息，来自：" + address.getHostAddress() + ":" + port);
        udpNet.sendResponse("LoginSequence-Online", address, port);
        sayLog(ChatColor.GREEN + "已发送 “LoginSequence-Online” 响应");
    }

    private void processInfoPacket(InetAddress address, int port) {
        sayLog("收到 “LoginSequence-Info” 请求，来自：" + address.getHostAddress() + ":" + port);
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024; // 获取当前内存使用量
        int onlinePlayers = getServer().getOnlinePlayers().size(); // 获取当前在线玩家数量
        int maxPlayers = getServer().getMaxPlayers(); // 获取最大玩家数量
        double tps = getCurrentTPS(); // 获取当前TPS
        // 修改响应格式，添加 MaxQuantity 字段
        String response = String.format("LoginSequence-Data Memory:%dMB Online:%d TPS:%.1f MaxQuantity:%d", memUsed, onlinePlayers, tps, maxPlayers);
        udpNet.sendResponse(response, address, port);
        sayLog(ChatColor.GREEN + "已发送 “LoginSequence-Data” 响应");
        sayLog(ChatColor.AQUA + response);
    }

    private void processPlayerPacket(String message, InetAddress address, int port) {
        // 解析玩家名
        String playerName = message.replace("LoginSequence-Player ", "").trim();
        if (playerName.isEmpty()) return;
        // 记录到缓存并设置1分钟过期
        pendingPlayers.put(playerName, scheduler.schedule(() -> {
            pendingPlayers.remove(playerName);
        }, 60, TimeUnit.SECONDS));
        sayLog(ChatColor.GREEN + "[LoginSequence] 接收到来自 " + address.getHostAddress() + ":" + port
                + " 的玩家认证请求: " + playerName);
    }


    private double getCurrentTPS() {
        try {
            if (udpNet.isFolia()) {
                return (Bukkit.getTPS()[0]);
            } else {
                return getStandardTPS();
            }
        } catch (Exception e) {
            noConSayLog(ChatColor.RED + "获取TPS失败: " + e.getMessage());
            return (-1.0);
        }
    }

    private double getStandardTPS() throws Exception {
        Object minecraftServer = getServer().getClass().getMethod("getServer").invoke(getServer());
        double[] recentTps = (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
        return recentTps[0];
    }

    private void printLogo() {
        // logo
        noConSayLog(ChatColor.AQUA+"    "+" "+ChatColor.BLUE+" __ "+" "+ChatColor.GOLD+"  __ ");
        noConSayLog(ChatColor.AQUA+"|   "+" "+ChatColor.BLUE+"(__ "+" "+ChatColor.GOLD+" /  \\");
        noConSayLog(ChatColor.AQUA+"|___"+" "+ChatColor.BLUE+" __)"+" "+ChatColor.GOLD+" \\__/ "+ChatColor.GREEN+"    LoginSequenceOnline v1.4");
        noConSayLog("");
    }
    private void createConfig() {
        File configFile = new File(getDataFolder(), "Config.yml");
        if (!configFile.exists()) {
            getLogger().info("配置文件不存在，正在创建...");
            try {
                if (!getDataFolder().exists()) {
                    getDataFolder().mkdirs();
                }
                configFile.createNewFile();
                getLogger().info("配置文件创建成功");

                // 写入默认配置项
                getConfig().set("Port", 1234);
                getConfig().set("log", true);
                saveConfig();
            } catch (IOException e) {
                getLogger().severe("创建配置文件时出错");
                e.printStackTrace();
            }
        }
    }

    private void sayLog(String s) {
        if (logEnabled) {
            CommandSender sender = Bukkit.getConsoleSender();
            sender.sendMessage(s);
        }
    }

    private void noConSayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    @Override
    public void onDisable() {
        getLogger().info("插件已禁用");
        if (udpNet != null) {
            udpNet.closeSocket();
        }
    }
}
