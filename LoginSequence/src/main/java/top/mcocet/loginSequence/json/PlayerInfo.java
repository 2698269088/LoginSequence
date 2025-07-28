package top.mcocet.loginSequence.json;

import top.mcocet.loginSequence.FillTask;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

public class PlayerInfo {
    private String playerName;
    private String uuid;
    private String firstJoinTime = "";
    private String lastJoinTime = "";
    private String lastLoginTime = "";
    private String firstLoginIP = "";
    private String lastLoginIP = "";
    private boolean registered = false;
    private String password = "None";

    private int failedAttempts = 0; // 失败次数
    private long lastFailedAttemptTime = 0; // 最后一次失败后锁定的时间

    // 更新最后加入时间（带piEula检查）
    public void updateLastJoinTime() {
        if (FillTask.piEula) { // 仅当piEula启用时更新时间
            this.lastJoinTime = Instant.now().toString();
        }
    }

    // 更新登录IP
    public void updateLoginIP(String ip) {
        if (FillTask.piEula) {
            if (firstLoginIP.isEmpty()) {
                this.firstLoginIP = ip;
            }
            this.lastLoginIP = ip;
        }
    }

    // 设置初始时间
    public PlayerInfo() {
        if (FillTask.piEula) {
            String currentTime = Instant.now().toString();
            this.firstJoinTime = currentTime;
            this.lastJoinTime = currentTime;
        }
    }

    // 密码加密
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }


    // Getters and Setters
    public String getFirstLoginIP() { return firstLoginIP; }
    public void setFirstLoginIP(String firstLoginIP) { this.firstLoginIP = firstLoginIP; }

    public String getLastLoginIP() { return lastLoginIP; }
    public void setLastLoginIP(String lastLoginIP) { this.lastLoginIP = lastLoginIP; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getFirstJoinTime() { return firstJoinTime; }
    public void setFirstJoinTime(String firstJoinTime) { this.firstJoinTime = firstJoinTime; }

    public String getLastJoinTime() { return lastJoinTime; }
    public void setLastJoinTime(String lastJoinTime) { this.lastJoinTime = lastJoinTime; }

    public String getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(String lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = sha256(password); }

    public int getFailedAttempts(){ return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public long getLastFailedAttemptTime(){ return lastFailedAttemptTime;}
    public void setLastFailedAttemptTime(long lastFailedAttemptTime) { this.lastFailedAttemptTime = lastFailedAttemptTime; }

    public boolean verifyPassword(String inputPassword) { return this.password.equals(sha256(inputPassword)); }
    public void increaseFailedAttempts() {
        this.failedAttempts++;
        this.lastFailedAttemptTime = System.currentTimeMillis();
    }
}
