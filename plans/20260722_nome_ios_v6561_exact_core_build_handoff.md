# Nome iOS v6.5.6.1 精确 Core 构建交接

更新时间：2026-07-22（Asia/Shanghai）

## 后续状态更新（优先于下文原计划）

同日后续排查找到了官方 Hydra build `1460001` 的预编译 arm64 Swift JSON
Core 6.5.6.1，因此不再需要安装 Nix 或在本机从头构建 Haskell Core。

该官方包使用 GHC 8.10.7，原始 archive 为 Darwin platform 1。device 副本经
官方固定版本 `mac2ios FILE` 转为 IOS platform 2，simulator 副本经
`mac2ios -s FILE` 转为 IOSSIMULATOR platform 7。generic iOS device 构建、
arm64 Simulator 构建以及两台专用 Simulator 的 3+3 双向真实消息均已通过。

当前权威结果与哈希记录：
`plans/evidence/20260722_nome_ios_v6561_build1460001/RESULTS.md`。

下文“安装 Nix 并本机构建”的内容仅保留为原始调查记录和最终回退方案，不再是
当前执行路线。

## 结论

当前 iOS 端已接入官方预编译的 Core 6.5.6.1，并完成 device/simulator 平台
转换、Xcode 双路线构建和两台 Simulator 的 3+3 双向真实消息验证。iOS Core
本地源码构建阻塞已经关闭。

这不是 Android 工作；不得触碰 Android 工作树、Android 测试进程或用户测试
产物。当前剩余边界是真实 iPhone 运行、后台通知矩阵和发布签名，不是 Core
编译或 Simulator 通信。

## 权威入口与起点

- 工作目录：`/Users/forkman03/project/nome/simplex-chat-ios-integration`
- 分支：`codex/nome-ios-v656-integration`
- 本文档编写前起点：`ab37f8b10142084cbb849ede89577f9c92fec9bc`
- GitHub 远端：`github=https://github.com/cxhihilwb123-hash/nome.git`
- 官方只读上游：`origin=https://github.com/simplex-chat/simplex-chat.git`
- Draft PR：<https://github.com/cxhihilwb123-hash/nome/pull/1>
- 官方源码 tag：`v6.5.6`
- 官方源码提交：`59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- Cabal Core 版本：`6.5.6.1`
- iOS `MARKETING_VERSION`：`6.5.6`，build `337`

新任务开始时先执行：

```bash
cd /Users/forkman03/project/nome/simplex-chat-ios-integration
git branch --show-current
git status --short --branch
git log -3 --oneline --decorate
```

预期分支为 `codex/nome-ios-v656-integration`，工作树应为空。若出现新改动，先
识别来源，不要 reset、clean 或 stash。本文档中的起点 SHA 是文档提交前的
checkpoint；以 GitHub Draft PR 分支的最新非合并提交作为实际续跑起点。

## 已通过的验证

- v6.5.5 兼容 Core 已在两台隔离的 arm64 iOS Simulator 上完成 3 轮双向消息：
  Lab -> Peer 3/3、Peer -> Lab 3/3，四个 `.xcresult` 均通过。
- 真实 composer 发送、真实会话持久化、接收端实时等待和精确消息断言均通过。
- 现有自动诊断不会在普通 scheme 运行中发送合成消息；未显式提供 run id 时，
  两项诊断测试会安全跳过。
- 详细的已提交证据：
  `plans/evidence/20260722_nome_ios_real_core_message_diagnostic/RESULTS.md`。
- 两台专用 Simulator 已恢复为 Shutdown：
  - Lab：`539249D8-761C-4DB0-966C-75E083869D91`
  - Peer：`8FB4B1DF-2CA4-4185-B965-45DAF4E16534`
- 普通 iPhone 17 Pro Simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`
  未被本批诊断改变。

当前兼容库名称包含 `6.5.5.0`。它们来自官方 v6.5.5 arm64 Darwin 产物并已
转换为 IOSSIMULATOR 平台元数据；不能把这些结果表述成 v6.5.6.1 精确 Core
通过。

## 官方 v6.5.6.1 产物调查

Hydra 精确 jobset 信息：

- jobset：`simplex-chat-simplex-chat:v6-5-6`
- evaluation：`1834`
- evaluation URL：<https://ci.zw3rk.com/eval/1834>
- full errors：<https://ci.zw3rk.com/eval/1834/errors?full=1>
- flake source 精确指向
  `59fce95d3cd08897b4ef742447b785cf2e56c7ce`

精确 x86_64 iOS Swift JSON 产物已经由官方 Hydra 成功构建：

| 项目 | 值 |
|---|---|
| Hydra build | `1459995` |
| Build URL | <https://ci.zw3rk.com/build/1459995> |
| Job | `x86_64-darwin.x86_64-darwin-ios:lib:simplex-chat` |
| Nix output | `simplex-chat-lib-simplex-chat-6.5.6.1` |
| 文件 | `pkg-ios-x86_64-swift-json.zip` |
| 大小 | `73,592,657` bytes |
| SHA-256 | `19af676dfbb3d953cddff3c405ec4426aeb6302ce125ee93c86b4f7eae543ffa` |
| 下载 | <https://ci.zw3rk.com/build/1459995/download/1/pkg-ios-x86_64-swift-json.zip> |

