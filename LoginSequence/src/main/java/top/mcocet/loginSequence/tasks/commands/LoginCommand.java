package top.mcocet.loginSequence.tasks.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;
import top.mcocet.loginSequence.tasks.Login;

public class LoginCommand implements CommandExecutor {
    private final LoginSequence plugin;

    public LoginCommand(LoginSequence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /login <密码>");
            return false;
        }

        String password = args[0];
        Player player = (Player) sender;
        boolean success = Login.handleLogin(player, password);

        // 如果登录成功，将玩家加入队列
        if (success) {
            player.sendMessage(ChatColor.AQUA + "玩家 " + player.getName() + " 登录成功！");
            /*
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
            */
        }
        return true;
    }
}
