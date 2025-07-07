private String firstLoginIp = "0.0.0.0";
private String lastLoginIp = "0.0.0.0";

public String getFirstLoginIp() {
    return firstLoginIp;
}

public void setFirstLoginIp(String firstLoginIp) {
    this.firstLoginIp = firstLoginIp;
}

public String getLastLoginIp() {
    return lastLoginIp;
}

public void setLastLoginIp(String lastLoginIp) {
    this.lastLoginIp = lastLoginIp;
}

// 新增方法同时更新最后登录时间和IP
public void updateLastLogin(String ip) {
    this.lastLoginTime = Instant.now().toString();
    this.lastLoginIp = ip;
}