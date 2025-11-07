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
            // 重置失败次数
            playerInfo.setFailedAttempts(0);
            playerInfo.setLastFailedAttemptTime(0);
            // 更新最后登录时间
            playerInfo.setLastLoginTime(java.time.Instant.now().toString());
            PlayerDataManager.asyncSave();
            return true;
        } else {
            // 登录失败，更新错误计数器
            playerInfo.increaseFailedAttempts();
            int remainingAttempts = 5 - playerInfo.getFailedAttempts();
            player.sendMessage(ChatColor.RED + "密码错误！剩余尝试次数: " + remainingAttempts);
            
            // 如果失败次数达到上限，锁定账户
            if (remainingAttempts <= 0) {
                player.sendMessage(ChatColor.RED + "密码错误次数过多，账户已被锁定30分钟！");
                // 可以选择踢出玩家
                // player.kickPlayer(ChatColor.RED + "密码错误次数过多，账户已被锁定30分钟！");
            }
            PlayerDataManager.asyncSave();
            return false;
        }
    }

    private static boolean isLocked(PlayerInfo info) {
        return info.getFailedAttempts() >= 5 &&
                System.currentTimeMillis() - info.getLastFailedAttemptTime() < 30 * 60 * 1000;
    }
}