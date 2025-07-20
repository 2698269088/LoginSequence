package top.mcocet.loginSequenceBC;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

public final class LoginSequenceBC extends Plugin {
    private FillTask fillTask;
    private UDPNet udpNet;

    @Override
    public void onEnable() {
        // 初始化配置文件
        fillTask = new FillTask(this);
        // 启动 UDP 网络监听
        int port = fillTask.port;
        udpNet = new UDPNet(port);
        udpNet.start();

        printLogo();
    }

    @Override
    public void onDisable() {
        // 插件关闭逻辑
        // 停止 UDP 服务
        if (udpNet != null) {
            udpNet.stop();
        }
    }

    private void printLogo() {
        CommandSender sender = ProxyServer.getInstance().getConsole();
        sender.sendMessage(ChatColor.AQUA + "    " + ChatColor.BLUE + " __ " + ChatColor.GOLD + " __");
        sender.sendMessage(ChatColor.AQUA + "|   " + ChatColor.BLUE + "(__ " + ChatColor.GOLD + "|__)");
        sender.sendMessage(ChatColor.AQUA + "|___" + ChatColor.BLUE + " __)" + ChatColor.GOLD + "|__)" + ChatColor.GREEN + "    LoginSequenceBC v1.0");
        sender.sendMessage("");
    }

    private void sayLog(String s) {
        ProxyServer.getInstance().getConsole().sendMessage(s);
    }
}
