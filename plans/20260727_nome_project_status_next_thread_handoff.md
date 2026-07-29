# Nome 项目状态与新线程交接

生成时间：2026-07-27 01:07 CST（Asia/Shanghai）  
权威状态：Android / Core / Desktop 已双远端发布；iOS 无声通话专项仍有未提交代码；Nome NTF 尚未接入。

> 新线程必须先完整阅读本文件，再做任何编辑、构建、设备安装、服务器操作或 Git 操作。
> 2026-07-24 及更早的 `plans/` 文件是历史证据，不能覆盖本文件记录的当前状态。

## 0. 2026-07-27 macOS ARM64 最新追加（覆盖本文后续旧 Desktop 结论）

- 已在保留混合 dirty tree 的前提下完成当前 macOS ARM64 Core/JNI、`Nome.app` 和 `Nome-6.5.6.dmg` 重建；没有 commit、push、stash、reset 或 clean。
- 修复真实 Core 配置缺陷：Nome SMP 预设必须显式使用 `smp.nome.im:5223`。修复前域名默认端口误连 443 并报 CA 错误；修复后最终 App 包内 Core 的 SMP/XFTP server test 均通过。
- 两套全新隔离资料的实际数据库只包含启用的 Nome operator，以及 `smp.nome.im:5223` / `xftp.nome.im` 两行预设服务器；没有上游默认服务器或上游默认联系人。
- 最终 App 已完成真实双账号连接，两端均为 `ready`；A→B、B→A 消息均在真实接收端生成 `rcv_new`；B→A 文件上传和接收均为 `complete`，最终字节 hash 一致并经 Quick Look 实际渲染。
- 完整退出/重启后连接与消息恢复，chat/agent DB integrity 均为 `ok`。
- Desktop Gradle 当前证据是 38 suites、187 tests、0 failures/errors/skipped；端口修复后的 Nome 配置定向 11 项通过，Nome Core 用户可见版本标签定向 1 项通过。按用户要求没有再重复运行全量 Core。
- DMG checksum、只读挂载、包内唯一 App、Bundle ID、thin arm64、483 个普通资源/0 符号链接、`codesign --verify --deep --strict` 均通过。
- 最新 DMG SHA-256 为 `558256fbc7110ee7607125034b3180f19c6b01e559f326788e3786a910b1fb24`，大小 `245463720` bytes；包内 `libsimplex.dylib` SHA-256 仍为 `4d80ad3e83cb9af38b70020b82aa521837c8466f6553d5f4076110ff6d4a9531`。
- 当前仅为 ad-hoc 签名，Gatekeeper `spctl` 会拒绝；Developer ID、公证、stapling 仍是公开分发阻断。
- TURN 的 3478/5349 可达，5349 TLS 握手和证书校验通过。最终 B App 已通过正式服务器收到 A 的真实加密语音来电，接受、等待和挂断均工作，呼叫记录在两端落库；这只证明信令，不证明双端音视频媒体字节。
- 完整无敏感数据证据：[`20260727_nome_macos_final_runtime_evidence.md`](20260727_nome_macos_final_runtime_evidence.md)。
- Mac 已解锁，最后一次打包的 App 已完成主会话、文件实际打开、联系人详情、安全码、群资料、设置、Nome 服务器、ICE、隐私、外观、关于页和呼叫入口逐页复核。
- 发现并修正频道提示误导文案：实验性频道中继未配置现在显示为“频道功能提示”，不再被理解为 SMP 聊天服务器故障；同时为通话和挂断按钮补全可访问性名称。末轮定向 Desktop 测试 4/4 通过，Core 没有重新编译。
- Desktop 尚未完成的真实门禁是双端 WebRTC 音频/视频媒体、Developer ID/公证、Nome 法律条款、Nome NTF、生产 universal links，以及 TURN 客户端静态凭据的限流/滥用监控/轮换策略。

## 1. 一句话结论

- Android、共享 Core、TURN、Nome 服务器迁移、共用 UI 和 Desktop 已整理为 8 个提交，当前 HEAD 为
  `b44bf456014d4e390a965ede6199ce57133cc2a1`。
- 分支 `codex/nome-android-v656` 已普通推送到 GitHub 和局域网 Gitea；两边实时引用均为同一 HEAD。
- Android 最终 Debug APK 已构建，JVM 单测、Desktop 测试和 Nome Core 定向测试通过。
- 最终 APK 尚未在本次提交后重新安装到真机，也没有完成两机消息、图片、语音、视频、前后台通知的最终矩阵。
- iOS integration 工作树有 7 个未提交的通话音频修复/诊断文件，尚未真机闭环，绝不能宣称已修复。
- `smp.nome.im`、`xftp.nome.im`、`turn.nome.im` 已进入客户端；`ntf.nome.im/ntf-server` 尚未进入代码，
  当前 DNS 也没有 `ntf.nome.im` A 记录。
