package top.mcocet.loginSequenceBC;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPNet {

    private final int port;
    private boolean running = true;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public UDPNet(int port) {
        this.port = port;
    }

    public void start() {
        executor.submit(() -> {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                ProxyServer.getInstance().getLogger().info("UDP 服务已启动，监听端口: " + port);

                byte[] buffer = new byte[1024];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    ProxyServer.getInstance().getLogger().info("收到 UDP 消息: " + message);

                    if (message.startsWith("LoginSequence-Link ")) {
                        String[] parts = message.split(" ");
                        if (parts.length == 3 && parts[0].equals("LoginSequence-Link")) {
                            String playerName = parts[1];
                            String serverName = parts[2];

                            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
                            if (player != null) {
                                ProxyServer.getInstance().getLogger().info("尝试将玩家 " + playerName + " 转移到服务器 " + serverName);
                                player.connect(ProxyServer.getInstance().getServerInfo(serverName));
                            } else {
                                ProxyServer.getInstance().getLogger().warning("未找到在线玩家: " + playerName);
                            }
                        } else {
                            ProxyServer.getInstance().getLogger().warning("无效的 UDP 命令格式: " + message);
                        }
                    }
                }
            } catch (IOException e) {
                ProxyServer.getInstance().getLogger().severe("UDP 监听发生错误");
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }
}
