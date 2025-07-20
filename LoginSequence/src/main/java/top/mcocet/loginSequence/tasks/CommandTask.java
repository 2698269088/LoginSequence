package top.mcocet.loginSequence.tasks;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.potion.PotionEffectType;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;
import top.mcocet.loginSequence.json.PlayerDataManager;
import top.mcocet.loginSequence.json.PlayerInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

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

            case "list":
                if (!checkPermission(sender, "logseq.list")) return true;
                handleListCommand(sender);
                return true;

            case "link":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    // 直接连接子服务器
                    try (ByteArrayOutputStream b = new ByteArrayOutputStream();
                         DataOutputStream out = new DataOutputStream(b)) {
                        out.writeUTF("Connect");
                        out.writeUTF(FillTask.server);
                        player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "数据流操作异常", e);
                        player.sendMessage(ChatColor.AQUA + "[Server]: " + ChatColor.RED + "无法连接到服务器，请稍后再试。");
                    }
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行");
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
        sender.sendMessage(ChatColor.GOLD + "/logseq help" + ChatColor.WHITE + " - 显示帮助信息");
        sender.sendMessage(ChatColor.GOLD + "/logseq ping" + ChatColor.WHITE + " - 测试服务器连通性");
        sender.sendMessage(ChatColor.GOLD + "/logseq info" + ChatColor.WHITE + " - 请求服务器状态数据");
        sender.sendMessage(ChatColor.GOLD + "/logseq stavie" + ChatColor.WHITE + " - 查看服务器实时状态");
        sender.sendMessage(ChatColor.GOLD + "/logseq list" + ChatColor.WHITE + " - 查看玩家数据列表");
    }

    private void handleStatusRequest(CommandSender sender) {
        String status = String.format(
                ChatColor.AQUA + "服务器状态:\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "内存占用: %dMB\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "在线玩家: %d/%d (%.1f%%)\n" +
                        ChatColor.WHITE+"[LoginSequence Info] " + ChatColor.GREEN + "TPS: %.1f",
                pingOnline.getMemUsage(),
                pingOnline.getOnlinePlayers(),
                pingOnline.getMaxQuantity(),
                pingOnline.getQuantityPercentage(),
                pingOnline.getServerTPS()
        );
        sender.sendMessage(status);
    }
    private void handleListCommand(CommandSender sender) {
        List<PlayerInfo> players = PlayerDataManager.getAllPlayers();

        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有玩家数据");
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "====== 玩家数据列表 ======");
        sender.sendMessage(ChatColor.AQUA + "总玩家数: " + players.size());
        for (PlayerInfo info : players) {
            sender.sendMessage(String.format(
                    ChatColor.WHITE + "名称: %s\n" +
                    ChatColor.GOLD + "=================================\n" +
                    ChatColor.YELLOW + " UUID: %s \n 首次加入时间: %s \n 最后加入时间: %s \n 首次加入IP: %s \n 最后加入IP: %s \n %s",
                    info.getPlayerName(),
                    info.getUuid(),
                    info.getFirstJoinTime().isEmpty() ? "未记录" : info.getFirstJoinTime(),
                    info.getLastJoinTime().isEmpty() ? "未记录" : info.getLastJoinTime(),
                    info.getFirstLoginIP().isEmpty() ? "未记录" : info.getFirstLoginIP(),
                    info.getLastLoginIP().isEmpty() ? "未记录" : info.getLastLoginIP(),
                    info.isRegistered() ? "已注册" : "未注册"
            ));
        }
    }
}
