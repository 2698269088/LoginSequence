package top.mcocet.loginSequence.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import top.mcocet.loginSequence.FillTask;
import top.mcocet.loginSequence.LoginSequence;

public class PlayerAuthListener implements Listener {
    
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().substring(1); // 移除开头的 "/"
        
        // 如果玩家已通过验证，允许所有命令
        if (!LoginSequence.WaitLogin.contains(player)) {
            return;
        }
        
        // 允许登录和注册相关命令
        if (message.startsWith("l ") || message.startsWith("login ") || 
            message.startsWith("log ") || message.startsWith("reg ") || 
            message.startsWith("register ") || message.startsWith("unreg ") ||
            message.startsWith("unregister ") || message.startsWith("changepasswd ") ||
            message.startsWith("cp ")) {
            return;
        }
        
        // 阻止其他命令
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "请先登录后再使用命令!");
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家正在等待登录，阻止聊天
        if (LoginSequence.WaitLogin.contains(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "请先登录后再进行聊天!");
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家正在等待登录，限制移动
        if (LoginSequence.WaitLogin.contains(player)) {
            if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家正在等待登录，阻止交互
        if (LoginSequence.WaitLogin.contains(player)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家正在等待登录，阻止与实体交互
        if (LoginSequence.WaitLogin.contains(player)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        
        // 如果玩家正在等待登录，阻止打开背包
        if (LoginSequence.WaitLogin.contains(player)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家正在等待登录，阻止丢弃物品
        if (LoginSequence.WaitLogin.contains(player)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 玩家离开时从等待列表中移除
        LoginSequence.WaitLogin.remove(player);
    }
}