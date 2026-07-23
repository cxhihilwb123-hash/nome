# Nome iOS 真实设备验证记录

日期：2026-07-23（Asia/Shanghai）

## 结论

状态：**PERSONAL_TEAM_MAIN_APP_PASS_WITH_LIMITS**

本轮从 `codex/nome-ios-v656-integration` 继续，没有重做 Core 集成，也没有修改
Android。为了先用免费 Apple Developer Personal Team 做能完成的真机验证，使用
独立临时 worktree 生成了仅包含主 App 的测试包；临时签名、bundle identifier、
entitlement 和扩展裁剪没有合入产品分支。

真机 build 338 已覆盖安装到同一 Personal Team 测试 bundle 的 build 337 上。
安装、启动、首页与联系人页、现有数据读取、腾讯云服务器配置、双向通信、
冷启动以及前后台切换均通过。

免费账号不能签发当前产品所需的 Push、Notification Service Extension、
Share Extension、App Group 等完整能力，因此通知和分享扩展没有被宣称通过。
相机控制器可以从 Nome 正常打开，但 iPhone 镜像明确阻止 Mac 使用 iPhone
摄像头；真实取景和拍摄需在手机上人工确认。

## 本轮产物

- 源分支：`codex/nome-ios-v656-integration`
- 源 HEAD：`b4f822e55c3f9f363bc974556ec74ad512449b2a`
- 真机测试版本：Nome `6.5.6` build `338`
- 架构：`arm64`
- App 二进制 SHA-256：
  `3948447b1d450b6310cb267a398ca1f583fb696af19c50e2c39c7f7312231614`
- Xcode 结果：`** BUILD SUCCEEDED **`
- 安装方式：同一测试 bundle 覆盖安装，没有先卸载旧版本

三个 UI 源文件从产品 worktree 同步到临时签名 worktree 后做过 SHA-256 对照，
确保真机运行的是当前未提交的 Android 对齐 UI，而不是旧界面。

## 已通过

### 安装与启动

- Personal Team 主 App 自动签名成功。
- build 338 覆盖安装成功，设备回读版本为 `6.5.6 (338)`。
- 解锁设备后 `devicectl` 启动成功。
- 设备进程列表确认 `Nome.app/Nome` 正在运行。

### 首页、联系人和数据保留

- 首页显示新版 28pt 标题、16pt 正文、72pt 会话行、44pt 头像、底部导航和 FAB。
- 联系人页使用与 Android 对齐的标题、搜索和紧凑分组列表。
- 覆盖安装后，原有 ForkMan 会话、群组和私密笔记仍存在。
- 完全关闭 App 后从主屏幕重新启动，原会话和本轮新消息仍能加载。

### 腾讯云服务器

“你的服务器”页面在 build 338 上继续显示并启用：

- 消息服务器：`124.223.71.168`
- 媒体与文件服务器：`124.223.71.168`

后续真实双向消息成功，也证明当前会话的实际网络链路可用。

### 双向通信

- iPhone 向 ForkMan 会话发送测试消息；
- 出站消息显示双勾和发送时间；
- Android 端回复 `338 收到`；
- iPhone 在同一会话显示该入站消息和接收时间。

因此本轮不是只验证本地 UI 或历史数据，而是完成了真实设备的双向通信闭环。

### 钥匙串/本地解密链路

- build 338 覆盖安装没有清空旧 build 337 的应用数据；
- 完全结束进程并冷启动后，App 能继续打开原有会话数据库；
- 刚完成的双向消息也在冷启动后保留。

这证明 Personal Team 测试 bundle 内的本地数据与解密凭据链路可继续使用。它不
等同于生产 `chat.simplex.app` 与完整 App Group/keychain access group 的迁移
证明；生产签名身份仍需付费团队单独验证。

### 前后台切换

- 从聊天页返回 iOS 主屏幕后，Nome 出现在 App 切换器中；
- 从 App 切换器恢复后，仍停留在原聊天上下文；
- 连接状态、会话内容和已发送消息保持正常；
- 随后彻底结束进程并冷启动也通过。

## 受免费账号或镜像限制的项目

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| Personal Team 主 App 构建 | PASS | `6.5.6 (338)`，arm64 |
| 覆盖安装 | PASS | 未卸载 build 337，数据保留 |
| 启动/冷启动 | PASS | 进程运行，冷启动后数据可读 |
| 首页与联系人页 | PASS | 新版 Android 对齐 UI 已在真机确认 |
| 腾讯云服务器 | PASS | 自定义消息/文件服务器继续启用 |
| 双向通信 | PASS | 出站双勾，Android 回复在 iPhone 入站 |
| Personal Team 钥匙串/解密链路 | PASS | 覆盖安装与冷启动后旧数据、新消息可读 |
| 前后台切换 | PASS | 主屏幕、App 切换器、恢复和冷启动均通过 |
| 相机入口/控制器 | PASS | Nome 可打开系统相机控制器 |
| 相机真实取景/拍摄 | MANUAL | iPhone 镜像阻止 Mac 使用手机摄像头 |
| 推送通知/NSE | UNSUPPORTED_FREE_TEAM | Personal Team 无法签发所需能力 |
| 分享扩展 | UNSUPPORTED_FREE_TEAM | 免费验证包未包含 Share Extension |
| 生产 bundle 完整能力 | PENDING_PAID_TEAM | 需付费团队验证兼容签名与扩展 |

## 证据保留

可提交的去敏结论是本文档。包含设备标识、团队信息和完整日志的原始证据保存在
同目录 Git 忽略的 `private/` 子目录，包括：

- build 338 安装、版本回读、启动和进程 JSON/日志；
- build 338 首页与联系人页截图；
- 腾讯云服务器保留截图；
- 出站双勾与 Android 入站回复截图；
- 主屏幕、App 切换器和恢复前台截图；
- 冷启动后数据保留截图；
- iPhone 镜像相机限制提示和系统相机控制器截图。

临时 Personal Team 签名 worktree 和 DerivedData 位于 `/private/tmp`，不会合入
产品分支。

## 完整生产验证的继续条件

使用能够签发主 App、Notification Service Extension、Share Extension、
App Group、keychain access group、Push 和 Associated Domains 的付费 Apple
Development team 后，还需补测：

1. 生产 bundle 覆盖安装和兼容数据/钥匙串迁移；
2. APNs 前台、后台、锁屏和通知扩展处理；
3. 从系统分享面板进入 Share Extension；
4. 手机上真实拍照、确认和发送；
5. 上述完整能力下再次执行双向通信与前后台回归。
