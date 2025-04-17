package top.mcocet.loginSequence;

import top.mcocet.loginSequence.tasks.CheckingTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

import java.util.LinkedList;
import java.util.Queue;

public class LoginSequence extends JavaPlugin implements Listener {

    private final Queue<Player> queue = new LinkedList<>();
    private final String SC_SERVER_NAME = "lobby";
    private boolean isTransferring = false;
    private CheckingTask checkingTask;

    public LoginSequence() {
    }

    public void onEnable() {
        // 初始化BC连接
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getLogger().info("LoginSequence已启用！");
        checkingTask = new CheckingTask(this);
        (new BukkitRunnable() {
            public void run() {
                checkingTask.notifyQueuePositions();
            }
        }).runTaskTimer(this, 0L, 100L);
    }

    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getLogger().info("LoginSequence已关闭！");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.queue.add(player);
        // 显示玩家排队的位置
        player.sendMessage(ChatColor.YELLOW + "您已加入服务器排队队列，当前排队位置：" + this.queue.size());
        player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + this.queue.size(), "", 0, 100, 0);
        // 设置玩家隐身和失明效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        player.setGameMode(GameMode.ADVENTURE);
        if (!this.isTransferring) {
            checkingTask.processQueue();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.queue.remove(player);
    }

    public void processQueue() {
        checkingTask.processQueue();
    }

    public void sendPlayerToSC(Player player) {
        checkingTask.sendPlayerToSC(player);
    }

    public Queue<Player> getQueue() {
        return queue;
    }

    public void setIsTransferring(boolean isTransferring) {
        this.isTransferring = isTransferring;
    }

    public boolean getIsTransferring() {
        return isTransferring;
    }
}