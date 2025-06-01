package top.mcocet.loginSequence.tasks;

// 特别说明：
// 因为之前原作者手残
// 导致插件源代码全部丢失
// 现在你看到的是反编译后得到的版本（
// 如果你觉得代码写得很抽象
// 我也没办法（
// 这是反编译后又经过AI优化并增加注释的版本（（（
// 好在代码没有经过混淆等处理
// 将就着看吧（（（（
// :(

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.mcocet.loginSequence.LoginSequence;

public class PingOnline {
    private int port; // 端口号
    private String ip; // IP地址
    private boolean pionli; // 是否启用在线检测
    private String server; // 服务器名称
    private static boolean serverOnlineInfo; // 服务器在线状态
    private boolean outLog = true; // 是否输出日志
    private final LoginSequence plugin; // 主插件实例
    private DatagramSocket socket; // UDP套接字
    private boolean testingServer = true; // 是否正在测试服务器
    private BukkitRunnable connectivityTask; // 连接性测试任务
    private BukkitRunnable currentTimeoutTask;// 超时检测任务引用
    private boolean wasOffline = false; // 新增状态跟踪字段

    public PingOnline(LoginSequence plugin) {
        this.plugin = plugin;
        initialize();
    }

    public static String getServerName() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
            new File("plugins"+File.separator+"LoginSequence"+File.separator+"Config.yml")); // 优化路径分隔符
        String getServerNames;
        try {
            // 获取服务器名称（不知道为什么，返回的总是null）
            // 现在好了
            getServerNames = Objects.requireNonNull(yaml.getString("server")).strip();
            sayLogStatic(ChatColor.GREEN+"成功获取到了子服务器名称： "+getServerNames);
        } catch (NullPointerException var3) {
            // 无法读取数据，使用默认值
            getServerNames = "lobby";
            sayLogStatic(ChatColor.YELLOW+"[!] 配置文件读取错误，已设置为默认值：lobby");
            sayLogStatic(ChatColor.BLUE+"这个报错是正常现象，不用大惊小怪，因为原作者也没想到解决办法（ 【划掉】");
            sayLogStatic(ChatColor.BLUE+"这个问题已经修复了，如果还是不行，请检查配置文件");
        }
        return getServerNames;
    }

    private void initialize() {
        File configFile = new File(this.plugin.getDataFolder(), "Config.yml");
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            try {
                configFile.createNewFile();
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.set("Port", 1234);
                yaml.set("ip", "127.0.0.1");
                yaml.set("pionli", false);
                yaml.set("server", "lobby");
                yaml.set("ontime", 30);
                yaml.set("nottime", 15);
                yaml.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "配置文件保存失败", e);
            }
        }

        YamlConfiguration yaml = new YamlConfiguration();

        try {
            yaml.load(configFile);
            this.port = yaml.getInt("Port"); // 添加this明确成员变量
            this.ip = yaml.getString("ip");
            this.pionli = yaml.getBoolean("pionli");
            this.server = yaml.getString("server");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "配置文件加载异常", e); // 增强异常处理
        }

        plugin.getLogger().info("配置文件读取完成！");
        plugin.getLogger().info("IP: " + ip);
        plugin.getLogger().info("端口： " + port);
        plugin.getLogger().info("连接性检测： " + pionli);
        plugin.getLogger().info("子服务器：" + server);
        if (!pionli) {
            sayLog(ChatColor.YELLOW + "[!] 服务器在线监测已关闭，将不会对服务器是否在线进行测试!");
            testingServer = false;
        }

        (new Thread(() -> {
            try {
                socket = new DatagramSocket();
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                plugin.getLogger().info("Listening on port " + port);

                while(!socket.isClosed()) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith("LoginSequence-Online")) {
                        // 收到响应时取消超时检测
                        if (currentTimeoutTask != null) {
                            currentTimeoutTask.cancel();
                        }

                        // 修改日志输出逻辑
                        if (wasOffline) {
                            sayLog(ChatColor.GREEN + "与服务器的连接恢复");
                            wasOffline = false; // 重置状态
                        } else {
                            sayLog(ChatColor.GREEN + "与服务器的连接正常");
                        }

                        plugin.setIsTransferring(false);
                        plugin.getCheckingTask().processQueue();
                        serverOnlineInfo = true;
                    } else {
                        serverOnlineInfo = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }

            }

        })).start();
    }

    // 新增内部类处理服务器连通性测试任务
    private class ServerConnectivityTask extends BukkitRunnable {
        @Override
        public void run() {
            if (testingServer && !serverOnlineInfo) {
                // 发送请求后启动超时检测
                if (sendHelloPacket(ip, port)) {
                    startTimeoutDetection(); // 启动超时检测
                }
                cancel();
            }
        }
    }

    // 新增超时检测方法
    private void startTimeoutDetection() {
        // 取消之前的检测任务
        if (currentTimeoutTask != null) {
            currentTimeoutTask.cancel();
        }

        currentTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!serverOnlineInfo && outLog) {  // 增加日志输出判断
                    plugin.getLogger().warning("请求超时");
                    sayLog(ChatColor.RED + "服务器连接超时，可能已经离线");
                    wasOffline = true;
                    plugin.getCheckingTask().notifyQueuePositions();
                }
            }
        };
        // 5秒后执行（20 ticks/秒 * 5 = 100 ticks）
        currentTimeoutTask.runTaskLater(plugin, 100L);
    }

    // 新增通用方法处理Hello包发送
    private boolean sendHelloPacket(String targetIp, int targetPort) {
        try {
            InetAddress address = InetAddress.getByName(targetIp);
            byte[] message = "LoginSequence-Hello".getBytes();
            DatagramPacket packet = new DatagramPacket(message, message.length, address, targetPort);
            socket.send(packet);
            if (outLog) {  // 增加日志输出判断
                plugin.getLogger().info("Sent 'LoginSequence-Hello' to " + targetIp + ":" + targetPort);
            }
            return true;
        } catch (Exception e) {
            if (outLog) {  // 增加日志输出判断
                plugin.getLogger().warning("连接性测试失败：" + e.getMessage());
                e.printStackTrace();
            }
            serverOnlineInfo = false;
            return false;
        }
    }

    public void startConnectivityTest() {
        connectivityTask = new ServerConnectivityTask();
        connectivityTask.runTaskLater(plugin, 0L); // 只执行一次
    }

    // 修改手动测试方法
    public void manualPing(CommandSender sender) {
        plugin.getLogger().info("手动触发连接性测试...");
        if (!testingServer) {
            sendServerStatusMessage(sender, ChatColor.YELLOW + "服务器在线检测功能已关闭");
            return;
        }

        boolean success = sendHelloPacket(ip, port);
        String message = success ?
            ChatColor.GREEN + "已发送检测请求，等待服务器响应..." : // 修改提示信息
            ChatColor.RED + "连接性测试失败";
        sendServerStatusMessage(sender, message);
        if (success) {
            startTimeoutDetection(); // 启动超时检测
        }
    }

    // 新增统一消息处理方法
    private void sendServerStatusMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(message);
        } else {
            sender.sendMessage(message);
        }
    }

    public boolean isConnectivityTestEnabled() {
        return pionli;
    }

    public void performConnectivityTestForPlayer(final Player player) {
        plugin.getLogger().info("开始为玩家 " + player.getName() + " 进行连接性测试...");
        boolean success = sendHelloPacket(ip, port); // 复用发送方法

        // 重构超时检测逻辑
        new ConnectivityTimeoutCheck(player, success).runTaskTimer(plugin, 0L, 20L);
    }

    // 新增超时检测内部类
    private class ConnectivityTimeoutCheck extends BukkitRunnable {
        private final Player player;
        private final long startTime;
        private final boolean initialSuccess;

        ConnectivityTimeoutCheck(Player player, boolean initialSuccess) {
            this.player = player;
            this.startTime = System.currentTimeMillis();
            this.initialSuccess = initialSuccess;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime >= 5000L) {
                handleTimeoutResult();
                cancel();
            }
        }

        private void handleTimeoutResult() {
            if (plugin.getIsTransferring()) {
                serverOnlineInfo = true;
                player.sendMessage(ChatColor.GREEN + "服务器连接成功，正在为您处理...");
            } else {
                player.sendMessage(ChatColor.RED + "无法连接服务器，请等待服务器重新上线");
                serverOnlineInfo = false;
            }
        }
    }

    // 新增静默检测方法
    public boolean silentPingCheck() {
        boolean originalLogState = outLog;  // 保存原始日志状态
        outLog = false;  // 临时关闭日志输出
        boolean result = sendHelloPacket(ip, port);
        outLog = originalLogState;  // 恢复日志设置
        return result;
    }

    private void sayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    private static void sayLogStatic(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }
}
