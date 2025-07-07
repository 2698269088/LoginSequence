// 确保配置读取包含pi_eula
public static void readConfig(JavaPlugin plugin) {
    YamlConfiguration yaml = new YamlConfiguration();

    try {
        File configFile = new File(CONFIG_PATH);
        yaml.load(configFile);
        port = yaml.getInt("Port");
        ip = yaml.getString("ip");
        pionli = yaml.getBoolean("pionli");
        server = yaml.getString("server");
        sl = yaml.getBoolean("sl");
        pwl = yaml.getBoolean("pwl");
        piEula = yaml.getBoolean("pi_eula");  // 正确读取配置值
    } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "配置文件加载异常", e);
    }
}