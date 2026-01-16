package top.mcocet.loginSequence.tasks.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;

public class LogserCommand implements CommandExecutor {
    private final LoginSequence plugin;

    public LogserCommand(LoginSequence plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        
        // 检查是否启用了命令队列模式
        if (!FillTask.enableCommandQueue) {
            player.sendMessage(ChatColor.RED + "服务器未启用命令加入队列模式");
            return true;
        }
        
        // 将玩家加入队列
        plugin.addToQueue(player);
        return true;
    }
}