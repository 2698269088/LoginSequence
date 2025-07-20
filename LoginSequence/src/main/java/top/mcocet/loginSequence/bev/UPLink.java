package top.mcocet.loginSequence.bev;

import java.io.IOException;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;

public class UPLink {
    private final UDPLink udpLink = new UDPLink();
    private final LoginSequence plugin;

    public UPLink(LoginSequence plugin) {
        this.plugin = plugin;
    }

    public void sendLinkPacket(String playerName, String serverName) {
        String message = String.format("LoginSequence-Link %s %s", playerName, serverName);
        try {
            udpLink.send(
                    FillTask.bevip,
                    FillTask.bevport,
                    message
            );
            plugin.getLogger().info(String.format("已发送UDP跳转请求: %s -> %s", playerName, serverName));
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("UDP跳转请求失败: %s", e.getMessage()));
        }
    }
}