- 用户当前没有付费 Apple Developer 账号，因此 iOS APNs/NSE/后台通知不能按生产链路完成。

## 2. 工作树与分支拓扑

### 2.1 Android / Core / Desktop 主工作树

```text
路径：/Users/forkman03/project/nome/simplex-chat
分支：codex/nome-android-v656
HEAD：b44bf456014d4e390a965ede6199ce57133cc2a1
GitHub：https://github.com/cxhihilwb123-hash/nome.git
LAN：gitea-lan:forkman/nome.git
官方 origin：https://github.com/simplex-chat/simplex-chat.git（只读边界）
```

2026-07-27 推送后实时核验：

```text
github/codex/nome-android-v656 = b44bf456014d4e390a965ede6199ce57133cc2a1
lan/codex/nome-android-v656    = b44bf456014d4e390a965ede6199ce57133cc2a1
```

没有强推、没有标签推送、没有创建 PR、没有修改或推送官方 `origin`。

### 2.2 iOS 工作树

| 路径 | 分支 / HEAD | 远端状态 | 工作树状态 |
|---|---|---|---|
| `/Users/forkman03/project/nome/simplex-chat-ios` | `codex/nome-ios-v656` / `3f75e9840a5046697e21d6e4c5a87afb59deae09` | GitHub/LAN 均无同名远端分支 | 干净 |
| `/Users/forkman03/project/nome/simplex-chat-ios-integration` | `codex/nome-ios-v656-integration` / `e50f32fcec027029c95d4556cceaf600f8a83edb` | GitHub 与 LAN 均为 `e50f32fc` | **7 个 tracked 文件未提交** |
| `/Users/forkman03/project/nome/simplex-chat-ios-realcore-lab` | `codex/nome-ios-v655-realcore-lab` / `711076b9cde8ecc9be4a0b97bb1ee5f4698caa15` | GitHub 为 `711076b9`，LAN 无此分支 | 干净 |

`integration` 与另外两条 iOS 线已经分叉，不可未经逐提交审计直接 merge/rebase。

另有 3 个登记中的 `/private/tmp` iOS 工作树包含未提交内容，不得自动 prune 或删除：

- `/private/tmp/nome-ios-device-latest-20260725`
- `/private/tmp/nome-personal-team-device-20260723`
- `/private/tmp/nome-personal-team-ui-fix-20260723`

## 3. 本轮已发布的 8 个提交

```text
f46692c99027208edabb36e6e536b9172fa01bbd feat(nome-core): use official Nome routing
313ed319bd87ae768660be7388327957784efd90 feat(nome): migrate clients to official services
999fbab29e12a437e416c6d204ca798d039c6330 fix(calls): route media through Nome TURN
f452f17c7589366d6857b08a1e2266d0dbc29beb fix(android): restore background service after exit
f769481ed55ac9f5ce6668e84ce20de1359a7cbb feat(nome-ui): refine onboarding and navigation
69f2f6a724117a1ca3212c6a55b1f007c4eda4dc feat(desktop): complete Nome desktop experience
80b5d4135b2d46fea80e74d3cd55b1ee83068d54 docs(nome): sync product and architecture contracts
b44bf456014d4e390a965ede6199ce57133cc2a1 fix(build): isolate Android resource packaging
```

## 4. 已完成的产品与代码能力

### 4.1 Nome SMP/XFTP 自动配置

- Core 新建用户默认使用 Nome operator 和 Nome SMP/XFTP。
- Android 在正常网络接收器启动前幂等应用 Nome 服务器配置。
- 新 Core 存在 `OTNome` 时以 Nome operator 为唯一托管路由。
- 旧 Android Core 没有 `OTNome` 时使用 custom group 兼容回退。
- 升级只退休 Nome 管理的旧配置行，保留用户明确添加的自定义服务器。
- 非 Nome 内置运营商路由会被禁用。
- 配置失败会有限重试；失败后保留待重试状态，不伪造成功。

主要入口：

- `src/Simplex/Chat/Operators/Presets.hs`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/NomeServerConfiguration.kt`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt`

### 4.2 域名展示与迁移边界

- 普通只读 UI 对 SMP/XFTP/TURN 只展示域名。
- 用户进入自定义服务器编辑器后仍可查看和编辑完整地址。
- 旧腾讯云 IP 只保留在 legacy 迁移常量和迁移测试中，用于识别旧托管配置。
- 旧 IP 不是新用户默认路由，也不会显示在普通只读页面。
- 与旧主机相同但凭据不同的用户自定义项不会被迁移逻辑误删。

