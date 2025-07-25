# 🛡️ LoginSequence 插件组

一套专业的 Minecraft 登录管理解决方案，提供安全登录流程、跨服务器监控和优化跳转功能。

## 📦 插件组成

### 🧩 主插件：LoginSequence
**核心功能**：
- ✅ 自动对加入的玩家进行排队处理（基础框架）
- ✅ 远程服务器在线状态检测
- 🛡️ 玩家数据安全存储（扩展功能）
- 🛡️ 安全登录系统（扩展功能）
- 🔒 密码登录功能（*待实现*）
- 📊 远程服务器状态监控
- 📊 服务器过载检测
- ⚠️ 重要：启用详细的玩家数据记录功能需要您同意pi_EULA协议。关于协议内容，可以查看插件的配置文件目录，或在此存储库中查看。

### 🔌 子插件：LoginSequenceOnline
**核心功能**：
- 👥 实时玩家在线状态反馈
- 📈 服务器运行数据提供
- 🔑 登录验证支持  
**依赖**：必须与主插件同时使用

### 🌉 BC插件：LoginSequenceBC
**核心功能**：
- ⚡ 优化跨服务器跳转
- 🛡️ 提高传输稳定性
- ⏱️ 减少连接延迟  
**依赖**：必须与主插件同时使用

---

## ⚠️ 重要注意事项

```diff
- Velocity 兼容性警告 -
! 当前版本暂未适配 Velocity 服务端
! 请在 LoginSequence 配置中关闭 BC/Velocity 跳转功能
! 完整 Velocity 支持将在未来版本中实现
- BC 兼容性警告 -
! 目前LoginSequenceBC插件仍在测试阶段，不建议您立刻使用
