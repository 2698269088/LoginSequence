package top.mcocet.loginSequence;

// 特别说明：
// 因为之前原作者手残
// 导致插件源代码全部丢失
// 现在你看到的是反编译后得到的版本（
// 如果你觉得代码写得很抽象
// 我也没办法（
// 这是反编译后又经过AI优化并增加注释的版本（（（
// 将就着看吧（（（（
// :(

import java.util.LinkedList;
import java.util.Queue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
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
import top.mcocet.loginSequence.tasks.CheckingTask;
import top.mcocet.loginSequence.tasks.PingOnline;

public class LoginSequence extends JavaPlugin implements Listener {
    private final Queue<Player> queue = new LinkedList(); // 玩家排队队列
    private String targetServer; // 目标服务器
    private boolean isTransferring = false; // 是否正在转移玩家
    private CheckingTask checkingTask; // 检查任务实例
    private PingOnline pingOnline; // 在线检测实例

    public void onEnable() {
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        // 注册BungeeCord插件通道
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // logo
        sayLog(ChatColor.AQUA+"    "+" "+ChatColor.BLUE+" __ "+" "+ChatColor.YELLOW+" ___");
        sayLog(ChatColor.AQUA+"|   "+" "+ChatColor.BLUE+"(__ "+" "+ChatColor.YELLOW+"|___");
        sayLog(ChatColor.AQUA+"|___"+" "+ChatColor.BLUE+" __)"+" "+ChatColor.YELLOW+"|___"+ChatColor.GREEN+"    LoginSequence v1.3.2");
        getLogger().info("LoginSequence已启用！");
        // 初始化检查任务和在线检测
        checkingTask = new CheckingTask(this);
        pingOnline = new PingOnline(this);
        // 如果启用了连接性测试，则开始测试
        if (pingOnline.isConnectivityTestEnabled()) {
            pingOnline.startConnectivityTest();
        } else {
            isTransferring = false;
            checkingTask.processQueue();
        }

        // 定时通知玩家排队位置
        (new BukkitRunnable() {
            public void run() {
                LoginSequence.this.checkingTask.notifyQueuePositions();
            }
        }).runTaskTimer(this, 0L, 100L);
        // 设置命令执行器
        getCommand("logseq").setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "请指定子命令。用法: /logseq ping");
                return false;
            } else if (args[0].equalsIgnoreCase("ping")) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (!player.hasPermission("logseq.ping")) {
                        player.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
                        return false;
                    }

                    pingOnline.manualPing(player);
                } else if (sender.isOp()) {
                    pingOnline.manualPing(sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "此命令只能由玩家或控制台执行。");
                }

                return true;
            } else if (args[0].equalsIgnoreCase("cmdpingtest")) {  // 新增cmdpingtest命令处理
                if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                    sender.sendMessage(ChatColor.RED + "该指令只能由控制台执行");
                    return false;
                }
                silentPingTest();  // 执行静默检测
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "未知的子命令。");
                return false;
            }
        });
    }

    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("LoginSequence已关闭！");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
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
            }
        }.runTaskLater(this, 1L); // 延迟 1 tick 执行
        // 2秒后执行的代码
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return; // 检查玩家是否还在线
                String playerName = player.getName();
                playerInitOut(playerName);
                player.sendMessage(ChatColor.AQUA + "玩家 " + player.getName() + " 加入了服务器！");
                queue.add(player);
                ChatColor var10001 = ChatColor.YELLOW;
                player.sendMessage(var10001 + "您已加入服务器排队队列，当前排队位置：" + queue.size());
                player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + queue.size(), "", 0, 100, 0);

                if (pingOnline.isConnectivityTestEnabled()) {
                    pingOnline.performConnectivityTestForPlayer(player);
                } else {
                    isTransferring = false;
                    checkingTask.processQueue();
                }
            }
        }.runTaskLater(this, 40L); // 60 ticks = 3秒
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 禁止玩家移动
        if (event.getFrom().distanceSquared(event.getTo()) > 0.25) {
            event.setCancelled(true);
            event.getPlayer().teleport(event.getFrom());
        }
    }


    public void playerInitOut(String playerName){
        sayLog(ChatColor.GOLD+"========================================");
        sayLog(ChatColor.GOLD+"=");
        sayLog(ChatColor.GOLD+"= "+ChatColor.AQUA+"玩家 " + playerName + " 加入了服务器");
        sayLog(ChatColor.GOLD+"=");
        sayLog(ChatColor.GOLD+"========================================");
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

    // 静默检测方法
    private void silentPingTest() {
        boolean success = pingOnline.silentPingCheck();
        // 静默模式不输出任何日志
    }

    private void sayLog(String s) {
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }
}
