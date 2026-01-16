package top.mcocet.loginSequence.tasks.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import top.mcocet.loginSequence.LoginSequence;
import top.mcocet.loginSequence.tasks.Register;

public class RegisterCommand implements CommandExecutor {
    private final LoginSequence plugin;

    public RegisterCommand(LoginSequence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        
        // 检查玩家是否已经登录（注册后会自动登录）
        if (!LoginSequence.WaitLogin.contains(player)) {
            player.sendMessage(ChatColor.RED + "你已经登录了!");
            return true;
        }

        // 处理注册
        boolean success = Register.handleRegister(player, args);
        
        if (success) {
            // 注册成功后自动登录
            // 从等待登录列表中移除
            LoginSequence.WaitLogin.remove(player);
            
            player.sendMessage(ChatColor.GREEN + "注册成功！你已自动登录。");
            
            // 启动一个短暂的延迟，然后将玩家加入队列
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 检查是否启用了命令队列模式
                    if (top.mcocet.loginSequence.FillTask.enableCommandQueue) {
                        // 提示玩家需要执行命令才能加入队列
                        player.sendMessage(ChatColor.YELLOW + "注册成功！请执行 /logser 命令加入排队队列");
                        
                        // 移除玩家的限制效果
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW);
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    } else {
                        // 没有启用命令队列模式，自动加入队列
                        plugin.getQueue().add(player);
                        player.sendMessage(ChatColor.YELLOW + "您已加入服务器排队队列，当前排队位置：" + plugin.getQueue().size());
                        player.sendTitle(ChatColor.AQUA + "等待连接服务器，当前排队位置：" + ChatColor.YELLOW + plugin.getQueue().size(), "", 0, 100, 0);

                        // 只有在服务器在线时才处理队列
                        if (plugin.getPingOnline().isGetServerOnlineInfo() || !top.mcocet.loginSequence.FillTask.pionli) {
                            if (plugin.getCheckingTask() != null) {
                                plugin.getCheckingTask().notifyQueuePositions();
                                plugin.processQueue();
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "服务器当前不在线，请稍后再试");
                        }
                        
                        // 移除玩家的限制效果
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW);
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    }
                }
            }.runTaskLater(plugin, 20L); // 延迟1秒执行
        }
        
        return true;
    }
}