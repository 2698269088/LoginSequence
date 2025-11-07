package top.mcocet.loginSequence.tasks;

import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.json.PlayerInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.Optional;

public class Register {
    public static boolean handleRegister(Player player, String[] args) {
        String uuid = player.getUniqueId().toString();
        Optional<PlayerInfo> playerInfoOpt = PlayerDataManager.getPlayer(uuid);

        // 检查是否已注册
        if (playerInfoOpt.isPresent() && playerInfoOpt.get().isRegistered()) {
            player.sendMessage(ChatColor.RED + "你已经注册过了，请使用 /login 命令登录");
            return false;
        }

        // 检查参数数量
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "用法: /register <密码> [验证密码]");
            return false;
        }

        String password = args[0];
        // 密码长度检查
        if (password.length() < 6) {
            player.sendMessage(ChatColor.RED + "密码长度至少为6位");
            return false;
        }
        
        // 密码强度检查
        if (!isPasswordStrong(password)) {
            player.sendMessage(ChatColor.RED + "密码必须包含字母和数字");
            return false;
        }

        // 如果配置要求二次验证（验证密码或邮箱），检查第二个参数
        if (FillTask.pwl) { // 假设pwl配置表示需要验证密码
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "请再次输入密码以确认");
                return false;
            }
            String confirmPassword = args[1];
            if (!password.equals(confirmPassword)) {
                player.sendMessage(ChatColor.RED + "两次输入的密码不一致");
                return false;
            }
        }

        // 注册玩家
        PlayerInfo playerInfo;
        if (playerInfoOpt.isPresent()) {
            playerInfo = playerInfoOpt.get();
        } else {
            playerInfo = new PlayerInfo();
            playerInfo.setPlayerName(player.getName());
            playerInfo.setUuid(uuid);
        }

        playerInfo.setPassword(password); // 注意：实际项目应存储哈希值
        playerInfo.setRegistered(true);
        // 设置最后登录时间
        playerInfo.setLastLoginTime(java.time.Instant.now().toString());
        // 重置失败次数
        playerInfo.setFailedAttempts(0);
        playerInfo.setLastFailedAttemptTime(0);
        PlayerDataManager.addPlayer(playerInfo);
        PlayerDataManager.asyncSave();

        player.sendMessage(ChatColor.GREEN + "注册成功！现在你可以使用 /login <密码> 命令登录了");
        return true;
    }
    
    /**
     * 检查密码强度
     * @param password 密码
     * @return 是否满足强度要求
     */
    private static boolean isPasswordStrong(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        
        return hasLetter && hasDigit;
    }
}