### 4.3 TURN 与通话

- 默认 ICE 已改为 `turn.nome.im`，包含 STUN、TURN UDP、TURN TCP 和 TLS TURN。
- Android/JS 客户端会使用 Nome 默认 ICE；用户明确配置自定义 ICE 时覆盖默认值。
- 客户端包含零配置使用所需的共享访问能力，这些值属于可提取的公共客户端能力，不能作为服务器唯一防滥用边界。
- 服务器上线必须具备限流、滥用监控和凭据轮换；管理员凭据、TLS 私钥、APNs 密钥不得进入客户端或交接文档。

### 4.4 Android 后台服务

- 已修复意外进程退出时把“期望运行状态”错误写成停止的问题。
- 显式停止仍会持久化停止状态；意外退出时可由现有 worker/receiver 恢复前台服务。
- 这不等于已经证明 Vivo/其他 OEM 在强制停止、深度省电和锁屏后的最终到达率。
- Android 最终前后台通知与消息到达仍需真机矩阵验证。

### 4.5 UI 与 Desktop

- 共用首启、联系人/新建连接、服务器、设置、中文文案和 Nome Logo 已整理提交。
- Desktop Nome 首页、首启、主题、托盘、更新策略、设置和资源已经提交并通过 Desktop 测试。
- 本轮已重新生成当前 HEAD 对应的 ARM64 App 和 ad-hoc DMG；2026-07-24 的 DMG 仅为历史产物。新包尚未做 Developer ID 签名、公证和 stapling，不能作为公开分发包。

## 5. 服务器与通知现状

2026-07-27 只读 DNS 检查结果如下；这只证明域名解析，不等于应用层服务完整健康：

| 服务 | 客户端代码 | DNS | 本轮应用层/端口实测 |
|---|---|---|---|
| `smp.nome.im` | 已接入 | 有 A 记录 | Desktop 两账号连接与双向消息真实 E2E 通过 |
| `xftp.nome.im` | 已接入 | 有 A 记录 | Desktop B→A 上传/接收完成、hash 一致并实际打开 |
| `turn.nome.im` | 已接入 | 有 A 记录 | Desktop 真实来电信令、接受和挂断通过；双端媒体及移动端矩阵未完成 |
| `ntf.nome.im` | **未接入** | **无 A 记录** | `ntf-server` 未部署/未验证 |

当前 Core/Terminal 明确没有默认 Nome NTF，规范也不声明默认 NTF。Android 前台服务或 iOS 的 NSE/APNs 代码不等于
Nome `ntf-server` 已部署。

用户当前没有付费 Apple Developer 账号。即使先部署 `ntf-server`，iOS 生产后台通知仍需要可用的 Apple Team、Push
Notifications capability、APNs key/certificate 和匹配的签名配置。任何 APNs 密钥都不得写进仓库、App 或交接文档。

## 6. 最终验证证据

### 6.1 Nome Core

命令：

```text
cabal test simplex-chat-test --offline --test-show-details=direct --test-options='--match Nome'
```

结果：9 examples，0 failures，测试套件 PASS。

### 6.2 Android

最终 HEAD 命令：

```text
./gradlew :android:testDebugUnitTest :android:assembleDebug --offline
```

结果：

- `BUILD SUCCESSFUL`
- 72 actionable tasks
- JVM 单测 54/54，0 failures，0 errors
- Android instrumentation tests 本轮未在真机执行

最终 APK：

```text
路径：apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk
版本：6.5.6 (359)
大小：约 326 MiB
SHA-256：b19aab839d168caa130785cf58158b2d84aa7bcb0964b50d3f8e0f87daea7be8
```

此 APK 在最终提交后尚未重新安装并读回真机版本/哈希。

### 6.3 Desktop

最终 HEAD 命令：

```text
./gradlew :common:desktopTest --offline
```

结果：`BUILD SUCCESSFUL`；完整 Desktop 结果为 38 suites、187 tests、0 failures/errors/skipped。末轮频道提示和通话按钮可访问性修正后，变更范围定向测试 4/4 通过。

Android 与 Desktop 分平台执行均通过。一次把 Android 与 Desktop 混在同一 Gradle invocation 的尝试受本机 Compose 编译内存
上限影响，没有作为验收依据。

### 6.4 Git

- 8 提交范围 `git diff --check` 通过。
- GitHub 和 LAN 远端分支均实时读回为 `b44bf4560`。
- 官方 `origin` 未改变、未推送。

