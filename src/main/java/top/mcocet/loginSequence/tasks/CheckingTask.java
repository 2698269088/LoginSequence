package top.mcocet.loginSequence.tasks;

import top.mcocet.loginSequence.LoginSequence;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.bukkit.Bukkit.getLogger;

public class CheckingTask {

    private final String SC_writeUTF_SERVER_NAME = "lobby";
    private final String SC_RETURNR_NAME = "Connect";

    private LoginSequence loginSequence;

    public CheckingTask(LoginSequence loginSequence) {
        this.loginSequence = loginSequence;
    }

    public void processQueue() {
        if (loginSequence.getQueue().isEmpty()) {
            loginSequence.setIsTransferring(false);
        } else {
            final Player nextPlayer = loginSequence.getQueue().poll();
            // 连接服务器
            if (nextPlayer != null && nextPlayer.isOnline()) {
                loginSequence.setIsTransferring(true);
                nextPlayer.sendMessage(ChatColor.GREEN + "正在尝试连接服务器...");
                (new BukkitRunnable() {
                    int attempts = 0;

                    // 开始连接服务器
                    public void run() {
                        if (nextPlayer.isOnline()) {
                            loginSequence.sendPlayerToSC(nextPlayer);
                            // 尝试连接服务器的次数
                            int quantity = (this.attempts + 1);
                            nextPlayer.sendMessage(ChatColor.AQUA + "正在尝试连接服务器（第 " + quantity + " 次）...");
                            nextPlayer.sendActionBar(ChatColor.AQUA + "正在尝试连接服务器（第 " + quantity + " 次）...");
                            System.out.println("玩家 " + nextPlayer + " 正在连接服务器   " + quantity);
                            if(quantity<=1){
                                nextPlayer.sendTitle("", "", 0, 0, 0); // 清除标题
                                nextPlayer.sendActionBar(""); // 清除动作栏
                            }
                            this.attempts++;
                        } else {
                            loginSequence.setIsTransferring(false);
                            loginSequence.processQueue();
                            this.cancel();
                        }
                    }
                }).runTaskTimer(loginSequence, 0L, 100L);
            } else {
                this.processQueue();
            }
        }
    }

    public void sendPlayerToSC(Player player) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF(SC_RETURNR_NAME);
            out.writeUTF(SC_writeUTF_SERVER_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // 尝试执行可能抛出异常的代码
            // 用于向BC报告要连接的服务端
            player.sendPluginMessage(loginSequence, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            // 捕获其他可能的异常
            player.sendMessage(ChatColor.AQUA + "[Server]: " + ChatColor.RED + "无法连接到服务器，请稍后再试。");
            getLogger().warning("玩家 " + player.getName() + " 无法连接到服务器，发生未知错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void notifyQueuePositions() {
        int position = 1;

        // 处理队列
        for(Player player : loginSequence.getQueue()) {
            if (player.isOnline()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "当前排队位置：" + ChatColor.YELLOW + position);
                player.sendTitle(ChatColor.LIGHT_PURPLE + "等待连接服务器，当前排队位置：" + ChatColor.AQUA + position, "", 0, 100, 0);
                ++position;
            }
        }
    }
}