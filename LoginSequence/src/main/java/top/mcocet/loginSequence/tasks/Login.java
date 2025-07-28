package top.mcocet.loginSequence.tasks;

import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.json.PlayerInfo;
import top.mcocet.loginSequence.LoginSequence;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Optional;

public class Login {
    public static boolean handleLogin(Player player, String password) {
        String uuid = player.getUniqueId().toString();
        Optional<PlayerInfo> playerInfoOpt = PlayerDataManager.getPlayer(uuid);

        if (!playerInfoOpt.isPresent()) {
            player.sendMessage(ChatColor.RED + "你还没有注册，请先使用 /register 命令注册");
            return false;
        }

        PlayerInfo playerInfo = playerInfoOpt.get();
        if (!playerInfo.isRegistered()) {
            player.sendMessage(ChatColor.RED + "你还没有注册，请先使用 /register 命令注册");
            return false;
        }

        // 检查登录锁定状态
        if (isLocked(playerInfo)) {
            long remainingTime = (30 * 60 * 1000) - (System.currentTimeMillis() - playerInfo.getLastFailedAttemptTime());
            player.sendMessage(ChatColor.RED + "您的账号已被锁定，请在 " + ((int)remainingTime/1000/60)+1 + " 分钟后重试");
            return false;
        }

        // 验证密码
        if (playerInfo.verifyPassword(password)) {
            // 登录成功
            player.sendMessage(ChatColor.GREEN + "登录成功！");
            // 将玩家加入队列
            LoginSequence plugin = (LoginSequence) player.getServer().getPluginManager().getPlugin("LoginSequence");
            if (plugin != null) {
                player.sendMessage(ChatColor.AQUA + "玩家 " + player.getName() + " 加入了服务器！");
                plugin.getQueue().add(player);
                player.sendMessage(ChatColor.YELLOW + "您已加入服务器排队队列，当前排队位置：" + plugin.getQueue().size());
                player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + plugin.getQueue().size(), "", 0, 100, 0);

                // 只有在服务器在线时才处理队列
                if (plugin.getPingOnline().isGetServerOnlineInfo() || !FillTask.pionli) {
                    if (plugin.getCheckingTask() != null) {
                        plugin.getCheckingTask().notifyQueuePositions();
                        plugin.processQueue();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "服务器当前不在线，请稍后再试");
                }
            }
            return true;
        } else {
            // 登录失败，更新错误计数器
            playerInfo.increaseFailedAttempts();
            player.sendMessage(ChatColor.RED + "密码错误！剩余尝试次数: " + (5 - playerInfo.getFailedAttempts()));
            return false;
        }
    }

    private static boolean isLocked(PlayerInfo info) {
        return info.getFailedAttempts() >= 5 &&
                System.currentTimeMillis() - info.getLastFailedAttemptTime() < 30 * 60 * 1000;
    }
}
