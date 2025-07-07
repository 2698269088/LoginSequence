

// 修改玩家加入事件处理部分
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {

    // 新增：根据pi_eula配置决定是否存储玩家数据
    if (FillTask.piEula) {  // 使用FillTask的静态变量
        Player player = event.getPlayer();
        String playerName = player.getName();
        String uuid = player.getUniqueId().toString();
        String playerIp = player.getAddress().getAddress().getHostAddress();

        // 检查是否已存在该玩家数据
        Optional<PlayerInfo> existingPlayer = PlayerDataManager.getPlayer(uuid);

        if (!existingPlayer.isPresent()) {
            // 创建新玩家数据
            PlayerInfo info = new PlayerInfo();
            info.setPlayerName(playerName);
            info.setUuid(uuid);
            info.setFirstJoinTime(Instant.now().toString());
            info.setLastJoinTime(Instant.now().toString());
            info.setFirstLoginIp(playerIp);
            info.setLastLoginIp(playerIp);
            info.setLastLoginTime(Instant.now().toString());

            // 添加到数据管理器
            PlayerDataManager.addPlayer(info);

            // 异步持久化存储
            PlayerDataManager.asyncSave();
            LoginSequence.this.getLogger().info("已异步存储新玩家数据: " + playerName);
        } else {
            // 更新最后加入时间和IP
            PlayerInfo info = existingPlayer.get();
            info.updateLastJoinTime();
            info.updateLastLogin(playerIp);  // 同时更新时间和IP

            // 异步持久化存储
            PlayerDataManager.asyncSave();
            LoginSequence.this.getLogger().info("已异步更新玩家最后登录信息: " + playerName);
        }
    } else {
    }
}