## 7. iOS 无声通话专项的真实状态

继续通话问题时必须使用：

```text
/Users/forkman03/project/nome/simplex-chat-ios-integration
branch: codex/nome-ios-v656-integration
committed HEAD: e50f32fcec027029c95d4556cceaf600f8a83edb
```

该工作树当前有 7 个 tracked 文件未提交，staged 为空：

- `apps/ios/Shared/ContentView.swift`
- `apps/ios/Shared/Views/Call/ActiveCallView.swift`
- `apps/ios/Shared/Views/Call/CallController.swift`
- `apps/ios/Shared/Views/Call/IncomingCallView.swift`
- `apps/ios/Shared/Views/Call/WebRTC.swift`
- `apps/ios/Shared/Views/Call/WebRTCClient.swift`
- `apps/ios/Tests/Tests_iOS.swift`

这些改动用于：

- 等待麦克风权限完成后再建立通话客户端；
- 协调 CallKit 与 WebRTC audio session 激活；
- 本地音频轨和 transceiver 映射兜底；
- 修正扬声器按钮误导状态；
- 记录 SDP、音轨、路由和 RTP inbound/outbound 字节；
- 增加真机通话 UI 自动化路径。

当前用户现象是：特定 iPhone 发起时可能视频可见但无声音，重复发起还可能黑屏；另一方向可能正常。上述 7 文件尚未在
两台 iPhone 上完成最终验证，也没有提交或推送。因此：

- 不得宣称无声通话已经修复；
- 不得只凭扬声器图标判断输出路由；
- 必须用双方 RTP inbound/outbound 音频字节、实际听到声音和视频画面共同验收；
- 在保护这 7 个文件前不得 reset、clean、stash、rebase、merge 或切换覆盖工作树。

## 8. 仍未完成的验收

1. 最终 Android APK 安装到当前设备，并读回版本、包名和设备 APK SHA-256。
2. 两台设备的新用户流程：设名后不返回设置页，直接进入首页。
3. Android↔Android、iPhone↔iPhone、Android↔iPhone 的文字、图片、语音消息双向传输。
4. 接收端最终文件字节与真实打开/完整播放，不能只看消息卡片或服务器对象。
5. 语音/视频通话双向发起，至少连续 3 轮；验证画面、双方实际声音、RTP 字节和第二次呼叫。
6. Android 退后台、锁屏、意外进程退出后的消息到达；显式 force-stop 需单独标记为系统限制场景。
7. iOS 退后台后的通知链路；在付费 Apple Developer/APNs 和 Nome NTF 完成前不能宣称通过。
8. `ntf.nome.im` DNS、TLS、`ntf-server`、客户端配置和 APNs 密钥轮换方案。

## 9. 主工作树保留的脏状态

创建本交接前，`simplex-chat` 暂存区为空，仍保留：

- tracked：`apps/multiplatform/android/src/debug/AndroidManifest.xml`
- tracked：`src/Simplex/Chat/Store/SQLite/Migrations/chat_query_plans.txt`
- 59 个 untracked 入口，包括 `.codex/`、`.gradle-review/`、截图、Android debug/test 探针、旧计划、证据目录、脚本和
  未使用的 `nome_lockup_dark@4x.png`。

这些内容属于历史/本地工作，不得用 `git reset --hard`、`git clean`、批量删除或自动 `git add -A` 处理。本交接文件和新线程提示
本身会新增未跟踪记录；是否提交/推送必须由用户在新线程明确授权。

## 10. 新线程安全执行顺序

1. 完整阅读本文件。
2. 只读核对所有工作树的 branch、HEAD、staged 和 dirty 状态。
3. 实时核对 GitHub/LAN 远端引用；不要依赖旧的本地 remote-tracking ref。
4. 明确本轮只选一个主线：
   - iOS 无声通话真机闭环；或
   - Android 最终 APK 安装与两机 E2E；或
   - Nome NTF/APNs 设计与部署。
5. 若继续 iOS 通话，先保护 integration 的 7 个未提交文件，再构建/安装/采集 RTP 证据。
6. 若设备不在身边，只能做代码审查、构建和模拟器测试，不能宣称真机问题已修复。
7. 任何服务器变更前先只读核对 DNS、证书、服务、端口和备份；不得把管理员/API/APNs/TLS 密钥写入命令、日志、文档或代码。
8. 只有用户再次明确要求时，才提交或推送新的改动；官方 `origin` 始终只读。

## 11. 新线程首条消息建议

直接复制 `plans/20260727_nome_next_thread_prompt.txt` 的内容作为新线程第一条消息。
