package top.mcocet.loginSequence;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.json.PlayerInfo;
import top.mcocet.loginSequence.tasks.*;
import top.mcocet.loginSequence.tasks.commands.*;

import static top.mcocet.loginSequence.FillTask.readConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;


public class LoginSequence extends JavaPlugin implements Listener {
    private String targetServer; // 目标服务器
    private boolean isTransferring = false; // 是否正在转移玩家
    private boolean piEula = false; // 玩家数据存储开关
    private PingOnline pingOnline; // 在线检测实例
    private CheckingTask checkingTask; // 检查任务实例
    private CommandTask commandTask; // 命令任务实例
    private SecureLogin secureLogin; // 登录验证实例
    private PlayerDataManager playerDataManager; // 玩家数据管理实例
    private PlayerInfo playerInfo; // 玩家信息
    private ScoreboardTask scoreboardTask; // 计分板任务实例
    private LoginCommand loginCommand; // 登录命令实例
    private RegisterCommand registerCommand; // 注册命令实例
    private LogoutCommand logoutCommand; // 登出命令实例
    private LogserCommand logserCommand; // 加入队列命令实例

    private final Queue<Player> queue = new LinkedList(); // 玩家排队队列
    public static ArrayList<Player> WaitLogin = new ArrayList<>(); // 等待登录的玩家列表
    public static ArrayList<Player> CommandQueuePlayers = new ArrayList<>(); // 已执行命令并等待加入队列的玩家列表
    private Map<String, Integer> playerWaitStatus = new HashMap<>(); // 玩家等待状态映射，key为玩家名称，value为等待状态（0:未等待, 1:正在等待）
    
    /**
     * 设置玩家的等待状态
     * @param playerName 玩家名称
     * @param inWaiting 是否在等待期内
     */
    public void setPlayerInWaitingPeriod(String playerName, boolean inWaiting) {
        if (inWaiting) {
            playerWaitStatus.put(playerName, 1);
        } else {
            playerWaitStatus.remove(playerName);
        }
    }
    
    /**
     * 获取玩家等待状态映射
     * @return 玩家等待状态映射
     */
    public Map<String, Integer> getPlayerWaitStatus() {
        return playerWaitStatus;
    }
    
    /**
     * 检查玩家是否在等待期内
     * @param playerName 玩家名称
     * @return 是否在等待期内
     */
    public boolean isPlayerInWaitingPeriod(String playerName) {
        return playerWaitStatus.containsKey(playerName) && playerWaitStatus.get(playerName) == 1;
    }

