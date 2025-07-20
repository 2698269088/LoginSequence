package top.mcocet.loginSequence.tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;


import java.util.ArrayList;
import java.util.Queue;

public class ScoreboardTask {
    private final LoginSequence plugin;
    private final PingOnline pingOnline;
    private final Queue<Player> queue;

    public ScoreboardTask(LoginSequence plugin, PingOnline pingOnline, Queue<Player> queue) {
        this.plugin = plugin;
        this.pingOnline = pingOnline;
        this.queue = queue;
        startScoreboardUpdates();
    }

    // 创建初始计分板
    public void createScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("serverinfosco");
        if (objective == null) {
            objective = board.registerNewObjective("serverinfosco", "dummy", ChatColor.AQUA + "服务器信息");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            // 初始化标签和默认值
            objective.getScore(ChatColor.YELLOW + "服务器状态: " + ChatColor.WHITE + "N/A").setScore(5);
            objective.getScore(ChatColor.YELLOW + "负载百分比: " + ChatColor.WHITE + "N/A").setScore(4);
            objective.getScore(ChatColor.YELLOW + "服务器TPS: " + ChatColor.WHITE + "N/A").setScore(3);
            objective.getScore(ChatColor.YELLOW + "在线玩家: " + ChatColor.WHITE + "N/A").setScore(2);
            objective.getScore(ChatColor.YELLOW + "排队位置: " + ChatColor.WHITE + "N/A").setScore(1);

        }
    }

    // 更新单个玩家计分板
    public synchronized void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective("serverinfosco");
        if (objective == null) return;

        // 清除所有现有条目
        Scoreboard scoreboard = objective.getScoreboard();
        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry) != null) {
                scoreboard.resetScores(entry);
            }
        }

        // 获取服务器状态信息
        boolean serverOnline = pingOnline.isGetServerOnlineInfo();

        String onlineStatus = serverOnline ? ChatColor.GREEN + "在线" : ChatColor.RED + "离线";
        String loadPercentage = serverOnline ? String.format("%.1f%%", pingOnline.getQuantityPercentage()) : "N/A";
        String serverTPS = serverOnline ? String.format("%.1f", pingOnline.getServerTPS()) : "N/A";
        String onlinePlayers = serverOnline ? Integer.toString(pingOnline.getOnlinePlayers()) : "N/A";

        // 更新服务器信息
        updateScore(objective, ChatColor.YELLOW + "服务器状态: " + ChatColor.WHITE, onlineStatus, 5);
        updateScore(objective, ChatColor.YELLOW + "负载百分比: " + ChatColor.WHITE, loadPercentage, 4);
        updateScore(objective, ChatColor.YELLOW + "服务器TPS: " + ChatColor.WHITE, serverTPS, 3);
        updateScore(objective, ChatColor.YELLOW + "在线玩家: " + ChatColor.WHITE, onlinePlayers, 2);

        // 更新排队位置
        int position = queue.contains(player) ? new ArrayList<>(queue).indexOf(player) + 1 : 0;
        if (position < 0) {
            position = 0; // 防止负数索引
        }
        /*
        int position = 1;
        boolean found = false;
        for (Player p : loginSequence.getQueue()) {
            if (p != null && p.equals(player)) {
                found = true;
                break;
            }
            position++;
        }
        if (!found) position = 0;
         */

        updateScore(objective, ChatColor.YELLOW + "排队位置: " + ChatColor.WHITE, String.valueOf(position), 1);
    }

    // 辅助方法：更新 Score
    private void updateScore(Objective objective, String prefix, String value, int score) {
        String fullText = prefix + value;
        Score scoreEntry = objective.getScore(fullText);
        scoreEntry.setScore(score);
    }

    // 定时更新所有在线玩家的计分板
    private void startScoreboardUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getScoreboard().getObjective("serverinfosco") != null) {
                        updateScoreboard(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }

    // 处理玩家加入事件
    public void handlePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        createScoreboard(player);
        updateScoreboard(player);
    }
}
