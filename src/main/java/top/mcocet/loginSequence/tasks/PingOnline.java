package top.mcocet.loginSequence.tasks;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;

public class PingOnline {
    // 配置文件路径常量
    private static final String CONFIG_PATH = "plugins/LoginSequence/Config.yml";
    // 日志文件路径
    private static final String LOG_PATH = "plugins/LoginSequence/UDPData.log";

    public static boolean serverOnlineInfo; // 服务器在线状态
    private boolean outLog = true; // 是否输出日志
    private final LoginSequence plugin; // 主插件实例
    private DatagramSocket socket; // UDP套接字
    private static boolean testingServer = true; // 是否测试服务器
    private BukkitRunnable connectivityTask; // 连接性测试任务
    private BukkitRunnable currentTimeoutTask;// 连通性测试超时检测任务引用
    private BukkitRunnable infoTimeoutTask;// 远程服务器信息检测超时检测任务引用
    private boolean wasOffline = false; // 状态跟踪字段
    private boolean oneWasOffline = true; // 初次检测服务器

    // 远程服务器状信息
    private volatile int memUsage = -1; // 内存使用率
    private volatile int onlinePlayers = -1; //  在线玩家数
    private volatile double serverTPS = -1.0; // 远程服务器TPS
    private volatile int maxQuantity = -1; // 最大在线人数
    private double quantityPercentage = -1.0; // 在线人数百分比

    private int port = FillTask.port; // 服务器端口
    private String ip = FillTask.ip; // 服务器IP
    private static boolean pionli = FillTask.pionli; // 远程服务器是否开启