该 ZIP 已在本机 `/tmp/nome-ios-real-core-v6561-official-x86_64` 独立下载、校验
并解压。其两个 Core archive 是：

```text
libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik.a
libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik-ghc9.6.3.a
```

全部静态库经 `lipo -info` 均为 x86_64。`/tmp` 内容可能被系统清理；若需重新
下载，必须再次核对上表 SHA-256，不能只根据文件名信任产物。

同一 evaluation 中，精确 aarch64 job
`aarch64-darwin.aarch64-darwin-ios:lib:simplex-chat` 没有成功产物，标准下载
端点返回 HTTP 404。full errors 显示它在 Hydra 构建开始前因
`haskell-project-plan-to-nix-pkgs.drv` 的本地 Nix cache copy 失败而终止。
这是官方 CI/evaluation 基础设施失败，不足以证明源码无法在本机构建。

不要用 master jobset 的 arm64 产物代替：已检查的 release-time master 构建
是 7.0.0.1/7.0.0.4/7.0.0.5，不是要求的 6.5.6.1。

## 本机前置条件与当前阻塞

- `/Applications/Xcode.app/Contents/Developer` 可用。
- 不改变全局 `xcode-select`；所有 Xcode/Nix iOS 命令按命令设置
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`。
- 当前本机未安装 Nix，`/nix` 不存在。
- Nix 的系统安装需要用户在 Terminal 中亲自授权；不要处理、记录或请求用户
  密码。官方安装入口：<https://nixos.org/download/>。
- 用户完成安装后可运行：

```bash
curl --proto '=https' --tlsv1.2 -L https://nixos.org/nix/install | sh
```

- 最近检查剩余磁盘约 64 GiB，而
  `scripts/ios/check-real-core-build-env.sh` 对本地 Nix iOS Core 构建建议至少
  80 GiB。先重新检查实际空间；低于建议值只记为风险，不擅自删除任何内容。
- `/tmp` 中存在可再生成的 iOS DerivedData，合计可能超过 10 GiB。只有获得
  用户明确删除授权后才可清理已确认的 DerivedData 目录；不得删除诊断证据、
  `.xcresult`、截图、Simulator profile、联系人或数据库。

## 下一任务执行顺序

### 1. 重新审计环境

```bash
command -v nix
nix --version
test -d /nix && echo /nix-present
df -h /Users/forkman03/project/nome

FULL_XCODE_DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  scripts/ios/check-real-core-build-env.sh --target physical-device

FULL_XCODE_DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  scripts/ios/check-real-core-build-env.sh --target simulator
```

如当前 shell 尚未加载 daemon profile，按 Nix 安装器给出的 profile 路径加载；
不要修改系统级 Xcode 选择，也不要在仓库中保存任何机器凭据。

### 2. 只构建缺失的精确 arm64 产物

官方精确 x86_64 产物已经存在，本机 Simulator 也不能使用 x86_64，因此优先
只运行缺失的 aarch64 target，节省时间和磁盘：

```bash
cd /Users/forkman03/project/nome/simplex-chat-ios-integration
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  nix build \
    -o /tmp/nome-ios-v6561-aarch64-result \
    '.#aarch64-darwin-ios:lib:simplex-chat'
```

若失败，保留完整命令、退出码、Nix drv 和日志路径，先判断是依赖/cache/磁盘
基础设施问题还是源码问题。不得为了绕过构建错误修改协议、数据库、消息状态机
或 Haskell/native Core 业务逻辑。

### 3. 在 `/tmp` 审计和组装，不直接覆盖 Libraries

构建完成后先查看真实输出结构：

```bash
find -L /tmp/nome-ios-v6561-aarch64-result -maxdepth 3 -type f -print
find -L /tmp/nome-ios-v6561-aarch64-result -maxdepth 3 -type f -name '*.a' \
  -exec lipo -info '{}' ';'
