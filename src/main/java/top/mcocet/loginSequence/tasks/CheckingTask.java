package top.mcocet.loginSequence.tasks;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import top.mcocet.loginSequence.LoginSequence;

public class CheckingTask {
    private static final String SERVER_CONNECT_COMMAND = "Connect";
    private final LoginSequence loginSequence;
    private static final String serverName = PingOnline.getServerName();

    public CheckingTask(LoginSequence loginSequence) {
        this.loginSequence = loginSequence;
    }

    public void processQueue() {
        if (loginSequence.getQueue().isEmpty()) {
            loginSequence.setIsTransferring(false);
        } else {
            final Player nextPlayer = (Player)loginSequence.getQueue().poll();
            if (nextPlayer != null && nextPlayer.isOnline()) {
                loginSequence.setIsTransferring(true);
                nextPlayer.sendMessage(ChatColor.GREEN + "正在尝试连接服务器...");
                (new BukkitRunnable() {
                    int attempts = 0;

                    public void run() {
                        if (nextPlayer.isOnline()) {
                            CheckingTask.this.sendPlayerToSC(nextPlayer);
                            int quantity = attempts + 1;
                            nextPlayer.sendMessage(ChatColor.AQUA + "正在尝试连接服务器（第 " + quantity + " 次）...");
                            nextPlayer.sendActionBar(ChatColor.AQUA + "正在尝试连接服务器（第 " + quantity + " 次）...");
                            executeSilentPingTest();
                            loginSequence.getLogger().log(Level.INFO, "玩家 {0} 正在尝试连接服务器{1}: {2}次", new Object[]{nextPlayer.getName(), serverName, quantity});
                            if (quantity <= 1) {
                                nextPlayer.sendTitle("", "", 0, 0, 0);
                                nextPlayer.sendActionBar("");
                            }

                            ++attempts;
                        } else {
                            CheckingTask.this.loginSequence.setIsTransferring(false);
                            CheckingTask.this.loginSequence.processQueue();
                            cancel();
                        }

                    }
                }).runTaskTimer(loginSequence, 0L, 100L);
            } else {
                processQueue();
            }
        }

    }

    public void sendPlayerToSC(Player player) {
        // 清除玩家状态效果
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF(SERVER_CONNECT_COMMAND);
            out.writeUTF(serverName);
            player.sendPluginMessage(loginSequence, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            loginSequence.getLogger().log(Level.SEVERE, "数据流操作异常", e);
        } catch (Exception e) {
            String errorMsg = String.format("玩家 %s 无法连接服务器: %s", player.getName(), e.getMessage());
            player.sendMessage(ChatColor.AQUA + "[Server]: " + ChatColor.RED + "无法连接到服务器，请稍后再试。");
            loginSequence.getLogger().log(Level.WARNING, errorMsg, e);
        }
    }

    public void notifyQueuePositions() {
        int position = 1;

        for(Player player : loginSequence.getQueue()) {
            if (player.isOnline()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "当前排队位置：" + ChatColor.YELLOW + position);
                player.sendTitle(ChatColor.LIGHT_PURPLE + "等待连接服务器，当前排队位置：" + ChatColor.AQUA + position, "", 0, 100, 0);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "logseq cmdpingtest");
                ++position;
            }
        }

    }

    private void executeSilentPingTest() {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "logseq cmdpingtest");
    }
}