    public void onEnable() {
        // logo
        sayLog(ChatColor.AQUA+"    "+" "+ChatColor.BLUE+" __ "+" "+ChatColor.YELLOW+" ___");
        sayLog(ChatColor.AQUA+"|   "+" "+ChatColor.BLUE+"(__ "+" "+ChatColor.YELLOW+"|___");
        sayLog(ChatColor.AQUA+"|___"+" "+ChatColor.BLUE+" __)"+" "+ChatColor.YELLOW+"|___"+ChatColor.GREEN+"    LoginSequence v1.10");
        sayLog("");
        // 注册事件监听器和BungeeCord通道
        Bukkit.getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        // 初始化任务
        FillTask.initConfig(this); // 初始化配置文件
        FillTask.readConfig(this); // 读取配置文件
        pingOnline = new PingOnline(this); // 初始化在线检测
        checkingTask = new CheckingTask(this); // 初始化BC跳转
        checkingTask.setPingOnline(pingOnline); // 设置PingOnline实例
        commandTask = new CommandTask(this, pingOnline); // 初始化指令
        secureLogin = new SecureLogin(this); // 初始化安全登录
        playerDataManager = new PlayerDataManager(); // 初始化玩家数据管理
        playerInfo = new PlayerInfo(); // 初始化玩家信息
        scoreboardTask = new ScoreboardTask(this, pingOnline, queue); // 初始化计分板任务
        loginCommand = new LoginCommand(this); // 创建登录指令
        registerCommand = new RegisterCommand(this); // 创建注册指令
        logoutCommand = new LogoutCommand(this); // 创建登出指令
        logserCommand = new LogserCommand(this); // 创建加入队列指令


        // 初始化指令
        getCommand("logseq").setExecutor(commandTask);
        getCommand("ls").setExecutor(commandTask);
        getCommand("login").setExecutor(loginCommand);
        getCommand("register").setExecutor(registerCommand);
        getCommand("logout").setExecutor(logoutCommand);
        getCommand("logser").setExecutor(logserCommand);
        
        // 添加日志过滤器
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addFilter(new LogFilter());
        context.updateLoggers();
        
        // 注册玩家认证监听器
        getServer().getPluginManager().registerEvents(new PlayerAuthListener(), this);

        getLogger().info("LoginSequence已启用！");

        piEula = FillTask.piEula;

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                secureLogin.onPlayerJoin(event);
                scoreboardTask.handlePlayerJoin(event); // 处理计分板初始化
            }
        }, this);

        // 如果启用了连接性测试，则开始测试
        if (pingOnline.isConnectivityTestEnabled()) {
            pingOnline.startConnectivityTest();
        } else {
            isTransferring = false;
            checkingTask.processQueue();
        }

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                secureLogin.onPlayerJoin(event);
            }
        }, this);

        // 定时通知玩家排队位置
        (new BukkitRunnable() {
            public void run() {
                LoginSequence.this.checkingTask.notifyQueuePositions();
            }
        }).runTaskTimer(this, 0L, 100L);

    }

    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("LoginSequence已关闭！");
    }

    // 玩家加入事件处理
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 确保安全登录模块初始化
        // secureLogin.initIfNeeded();
        // 玩家加入后自动获取远程服务器状态
        if (pingOnline.isGetServerOnlineInfo()) {
            pingOnline.sendInfoRequest(event.getPlayer());
        }

        Player player = event.getPlayer();
        if (!player.isValid()){
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.setGameMode(GameMode.ADVENTURE);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
                }

                // 存储基础数据
                Player player = event.getPlayer();
                String playerName = player.getName();
                String uuid = player.getUniqueId().toString();
                String playerIP = player.getAddress().getAddress().getHostAddress();
                Optional<PlayerInfo> existingPlayer = PlayerDataManager.getPlayer(uuid);
                if (!existingPlayer.isPresent()) {
                    PlayerInfo info = new PlayerInfo();
                    info.setPlayerName(playerName);
                    info.setUuid(uuid);
                    // 仅在piEula启用时记录敏感信息
                    if (FillTask.piEula) {
                        String currentTime = Instant.now().toString();
                        info.setFirstJoinTime(currentTime);
                        info.setLastJoinTime(currentTime);
                        info.updateLoginIP(playerIP);
                    }
                    PlayerDataManager.addPlayer(info);
                    PlayerDataManager.asyncSave();
                    LoginSequence.this.getLogger().info("已异步存储新玩家数据: " + playerName);
                } else {
                    PlayerInfo info = existingPlayer.get();
                    info.updateLastJoinTime(); // 可能不更新时间
                    // 仅在pi_Eula启用时更新
                    if (FillTask.piEula) {
                        info.updateLoginIP(playerIP);
                    }
                    PlayerDataManager.asyncSave();
                    LoginSequence.this.getLogger().info("已异步更新玩家最后加入时间: " + playerName);
                }
            }
        }.runTaskLater(this, 1L); // 延迟 1 tick 执行
        // 1秒后执行的代码
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()){
                    return; // 检查玩家是否还在线
                }
                String playerName = player.getName();
                playerInitOut(playerName);
                player.sendMessage(ChatColor.AQUA + "玩家 " + player.getName() + " 加入了服务器！");
                // 如果启用了密码登录，检查玩家是否已登录
                if(FillTask.pwl){
                    Optional<PlayerInfo> playerInfo = PlayerDataManager.getPlayer(player.getUniqueId().toString());
                    if (playerInfo.isPresent() && playerInfo.get().isRegistered()) {
                        // 已注册玩家，提示登录
                        player.sendMessage(ChatColor.YELLOW + "请使用 /login <密码> 登录");
                        // 添加玩家到等待登录列表
                        WaitLogin.add(player);
                        // 启动登录提示任务
                        startLoginPromptTask(player, true); // true表示已注册
                        // 不将玩家加入队列，直到登录成功
                        return;
                    } else {
                        // 未注册玩家，提示注册
                        player.sendMessage(ChatColor.YELLOW + "请使用 /register <密码> [验证密码] 注册账号");
                        // 添加玩家到等待登录列表
                        WaitLogin.add(player);
                        // 启动注册提示任务
                        startLoginPromptTask(player, false); // false表示未注册
                        // 不将玩家加入队列，直到注册并登录成功
                        return;
                    }
                }
                
                // 如果启用了命令队列模式，则不自动加入队列
                if (FillTask.enableCommandQueue) {
                    // 提示玩家需要执行命令才能加入队列
                    player.sendMessage(ChatColor.YELLOW + "请执行 /logser 命令加入排队队列");
                    return;
                }
                
                // 处理后续的登录逻辑
                playerLoginInit(player);
            }
        }.runTaskLater(this, 20L);
    }

    public void playerLoginInit(Player player){
        // 如果启用了命令队列模式，则不自动加入队列
        if (FillTask.enableCommandQueue) {
            // 提示玩家需要执行命令才能加入队列
            player.sendMessage(ChatColor.YELLOW + "请执行 /logser 命令加入排队队列");
            return;
        }
        
        // 如果没有启用密码登录，则正常加入队列
        queue.add(player);
        ChatColor var10001 = ChatColor.YELLOW;
        player.sendMessage(var10001 + "您已加入服务器排队队列，当前排队位置：" + queue.size());
        player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + queue.size(), "", 0, 100, 0);

        // 只有在服务器在线时才处理队列
        if (pingOnline.isGetServerOnlineInfo() || !FillTask.pionli) {
            if (pingOnline.isConnectivityTestEnabled()) {
                pingOnline.performConnectivityTestForPlayer(player);
            } else {
                isTransferring = false;
                checkingTask.processQueue();
            }
        } else {
            player.sendMessage(ChatColor.RED + "服务器当前不在线，请稍后再试");
        }
    }
    
    /**
     * 将玩家加入排队队列
     * @param player 玩家
     */
    public void addToQueue(Player player) {
        // 检查玩家是否已经在队列中
        if (queue.contains(player)) {
            player.sendMessage(ChatColor.RED + "您已经在排队队列中了！");
            return;
        }
        
        queue.add(player);
        ChatColor var10001 = ChatColor.YELLOW;
        player.sendMessage(var10001 + "您已加入服务器排队队列，当前排队位置：" + queue.size());
        player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + queue.size(), "", 0, 100, 0);

        // 只有在服务器在线时才处理队列
        if (pingOnline.isGetServerOnlineInfo() || !FillTask.pionli) {
            if (pingOnline.isConnectivityTestEnabled()) {
                pingOnline.performConnectivityTestForPlayer(player);
            } else {
                isTransferring = false;
                checkingTask.processQueue();
            }
        } else {
            player.sendMessage(ChatColor.RED + "服务器当前不在线，请稍后再试");
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 禁止玩家移动
        if (event.getFrom().distanceSquared(event.getTo()) > 0.25) {
            event.setCancelled(true);
            event.getPlayer().teleport(event.getFrom());
        }
    }

    // 玩家加入日志
    public void playerInitOut(String playerName){
        sayLog(ChatColor.GOLD+"========================================");
        sayLog(ChatColor.GOLD+"=");
        sayLog(ChatColor.GOLD+"= "+ChatColor.AQUA+"玩家 " + playerName + " 加入了服务器");
        sayLog(ChatColor.GOLD+"=");
        sayLog(ChatColor.GOLD+"========================================");
    }

    // 帮助信息处理方法
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "==== LoginSequence 指令帮助 ====");
        sender.sendMessage(ChatColor.GOLD + "/logseq ping" + ChatColor.WHITE + " - 测试服务器连通性");
        sender.sendMessage(ChatColor.GOLD + "/logseq info" + ChatColor.WHITE + " - 请求服务器状态数据");
        sender.sendMessage(ChatColor.GOLD + "/logseq stavie" + ChatColor.WHITE + " - 查看服务器实时状态");
        sender.sendMessage(ChatColor.GOLD + "/logseq help" + ChatColor.WHITE + " - 显示本帮助信息");
        sender.sendMessage(ChatColor.GOLD + "/logseq list" + ChatColor.WHITE + " - 查看玩家数据列表");
        sender.sendMessage(ChatColor.GOLD + "/logseq link" + ChatColor.WHITE + " - 直接跳转到子服务器");
    }

    // 状态查看处理方法
    private void handleStatusRequest(CommandSender sender) {
        if (sender instanceof Player && !sender.hasPermission("logseq.stavie")) {
            sender.sendMessage(ChatColor.RED + "你没有权限查看服务器状态");
            return;
        }

        String status = String.format(
                ChatColor.AQUA + "服务器状态:\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "内存占用: %dMB\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "在线玩家: %d/%d (%.1f%%)\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "TPS: %.1f",
                pingOnline.getMemUsage(),
                pingOnline.getOnlinePlayers(),
                pingOnline.getMaxQuantity(),
                pingOnline.getQuantityPercentage(),
                pingOnline.getServerTPS()
        );
        sender.sendMessage(status);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        queue.remove(player);
    }

    public void processQueue() {
        checkingTask.processQueue();
    }

    public Queue<Player> getQueue() {
        return queue;
    }

    public void setIsTransferring(boolean isTransferring) {
        isTransferring = isTransferring;
    }

    public boolean getIsTransferring() {
        return isTransferring;
    }

    public CheckingTask getCheckingTask() {
        return checkingTask;
    }

    public PingOnline getPingOnline() { return pingOnline; }

    // 静默检测方法
    public void silentPingTest() {
        boolean success = pingOnline.silentPingCheck();
        // 静默模式不输出任何日志
    }

    /**
     * 启动登录提示任务
     * @param player 玩家
     * @param isRegistered 是否已注册
     */
    private void startLoginPromptTask(Player player, boolean isRegistered) {
        new BukkitRunnable() {
            @Override
            public void run() {
                while (player.isOnline() && WaitLogin.contains(player)) {
                    if (isRegistered) {
                        player.sendTitle(ChatColor.YELLOW + "请使用 /login <密码> 登录!", "", 10, 40, 10);
                    } else {
                        player.sendTitle(ChatColor.YELLOW + "请使用 /register <密码> [验证密码] 注册!", "", 10, 40, 10);
                    }
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }.runTaskAsynchronously(this);
    }

    private void sayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }
}
