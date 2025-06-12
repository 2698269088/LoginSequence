package top.mcocet.loginSequence.tasks;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import top.mcocet.loginSequence.LoginSequence;

public class CommandTask implements CommandExecutor {
    private final LoginSequence plugin;
    private final PingOnline pingOnline;

    public CommandTask(LoginSequence plugin, PingOnline pingOnline) {
        this.plugin = plugin;
        this.pingOnline = pingOnline;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                showHelp(sender);
                return true;

            case "ping":
                if (!checkPermission(sender, "logseq.ping")) return true;
                pingOnline.manualPing(sender);
                return true;

            case "info":
                if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(ChatColor.RED + "该指令只能由玩家或控制台执行");
                    return false;
                }
                pingOnline.sendInfoRequest(sender);
                return true;

            case "stavie":
                if (sender instanceof Player && !checkPermission(sender, "logseq.stavie")) return true;
                handleStatusRequest(sender);
                return true;

            case "cmdpingtest":
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(ChatColor.RED + "该指令只能由控制台执行");
                    return false;
                }
                plugin.silentPingTest();
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "未知的子命令");
                return false;
        }
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender instanceof Player && !sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return false;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "==== LoginSequence 指令帮助 ====");
        sender.sendMessage(ChatColor.GOLD + "/logseq ping" + ChatColor.WHITE + " - 测试服务器连通性");
        sender.sendMessage(ChatColor.GOLD + "/logseq info" + ChatColor.WHITE + " - 请求服务器状态数据");
        sender.sendMessage(ChatColor.GOLD + "/logseq stavie" + ChatColor.WHITE + " - 查看服务器实时状态");
        sender.sendMessage(ChatColor.GOLD + "/logseq help" + ChatColor.WHITE + " - 显示本帮助信息");
    }

    private void handleStatusRequest(CommandSender sender) {
        String status = String.format(
                ChatColor.AQUA + "服务器状态:\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "内存占用: %dMB\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "在线玩家: %d\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "TPS: %.1f",
                pingOnline.getMemUsage(),
                pingOnline.getOnlinePlayers(),
                pingOnline.getServerTPS()
        );
        sender.sendMessage(status);
    }
}
