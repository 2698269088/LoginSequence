package top.mcocet.loginSequence.tasks.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.GameMode;
import top.mcocet.loginSequence.LoginSequence;
import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.json.PlayerInfo;

import java.util.Optional;

public class LogoutCommand implements CommandExecutor {
    private final LoginSequence plugin;

    public LogoutCommand(LoginSequence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        
        // 检查玩家是否已经登录
        if (LoginSequence.WaitLogin.contains(player)) {
            player.sendMessage(ChatColor.RED + "你还没有登录!");
            return true;
        }

        // 将玩家添加到等待登录列表
        LoginSequence.WaitLogin.add(player);
        
        // 应用限制效果
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
        
        // 启动登录提示任务
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            while (player.isOnline() && LoginSequence.WaitLogin.contains(player)) {
                player.sendTitle(ChatColor.YELLOW + "请使用 /login <密码> 登录!", "", 10, 40, 10);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // 从队列中移除玩家
        plugin.getQueue().remove(player);
        
        // 如果启用了命令队列模式，提醒玩家需要执行命令才能重新加入队列
        if (top.mcocet.loginSequence.FillTask.enableCommandQueue) {
            player.sendMessage(ChatColor.YELLOW + "登出成功！请执行 /logser 命令重新加入排队队列。");
        } else {
            player.sendMessage(ChatColor.GREEN + "登出成功！请重新登录。");
        }
        
        return true;
    }
}