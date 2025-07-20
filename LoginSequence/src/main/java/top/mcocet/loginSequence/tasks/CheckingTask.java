package top.mcocet.loginSequence.tasks;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;
import top.mcocet.loginSequence.bev.UPLink;
import top.mcocet.loginSequence.tasks.PingOnline;

public class CheckingTask {
    private final UPLink upLink;

    private static final String SERVER_CONNECT_COMMAND = "Connect";
    private final LoginSequence loginSequence;
    private final PingOnline pingOnline; // 新增PingOnline实例引用
    private static final String serverName = PingOnline.getServerName(); // serverName定义

    public CheckingTask(LoginSequence loginSequence) {
        this.loginSequence = loginSequence;
        this.pingOnline = new PingOnline(loginSequence); // 初始化PingOnline实例并传递插件引用
        this.upLink = new UPLink(loginSequence); // 初始化 UPLink
    }

    public void processQueue() {
        if (loginSequence.getQueue().isEmpty()) {
            loginSequence.setIsTransferring(false);
        } else {
            final Player nextPlayer = (Player)loginSequence.getQueue().poll();
            if (nextPlayer != null && nextPlayer.isOnline()) {
                loginSequence.setIsTransferring(true);
                nextPlayer.sendMessage(ChatColor.GREEN + "正在尝试连接服务器...");
                (new BukkitRunnable() {
                    int attempts = 0;
                    int connectCounter = 0;
                    public void run() {
                        // 检查服务器是否过载或离线
                        if (!pingOnline.isServerOverloaded() && PingOnline.serverOnlineInfo) {
                            if (nextPlayer.isOnline()) {
                                connectCounter++;
                                if (pingOnline.isConnectivityTestEnabled()) {
                                    pingOnline.sendPlayerPacket(nextPlayer.getName());
                                }
                                CheckingTask.this.sendPlayerToSC(nextPlayer);
                                int quantity = attempts + 1;
                                nextPlayer.sendActionBar(ChatColor.AQUA + "正在尝试连接服务器（第 " + quantity + " 次）...");
                                loginSequence.getLogger().log(Level.INFO, "玩家 {0} 正在尝试连接服务器: {1}次", new Object[]{nextPlayer.getName(), quantity});
                                // 当连接次数超过10次时的处理
                                if (connectCounter > 10) {
                                    this.cancel();
                                    loginSequence.setIsTransferring(false);
                                    loginSequence.getQueue().add(nextPlayer);
                                    nextPlayer.sendMessage(ChatColor.YELLOW + "连接次数已超过限制，重新加入排队队列");
                                    loginSequence.processQueue();

                                    // 重置玩家状态效果
                                    nextPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
                                    nextPlayer.removePotionEffect(PotionEffectType.INVISIBILITY);
                                    nextPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                                    nextPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                                    nextPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));

                                    return;
                                }
                                if (quantity <= 1) {
                                    nextPlayer.sendTitle("", "", 0, 0, 0);
                                    nextPlayer.sendActionBar("");
                                }
                                attempts++;
                            } else {
                                CheckingTask.this.loginSequence.setIsTransferring(false);
                                CheckingTask.this.loginSequence.processQueue();
                                cancel();
                            }
                        }
                        loginSequence.silentPingTest();
                    }
                }).runTaskTimer(loginSequence, 0L, 100L);
            } else {
                processQueue();
            }
        }

    }

    public void sendPlayerToSC(Player player) {
        // 更新远程服务器信息
        if (pingOnline.isStaServerInfoGet()){
            pingOnline.sendInfoRequest(player);
        }
        // 检查服务器是否过载
        if (pingOnline.isServerOverloaded()) {
            player.sendMessage(ChatColor.RED + "服务器负载过高，请稍后再试");
            return; // 阻止转移
        }
        if (FillTask.bevel) { // 新增的条件判断
            // 使用 UDP 方式跳转
            String serverName = PingOnline.getServerName();
            upLink.sendLinkPacket(player.getName(), serverName);
        }else{
            try (ByteArrayOutputStream b = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(b)) {
                out.writeUTF(SERVER_CONNECT_COMMAND);
                out.writeUTF(serverName);
                player.sendPluginMessage(loginSequence, "BungeeCord", b.toByteArray());
            } catch (IOException e) {
                loginSequence.getLogger().log(Level.SEVERE, "数据流操作异常", e);
            } catch (Exception e) {
                String errorMsg = String.format("玩家 %s 无法连接服务器: %s", player.getName(), e.getMessage());
                player.sendMessage(ChatColor.AQUA + "[Server]: " + ChatColor.RED + "无法连接到服务器，请稍后再试。");
                loginSequence.getLogger().log(Level.WARNING, errorMsg, e);
            }
        }
    }

    public void notifyQueuePositions() {
        int position = 1;

        for(Player player : loginSequence.getQueue()) {
            if (player.isOnline()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "等待连接服务器，当前排队位置：" + ChatColor.AQUA + position);
                loginSequence.silentPingTest();
                position++;
            }
        }

    }
}