```

必须同时确认：

- Core 文件名包含 `libHSsimplex-chat-6.5.6.1-`；
- 所有必要静态库架构为 arm64；
- archive 不含 preview-core markers；
- 记录 Nix output path、产物文件列表、每个文件 SHA-256 和总大小；
- 保留未经转换的 aarch64 Darwin 副本，供 physical-device 路线使用；
- 在另一个 staging 目录中把副本转换为 IOSSIMULATOR 平台元数据，供本机
  arm64 Simulator 使用。

转换工具的实际参数由 `--help` 确认：

```bash
python3 scripts/ios/convert-darwin-archive-to-ios-simulator.py --help
```

逐个 archive 转换时使用完整 Xcode developer dir、当前 Simulator SDK 版本和
项目最低部署版本。不要对保留的 device 原件原地转换。官方 x86_64 ZIP 只作为
版本/hash 对照和可重复来源，不安装到本机 arm64 Simulator。

### 4. 审计通过后才安装忽略目录

- device 原件放入被 Git 忽略的 `apps/ios/Libraries/ios`；
- arm64 IOSSIMULATOR 转换副本放入被 Git 忽略的
  `apps/ios/Libraries/sim`；
- 安装前记录旧库清单和 SHA-256，以便不丢失回退证据；
- 不把大型 Core archive 提交到 Git；
- Core 文件名/hash 改变后运行：

```bash
scripts/ios/sync-real-core-xcode-project.sh
scripts/ios/sync-real-core-xcode-project.sh --check
scripts/ios/check-real-core.sh --target simulator
scripts/ios/check-real-core.sh --target physical-device
```

`sync-real-core-xcode-project.sh` 可能修改已跟踪的
`apps/ios/SimpleX.xcodeproj/project.pbxproj`。只有精确版本、架构、平台元数据和
hash 审计均通过后才允许产生这项变更。

### 5. 构建与回归

1. 使用 generic iOS destination 完成 signed device build。
2. 只启动两台专用 Simulator，运行精确 Core 的 3+3 双向诊断：

```bash
scripts/ios/run-real-core-message-diagnostic.sh \
  --lab-simulator 539249D8-761C-4DB0-966C-75E083869D91 \
  --peer-simulator 8FB4B1DF-2CA4-4185-B965-45DAF4E16534 \
  --rounds 3 \
  --run-id v6561-$(date +%Y%m%d%H%M%S)
```

3. 验证 6 次本地发送、6 次实时接收、四个 `.xcresult` 和最终截图。
4. 专门重复 background/foreground 或陈旧连接恢复路径，确认 v6.5.5 兼容
   Core 曾出现的 stale-connection caveat 是否仍可复现。
5. 结束时将两台专用 Simulator 恢复为开始前状态；不影响普通 iPhone 17 Pro
   Simulator，也不影响正在运行的 Android 测试。
6. 把可公开的结论和去敏摘要写入新的 `plans/evidence/` 文档。原始聊天记录、
   邀请链接、二维码 payload、数据库和本机 profile 保持 Git 外。

### 6. 真机边界

当前状态只证明 Xcode 有 generic iOS destination、自动签名配置和 development
team `5NN7GUYB6T`；上游兼容阶段 bundle id 仍为 `chat.simplex.app`。当前没有已
连接且受信任的实体 iPhone/iPad，所以不能声称真机安装、启动、通知、相机、
分享扩展或后台恢复已通过。

只有用户连接、解锁并信任设备后，才能运行 physical-device smoke。不要为完成
任务而擅自改 bundle id、App Group、钥匙串组或签名身份。

## 强制安全边界

- iOS-only；不修改 `/Users/forkman03/project/nome/simplex-chat` Android 工作树。
- 不 reset、clean、stash，不覆盖用户未提交文件。
- 不向官方 `origin` push；只允许普通 push 到 `github`，禁止 force-push。
- 不修改全局 `xcode-select`。
- 不修改 Haskell/native Core、`Core.kt`、协议、数据库或消息状态机来绕过构建。
- 不删除 Simulator 数据、联系人、聊天数据库或诊断证据。
- 不把邀请链接、二维码 payload、密钥、token、密码或本机私有路径内容提交。
- 没有真实连接设备证据时，不宣称 physical-device 或 TestFlight ready。
- 不把 Draft PR 改为 Ready，不合并 PR。

## 完成标准

只有同时满足以下条件，才可把“v6.5.6.1 精确 Core 批次”标记为完成：

- 本机精确 aarch64 Core 构建成功，版本、架构、平台元数据和 SHA-256 有记录；
- device 原件与 arm64 Simulator 转换副本分开保留并通过 preflight；
- generic signed iOS build 通过；
- 两台专用 Simulator 使用精确 Core 完成 3+3 双向真实消息；
- stale connection/background-foreground 边界有复测结论；
- 专用 Simulator 恢复原状态，Android 工作树和测试未受影响；
- 新证据文档、必要的小型项目引用变更通过审查并独立提交；
- 普通 push 到 GitHub 分支成功，Draft PR 保持 Draft 并更新证据；
- 真机状态被如实标记为“未验证”或附上已连接设备的真实结果。

## 新任务首句

```text
继续 Nome iOS v6.5.6.1 精确 Core 构建。先完整读取
plans/20260722_nome_ios_v6561_exact_core_build_handoff.md，并从其中记录的
GitHub Draft PR 最新提交继续。先验证 Nix、/nix 和磁盘空间，再只构建缺失的
aarch64-darwin-ios:lib:simplex-chat；先在 /tmp 审计版本、架构、平台元数据和
SHA-256，未经审计不要覆盖 apps/ios/Libraries。保持 iOS-only，不修改 Android
工作树，不 reset/clean/stash，不改全局 xcode-select，不向 origin push。
```