    public PingOnline(LoginSequence plugin) {
        this.plugin = plugin;
        if(!pionli){
            testingServer = false;
            return;
        }
        // UDP监听线程
        (new Thread(() -> {
            try {
                socket = new DatagramSocket();
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                plugin.getLogger().info("Listening on port " + port);

                while(!socket.isClosed()) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());

                    // 日志记录功能
                    try {
                        String logEntry = String.format("[%s] From %s:%d - %s\n", LocalDateTime.now(), packet.getAddress().getHostAddress(), packet.getPort(), message);
                        Files.write(Path.of(LOG_PATH), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.WARNING, "无法写入UDP日志文件: " + e.getMessage());
                    }

                    if (message.startsWith("LoginSequence-Online")) {
                        // 收到响应时取消超时检测
                        if (currentTimeoutTask != null) { // 添加空指针检查
                            currentTimeoutTask.cancel();
                            currentTimeoutTask = null; // 重置引用
                        }

                        // 日志输出
                        if (wasOffline) {
                            sayLog(ChatColor.GREEN + "与服务器的连接恢复");
                            wasOffline = false; // 重置状态
                        } else {
                            sayLog(ChatColor.GREEN + "与服务器的连接正常");
                        }
                        plugin.setIsTransferring(false);
                        plugin.getCheckingTask().processQueue();
                        serverOnlineInfo = true;
                        if (oneWasOffline) {
                            try {
                                InetAddress address = InetAddress.getByName(ip);
                                byte[] infoMessage = "LoginSequence-Info".getBytes();
                                DatagramPacket infoPacket = new DatagramPacket(infoMessage, infoMessage.length, address, port);
                                socket.send(infoPacket);
                                plugin.getLogger().info("已请求服务器状态信息");
                            } catch (Exception e) {
                                plugin.getLogger().warning("自动状态请求失败: " + e.getMessage());
                            }
                        }
                        oneWasOffline = false;
                    } else if (message.startsWith("LoginSequence-Data")) {
                        // 收到响应时取消超时检测
                        if (infoTimeoutTask != null) { // 添加空指针检查
                            infoTimeoutTask.cancel();
                            infoTimeoutTask = null; // 重置引用
                        }
                        // 数据解析逻辑
                        parseServerData(message); // 调用解析方法
                    } else {
                        plugin.getLogger().warning("收到未知的数据包");
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

    public static String getServerName() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                new File("plugins"+File.separator+"LoginSequence"+File.separator+"Config.yml")); // 优化路径分隔符
        String getServerNames;
        try {
            // 获取服务器名称
            getServerNames = Objects.requireNonNull(yaml.getString("server")).strip();
            sayLogStatic(ChatColor.GREEN+"成功获取到了子服务器名称： "+getServerNames);
        } catch (NullPointerException var3) {
            // 无法读取数据，使用默认值
            getServerNames = "lobby";
            sayLogStatic(ChatColor.YELLOW+"[!] 配置文件读取错误，已设置为默认值：lobby");
        }
        return getServerNames;
    }

    // 服务器连通性测试任务
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

    // 超时检测
    private void startTimeoutDetection() {
        currentTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().warning("请求超时");
                sayLog(ChatColor.RED + "服务器连接超时，可能已经离线");
                wasOffline = true;
                serverOnlineInfo = false; // 设置状态
                plugin.getCheckingTask().notifyQueuePositions();
            }
        };
        // 5秒后执行（20 ticks/秒 * 5 = 100 ticks）
        currentTimeoutTask.runTaskLater(plugin, 100L);
    }

    // INFO超时检测
    private void infoTimeoutDetection() {
        infoTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().warning("无法获取远程服务器信息，请求超时");
                plugin.silentPingTest();
                // 重置远程服务器在线信息
                memUsage = -1;
                onlinePlayers = -1;
                serverTPS = -1.0;
                maxQuantity = -1;
                quantityPercentage = -1.0;
            }
        };
        // 5秒后执行（20 ticks/秒 * 5 = 100 ticks）
        infoTimeoutTask.runTaskLater(plugin, 100L);
    }

    // 处理Hello包发送
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

    // 手动测试方法
    public void manualPing(CommandSender sender) {
        plugin.getLogger().info("手动触发连接性测试...");
        if (!testingServer) {
            sendServerStatusMessage(sender, ChatColor.YELLOW + "服务器在线检测功能已关闭");
            return;
        }
        boolean success = sendHelloPacket(ip, port);
        String message = success ? ChatColor.GREEN + "已发送检测请求，等待服务器响应..." : ChatColor.RED + "连接性测试失败";
        sendServerStatusMessage(sender, message);
        if (success) {
            startTimeoutDetection(); // 启动超时检测
        }
    }

    // 统一消息处理方法
    private void sendServerStatusMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            ((Player) sender).sendMessage(message);
        } else {
            sender.sendMessage(message);
        }
    }

    public void performConnectivityTestForPlayer(final Player player) {
        plugin.getLogger().info("开始为玩家 " + player.getName() + " 进行连接性测试...");
        boolean success = sendHelloPacket(ip, port); // 复用发送方法
        // 超时检测逻辑
        new ConnectivityTimeoutCheck(player, success).runTaskTimer(plugin, 0L, 20L);
    }

    // 超时检测内部类
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
                player.sendMessage(ChatColor.RED + "无法连接服务器，联系服务器管理员");
                serverOnlineInfo = false;
                // plugin.silentPingTest();
            }
        }
    }

    // 静默检测方法
    public boolean silentPingCheck() {
        boolean originalLogState = outLog;  // 保存原始日志状态
        outLog = false;  // 临时关闭日志输出
        boolean result = sendHelloPacket(ip, port);
        outLog = originalLogState;  // 恢复日志设置
        return result;
    }

    public void sendInfoRequest(CommandSender sender) {
        if(!serverOnlineInfo){
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "logseq cmdpingtest");
        }
        if(serverOnlineInfo){
            try {
                InetAddress address = InetAddress.getByName(ip);
                byte[] message = "LoginSequence-Info".getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
                socket.send(packet);
                infoTimeoutDetection(); // 启动超时检测
                sender.sendMessage(ChatColor.GREEN + "已发送服务器状态查询请求");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "请求发送失败: " + e.getMessage());
            }
        } else {
            // 重置远程服务器在线信息
            memUsage = -1;
            onlinePlayers = -1;
            serverTPS = -1.0;
            maxQuantity = -1;
            quantityPercentage = -1.0;
            if(pionli){
                sender.sendMessage(ChatColor.RED + "服务器未在线，请稍后再试");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "未开启服务器在线检测");
            }
        }
    }

    // 数据解析方法
    private void parseServerData(String data) {
        try {
            String[] parts = data.split(" ");
            // 格式示例："LoginSequence-Data Memory:1024 Online:5 TPS:19.9 MaxQuantity:20"
            // 调整索引位置（原代码分割后 parts[0] 是前缀）
            memUsage = Integer.parseInt(parts[1].split(":")[1].replace("MB", "")); // 内存使用量
            onlinePlayers = Integer.parseInt(parts[2].split(":")[1]); // 在线玩家数
            serverTPS = Double.parseDouble(parts[3].split(":")[1]); // 服务器TPS
            maxQuantity = Integer.parseInt(parts[4].split(":")[1]); // 最大玩家数
            plugin.getLogger().info("已更新服务器状态数据");
        } catch (Exception e) {
            plugin.getLogger().warning("数据解析失败: " + data);
        }
        // 计算在线玩家百分比
        quantityPercentage = (double) onlinePlayers / maxQuantity * 100;
    }

    // 检查服务器负载是否过高
    public boolean isServerOverloaded() {
        return quantityPercentage >= 90.0;
    }

    //  获取连接性测试状态
    public boolean isConnectivityTestEnabled() {
        return pionli;
    }

    // 获取服务器在线信息
    public boolean isGetServerOnlineInfo(){
        return serverOnlineInfo;
    }

    //获取服务器在线信息
    public boolean isServerInfoGet(){
        boolean ret = false;
        if(serverOnlineInfo && pionli){
            ret = true;
        }
        return ret;
    }

    public static boolean isStaServerInfoGet(){
        boolean ret = false;
        if(serverOnlineInfo && pionli){
            ret = true;
        }
        return ret;
    }

    public static void setTestingServer(boolean eTestingServer){
        testingServer = eTestingServer;
    }

    private void sayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    private static void sayLogStatic(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }

    public int getMemUsage() {
        return memUsage;
    }
    public int getOnlinePlayers() {
        return onlinePlayers;
    }
    public double getServerTPS() {
        return serverTPS;
    }
    public String getTargetIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public double getQuantityPercentage() {
        return quantityPercentage;
    }
}
