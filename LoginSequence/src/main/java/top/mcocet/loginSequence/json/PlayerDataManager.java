package top.mcocet.loginSequence.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerDataManager {
    private static final String JSON_PATH = "plugins/LoginSequence/UserData.json";
    private static final Gson gson = new Gson();
    private static List<PlayerInfo> playerData = new ArrayList<>();

    public static synchronized void load() throws IOException {
        Path path = Paths.get(JSON_PATH);
        if (!Files.exists(path)) {
            save(); // 创建空文件
            return;
        }

        // 修改：逐行读取JSON文件
        playerData = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        PlayerInfo info = gson.fromJson(line, PlayerInfo.class);
                        playerData.add(info);
                    } catch (Exception e) {
                        // 处理格式错误的行
                        System.err.println("解析JSON行失败: " + line);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            // 增强异常处理：尝试回退到旧格式加载
            try (Reader fallbackReader = Files.newBufferedReader(path)) {
                playerData = gson.fromJson(fallbackReader, new TypeToken<List<PlayerInfo>>(){}.getType());
                // 成功加载后立即转换为新格式
                save();
            } catch (Exception ex) {
                throw new IOException("无法解析JSON文件", ex);
            }
        }
    }

    public static synchronized void save() throws IOException {
        Path path = Paths.get(JSON_PATH);
        Files.createDirectories(path.getParent());

        // 修改：每行写入一个玩家JSON对象
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (PlayerInfo info : playerData) {
                String jsonLine = gson.toJson(info);
                writer.write(jsonLine);
                writer.newLine(); // 添加换行符
            }
        }
    }

    public static synchronized void addPlayer(PlayerInfo info) {
        playerData.add(info);
    }

    public static synchronized Optional<PlayerInfo> getPlayer(String uuid) {
        return playerData.stream().filter(p -> p.getUuid().equals(uuid)).findFirst();
    }

    public static synchronized List<PlayerInfo> getAllPlayers() {
        return new ArrayList<>(playerData);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static synchronized void asyncSave() {
        executor.submit(() -> {
            try {
                save();
            } catch (IOException e) {
                // 处理异常
                e.printStackTrace();
            }
            return null;
        });
    }
}
