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
    private PingOnline pingOnline; // PingOnline实例引用
    private static final String serverName = PingOnline.getServerName(); // serverName定义

    public CheckingTask(LoginSequence loginSequence) {
        this.loginSequence = loginSequence;
        this.upLink = new UPLink(loginSequence); // 初始化 UPLink
    }

    // 添加设置PingOnline实例的方法
    public void setPingOnline(PingOnline pingOnline) {
        this.pingOnline = pingOnline;
    }

    public void processQueue() {
        if (loginSequence.getQueue().isEmpty()) {
            loginSequence.setIsTransferring(false);
        } else {
            final Player nextPlayer = (Player)loginSequence.getQueue().poll();
            if (nextPlayer != null && nextPlayer.isOnline()) {
                loginSequence.setIsTransferring(true);
                nextPlayer.sendMessage(ChatColor.GREEN + "正在尝试连接服务器...");
                
                // 检查是否启用延迟跳转模式
                if (FillTask.enableDelayTransfer) {
                    // 执行延迟跳转模式
                    handleDelayTransfer(nextPlayer);
                } else {
                    // 原始处理逻辑
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
                }
            } else {
                processQueue();
            }
        }

    }

    /**
     * 处理延迟跳转模式
     * @param player 玩家
     */
    private void handleDelayTransfer(Player player) {
        // 执行配置中的指令
        String command = FillTask.delayTransferCommand.replace("{player}", player.getName());
        if (command.startsWith("/")) {
            command = command.substring(1); // 移除开头的斜杠
        }
        Bukkit.dispatchCommand(player, command);
        // 发送消息给玩家
        player.sendMessage(ChatColor.YELLOW + "正在准备服务器切换...");
        
        // 设置玩家的等待状态
        loginSequence.setPlayerInWaitingPeriod(player.getName(), true);
        
        // 等待指定的时间后，如果玩家仍然在线，则将其重新加入队列
        int waitTicks = FillTask.delayTransferWaitTime * 20; // 将秒转换为游戏刻
        (new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // 玩家仍然在线，将其重新加入队列
                    loginSequence.getQueue().add(player);
                    player.sendMessage(ChatColor.YELLOW + "您未在指定的时间内加入服务器，您已重新加入排队队列");
                    
                    // 重置玩家的等待状态
                    loginSequence.setPlayerInWaitingPeriod(player.getName(), false);
                    
                    // 继续处理队列
                    loginSequence.setIsTransferring(false);
                    loginSequence.processQueue();
                } else {
                    // 玩家已经离开，不需要做任何事情
                    loginSequence.setIsTransferring(false);
                    loginSequence.processQueue();
                    
                    // 重置玩家的等待状态
                    loginSequence.setPlayerInWaitingPeriod(player.getName(), false);
                }
            }
        }).runTaskLater(loginSequence, waitTicks);
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
        if (FillTask.bevel) {
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