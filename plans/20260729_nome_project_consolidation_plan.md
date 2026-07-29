# Nome 整个项目整理与主线收口计划

日期：2026-07-29（Asia/Shanghai）  
状态：待执行  
适用根目录：`/Users/forkman03/project/nome`  
执行原则：先冻结证据，后建立备份；先在新工作树整合，后验证发布；最后且经用户明确授权才清理。

## 1. 目标

将当前 Nome 项目整理为可追踪、可恢复、可重复构建的正式工程，最终形成：

1. 一条 Android/Core/Desktop 权威主线。
2. 一条 iOS 权威主线。
3. 一条 Website/邀请码控制面权威主线。
4. 邀请码、取消通话同步、频道功能全部进入对应 Git 主线，不再依赖 `/private/tmp`。
5. 每个安装包都能映射到 Git commit、构建配置、包哈希、设备安装和功能验证记录。
6. 历史证据、真机备份、构建产物与源码分区管理。
7. 清理可再生缓存，但不删除唯一源码、真机数据、发布产物或仍未整合的证据。

## 2. 当前权威事实（2026-07-29 只读审计）

### 2.1 仓库边界

- `/Users/forkman03/project/nome/simplex-chat` 是 Android、Core、Desktop、iOS 共用 Git 仓库。
- `simplex-chat-ios`、`simplex-chat-ios-integration`、`simplex-chat-ios-realcore-lab` 是该仓库的 linked worktree，不是独立 Git 仓库。
- `/Users/forkman03/project/nome/website` 是独立 Git 仓库，负责官网、后台、邀请码控制面和发布下载。
- 原交接已明确要求保留混合 dirty tree，且 iOS 分叉不可未经审计直接 merge/rebase：
  `plans/20260727_nome_project_status_next_thread_handoff.md:60-75`、`:209-239`。

### 2.2 当前 Git 基线

| 责任线 | 当前基线 | 状态 |
|---|---|---|
| Android/Core/Desktop | `codex/nome-android-v656` / `33b97d66155c` | 比 GitHub/LAN `b44bf4560` 多 2 个本地提交；18 个 tracked 修改；staged 为空 |
| Android 邀请码 | `codex/nome-cross-platform-activation-android` / `ca6cf8e30` | 本地提交，无远端分支 |
| Android 取消通话 | `codex/nome-call-reject-android-20260728` / `46f2b5bf3` | 已包含 Android 邀请码提交；本地 clean worktree，无远端分支 |
| iOS integration | `codex/nome-ios-v656-integration` / `e50f32fcec02` | 22 个 tracked 修改，staged 为空，另有未跟踪 build records |
| iOS 邀请码 | `codex/nome-cross-platform-activation-ios` / `46cc76326` | 本地提交，无远端分支 |
| iOS 取消通话 | `codex/nome-call-reject-sync-20260728` / `5dbf8ced6` | 已包含 iOS 邀请码提交；本地 clean worktree，无远端分支 |
| Website | `main` / `105d69c` | clean；GitHub/LAN 停在 `d97001e`；Sites 停在 `c37be37` |

邀请码分支、验证边界和未发布状态记录在：
`/Users/forkman03/project/nome/activation-module-implementation-record.md:15-22`、`:139-164`。

### 2.3 当前发布漂移

- iOS build 374 记录了频道 UI、中文文案和真机安装，但没有记录源码 commit：
  `/Users/forkman03/project/nome/simplex-chat-ios-integration/plans/builds/20260728_nome_ios_6.5.6_build374.md:1-42`。
- build 374 使用的 `/private/tmp/nome-ios-relay-device-20260728.eFdlZP` 没有 `.git`。
- 该临时源码不存在 `NomeActivation.swift` 和 `NomeActivationPolicy.swift`，只能作为差异证据，禁止继续作为正式构建基线。
- 现有 build record 规范只要求版本、编号、日期、配置、标识符、改动和验证，尚未强制 commit/dirty 状态/包哈希：
  `/Users/forkman03/project/nome/simplex-chat-ios-integration/plans/builds/README.md:1-15`。

### 2.4 当前文件与磁盘状态

- `simplex-chat` 约 26 GB，主要是 Gradle、Android/Common/Desktop build、AVD 和本地构建缓存。
- 约 31,073 个 untracked 文件中，超过 30,000 个属于 `.gradle-review`、`.gradle-tmp` 等可再生缓存。
- 当前 `apps/multiplatform/.gitignore` 已忽略标准 build 和 `.nome-local`，但未覆盖 `.gradle-review`、`.gradle-tmp`：
  `apps/multiplatform/.gitignore:1-23`。
- `/private/tmp/nome-*` 约 13 GB，混有 clean worktree、无 Git 临时源码、DerivedData、构建目录和截图证据。
- `device-backups` 约 345 MB，包含真实手机容器和数据库，只能作为敏感备份处理。
- iOS `Local.xcconfig` 已被忽略，秘密配置不得进入 Git：
  `/Users/forkman03/project/nome/simplex-chat-ios-integration/apps/ios/.gitignore:73-75`。

## 3. 不在本次整理中做的事情

1. 不重写 SimpleX Core、协议或数据库架构。
2. 不改变邀请码产品规则、线上策略或生产数据库。
3. 不自动向官方 `origin` 推送。
4. 不在未授权情况下向 GitHub、LAN、Sites、App Store 或 TestFlight 发布。
5. 不把全量历史截图和缓存提交进 Git。
6. 不以“目录变整齐”为理由批量移动数百个历史文件并制造无意义 diff。
7. 不在整理阶段顺带开发新的产品功能。

## 4. 全程禁止动作

在所有保护与验收门禁完成前，禁止：

- `git reset --hard`
- `git clean`
- `git stash`
- `git add -A`
- 在现有 dirty worktree 上直接 rebase/merge
- 使用 `-X ours`、`-X theirs` 或整目录覆盖解决语义冲突
- 自动执行 `git worktree prune`
- 删除 `/private/tmp/nome-*`
- 删除 AVD、DerivedData、Gradle/Cabal/Node 缓存
- 删除、移动或覆盖 `device-backups`
- 把 `Local.xcconfig`、密钥、完整邀请码或服务器秘密写入 Git、日志或计划文件
- 再次从无 Git 的 build 374 临时源码打包

## 5. 目标结构

### 5.1 权威代码线

| 范围 | 目标分支 | 基线策略 |
|---|---|---|
| Android/Core/Desktop | `codex/nome-v656-consolidated-android` | 从 `33b97d661` 建立新 clean worktree，再整合 `46f2b5bf3` 和当前 dirty 变更 |
| iOS | `codex/nome-v656-consolidated-ios` | 从 `5dbf8ced6` 建立新 clean worktree；该提交已包含 `e50`、iOS 邀请码和取消通话修复 |
| Website/Control plane | `main` | 保持 `105d69c` clean 基线，核对后再决定 GitHub/LAN/Sites 同步 |

### 5.2 项目资料结构

不立即批量移动历史文件。先增加索引并冻结旧目录，新资料遵循：

```text
plans/
  current/          # 唯一当前状态、发布矩阵、下一步
  releases/         # 每个正式/测试 build 的可复现记录
  archive/          # 后续分批归档旧计划；不在首轮批量移动
  README.md         # 权威文档索引

/Users/forkman03/project/nome/deliverables/
  <platform>/<version>/<build>/
    artifact
    SHA256SUMS.txt
    release-record.md

/Users/forkman03/project/nome/device-backups/
  # 只保存敏感真机恢复材料；不进入 Git，后续迁移到加密备份介质
```

## 6. 分阶段执行计划

### Phase 0A：冻结当前权威快照

目的：在任何编辑、分支整合或清理前，证明当前所有资产的位置与状态。

操作：

1. 对两个 Git 仓库记录：remote、branch、HEAD、upstream、staged、modified、untracked。
2. 记录所有 linked worktree、prunable worktree、detached HEAD 和各自分支 SHA。
3. 固定关键提交表：`33b97d661`、`b44bf4560`、`ca6cf8e30`、`46f2b5bf3`、`e50f32fce`、`46cc76326`、`5dbf8ced6`、`105d69c`、`c37be37`、`d97001e`。
4. 对 Android 和 iOS integration 生成：
   - `git diff --binary`
   - `git diff --cached --binary`
   - untracked 文件清单
   - tracked modified 文件清单
5. 对 build 372/373/374 建立映射表：build number、源码位置、是否有 Git、commit、配置、包哈希、安装设备、已验证功能。
6. 对 build 374 临时源码生成仅包含源码/配置文件名的清单和 SHA-256；不读取或输出秘密值。
7. 对 `device-backups`、`deliverables` 生成文件清单和 SHA-256，保持内容原位。

退出门禁：

- 所有关键 SHA 均能通过 `git cat-file -e <sha>^{commit}` 找到。
- 两份 dirty patch 非空且 SHA-256 已记录；staged patch 明确为空或有记录。
- build 374 源码清单能够证明其 Git 缺失和 activation 文件缺失。
- 执行前后各 worktree 的 `git status --short` 完全一致。

### Phase 0B：建立可恢复备份

目的：即使后续整合失败，也能恢复所有提交、dirty 修改和唯一临时源码差异。

操作：

1. 为 `simplex-chat` 和 `website` 创建包含所有本地 refs 的 Git bundle。
2. 保存 Phase 0A 的 binary patch、状态表、untracked 源码归档和 SHA256SUMS。
3. 只归档 build 374 中相对 Git 主线不同的源码与构建记录；排除 DerivedData、Libraries、缓存和已可重建产物。
4. 保持 `Local.xcconfig` 在 Git 外；仅在私有备份中保存，其权限设为仅当前用户可读。
5. 在新的临时目录验证：
   - `git bundle verify`
   - 能从 bundle 读出关键分支
   - binary patch 能通过 `git apply --check`
   - 源码归档 SHA-256 校验通过

退出门禁：

- Git 提交、dirty tree、build 374 唯一源码差异三类资产都有独立恢复路径。
- 备份不包含 Gradle、AVD、DerivedData、Node modules 等可再生缓存。
- 备份验证通过后才能进入 Phase 1。

### Phase 1：iOS 权威主线收口

优先原因：build 374 已出现频道修改与邀请码实现分离，是当前发布漂移最高风险点。

操作：

1. 不触碰现有 dirty integration worktree；从 `5dbf8ced6` 建立新的 clean worktree 和 `codex/nome-v656-consolidated-ios`。
2. 证明 `5dbf8ced6` 已包含：
   - integration 基线 `e50f32fce`
   - 邀请码头提交 `46cc76326`
   - 取消通话提交 `72c6b213c`、`5dbf8ced6`
3. 将 integration 的 22 个 tracked 修改按责任拆分，禁止整坨提交：
   - A：服务器和构建配置：`Debug.xcconfig`、`Release.xcconfig`、Info.plist、`SimpleXAPI.swift`、`ContentView.swift`
   - B：通话运行时与诊断：`ActiveCallView`、`CallController`、`CallManager`、`IncomingCallView`、`WebRTC`、`WebRTCClient`、Tests
   - C：频道/UI/中文：Chat toolbar/item/view、Group info、Chat list、SwipeLabel、AddChannel、Localizable
4. 每组先生成独立 patch，在新 worktree 逐组应用、人工解决语义冲突、运行定向测试，再形成独立 commit。
5. 将 build 374 临时源码与 consolidated worktree 做文件级和语义级比较；只找回 Git 中缺失且确属频道/UI需求的变更。
6. 禁止从临时目录整文件覆盖 `ChatView.swift`、`SwipeLabel.swift`、`SimpleXAPI.swift` 等与邀请码/通话重叠文件。
7. 更新 build record 模板并提交 build 372/373/374 历史记录。

iOS 验收：

- `git merge-base --is-ancestor 46cc76326 <ios-consolidated-head>` 返回 0。
- `git merge-base --is-ancestor 5dbf8ced6 <ios-consolidated-head>` 返回 0。
- `NomeActivation.swift`、`NomeActivationPolicy.swift`、频道 UI 和 call-reject 实现均能从 Git 找到。
- `git diff --check` 通过。
- activation policy reducer 和相关 XCTest 全部通过。
- arm64 Simulator clean build 通过。
- 邀请码 enforced：未激活发送被拦截；激活后可发送；重启仍有效。
- 邀请码 disabled：正常发送，不弹激活页。
- 两端通话：接收方取消后，发起方在限定时间内退出等待连接状态。
- 频道：创建、加入、列表标签、顶部卡片、中文按钮、成员数/列表至少完成模拟器或真机验证。
- 下一 iOS 包必须从 consolidated Git worktree 构建，记录 commit 和 artifact SHA；禁止沿用 build 374 临时源。

### Phase 2：Android/Core/Desktop 权威主线收口

操作：

1. 不触碰现有 dirty 主工作树；从 `33b97d661` 建立新的 clean worktree 和 `codex/nome-v656-consolidated-android`。
2. 将 `codex/nome-call-reject-android-20260728` 以保留提交历史的方式整合；该分支已包含 Android 邀请码和 call-reject 提交。
3. 若 merge 出现冲突，按文件责任人工解决，禁止整侧覆盖：
   - 邀请码/网络门禁优先保持 activation policy 语义。
   - call-reject 优先保持双端状态机终止语义。
   - 33b 的 Desktop/Core 产品契约保持独立。
4. 将当前 18 个 tracked 修改按责任拆分：
   - A：频道/首页/搜索/UI标签和中文资源
   - B：服务器配置和 `SimpleXAPI.kt`
   - C：Haskell Core/Terminal/operator 行为
   - D：测试和 query plan 再生成
5. 将真正的 untracked 源码文件单独审查；Gradle 缓存、截图和历史 evidence 不进入功能 commit。
6. 重新生成或规范化 `chat_query_plans.txt`，解决当前 trailing whitespace，确保 `git diff --check` 通过。

Android/Core/Desktop 验收：

- `git merge-base --is-ancestor ca6cf8e30 <android-consolidated-head>` 返回 0。
- `git merge-base --is-ancestor 46f2b5bf3 <android-consolidated-head>` 返回 0。
- 激活前本地浏览可用，网络消息/连接/群组/频道/通话被阻止。
- 激活后发送、重启保持、过期/撤销回到 local-only 均有自动化验证。
- Android unit tests、activation instrumentation、APK assemble 通过。
- 接收方取消视频/语音后，发起方同步退出等待状态。
- 频道创建/加入/标签/成员展示完成端到端验证。
- Core Nome 定向测试和 Desktop tests 通过。
- `git diff --check` 通过。

### Phase 3：Website 与邀请码控制面对齐

操作：

1. 保持 `website/main` 的 clean 状态，核对 `105d69c`、`c37be37`、`d97001e` 的提交关系。
2. 重新运行 lint、TypeScript、生产 build、完整测试和迁移静态检查。
3. 只读检查当前生产 policy、revision、Android/iOS platform 支持和健康状态；不得从旧实施记录推断当前线上状态。
4. 检查后台邀请码生成、有效期、兑换截止日、永久可见/加密保存、普通管理员遮罩权限。
5. GitHub/LAN push、Sites 部署、生产 policy 修改分别作为独立授权门；一次授权不自动包含其他操作。

Website 验收：

- `git status --short` 为空。
- lint、typecheck、build、tests 全部通过。
- Android/iOS 使用同一邀请码 API 合约和 entitlement 规则。
- 未认证/普通管理员不能看到完整邀请码；Owner 操作有审计。
- 生产策略变化前后有 revision 和回滚记录。

### Phase 4：统一构建与发布纪律

修改 build record 模板，强制记录：

- 平台、版本、build number、日期、配置
- Bundle ID / Application ID
- Git 仓库、branch、完整 commit SHA
- `git status --short` 结果
- 是否包含受控 dirty patch；正式发布必须为否
- 配置来源与非秘密指纹
- 依赖/Core/Libraries 版本或哈希
- artifact 标准文件名和 SHA-256
- 安装设备、安装方式、是否保留容器
- 邀请码、消息、频道、通话、升级/重装验证矩阵
- 分发边界：本地安装、测试、公开发布

构建门禁：

- 构建脚本发现 tracked dirty 或未记录的源码 untracked 时直接失败。
- 缓存目录允许存在，但必须被正确忽略。
- 所有新包只允许从 consolidated Git worktree 构建。
- build number 必须单调递增；artifact 名称遵循现有标准。

### Phase 5：文档、证据和敏感备份治理

操作：

1. 新建 `plans/README.md`，标明唯一当前计划、当前状态、最新 release record 和历史归档入口。
2. 保留原交接文档作为历史证据，不让旧文档覆盖当前状态：
   `plans/20260727_nome_project_status_next_thread_handoff.md:1-8`。
3. 新证据默认只提交文本摘要、SHA256SUMS 和少量关键截图；大批截图放 `deliverables`，不进入 Git。
4. 当前 106 MB evidence 不在首轮批量移动；先分类 tracked/untracked，再分批归档，避免制造巨型 rename diff。
5. `device-backups` 生成清单后迁移到加密备份介质；确认两份可读副本前，不删除项目根目录中的原件。

验收：

- 新成员只读 `plans/README.md` 就能找到当前权威状态。
- 同一 build 只有一份权威 release record。
- Git 不包含设备数据库、完整邀请码、秘密配置或大批可重建截图。

### Phase 6：忽略规则、worktree 和磁盘整理

只有 Phase 0-5 全部通过，并再次取得用户明确清理授权后执行。

操作：

1. 小型、可审阅地更新 `.gitignore`：
   - `.gradle-review/`
   - `apps/multiplatform/.gradle-review/`
   - `apps/multiplatform/.gradle-tmp/`
   - 本地截图命名模式
   - 本地 Codex 环境目录（若确认不属于项目资产）
2. 将 AVD/模拟器运行目录迁移到仓库外的开发缓存位置。
3. 对每个 `/private/tmp/nome-*` 分类：
   - Git worktree：确认分支已整合/备份后再移除
   - 无 Git 临时源码：确认差异已进入 Git 后再归档或删除
   - DerivedData/build/cache：证明可重建、无进程占用后删除
   - 截图/日志：确认已有 manifest 或不再需要后处理
4. 先运行 `git worktree prune --dry-run --verbose`；核对分支和 bundle 后才允许实际 prune 元数据。
5. 保留 iOS `Libraries`、正式 release artifact、真机备份和不能重建的设计资产。

验收：

- canonical worktree 的 untracked 列表不再被缓存淹没。
- 所有被删目录都满足：非唯一、可重建、无活跃进程、已核对 manifest。
- 清理后重新运行 Android/iOS/Website 基础构建，不因误删本地依赖而失效。
- 预期释放二十多 GB；以清理前后 `du`/`df` 实测为准，不以估算宣称结果。

### Phase 7：提交、远端保护与最终交接

操作：

1. 每个平台按功能形成小提交，不使用 `git add -A`。
2. 本地 commit 与远端 push 分开授权。
3. 用户明确授权后，将 consolidated 分支普通推送到 GitHub 和 LAN；不 force-push，不推官方 `origin`。
4. 远端读回并验证 SHA；必要时为通过完整发布门禁的 build 创建版本 tag。
5. 生成新的唯一项目状态文档，包含：
   - 三条权威主线和 SHA
   - 最新 Android/iOS/Web build/deploy 状态
   - 已完成验证和未完成边界
   - 下一步唯一入口

最终验收：

- Android、iOS、Website 各有一条清晰权威主线。
- GitHub/LAN（若获授权）能够读回正确 SHA。
- 邀请码、取消通话、频道功能不再只存在于本地分支或 `/private/tmp`。
- 新安装包能从记录中的 Git SHA 重建并得到相同功能集合。
- canonical worktree 干净，`git diff --check` 通过。
- 当前交接文件、build record 和安装包三者信息一致。

## 7. 执行顺序与停机点

```text
Phase 0A 冻结快照
  -> Phase 0B 恢复备份验证
  -> Phase 1 iOS 收口与验证
  -> Phase 2 Android/Core/Desktop 收口与验证
  -> Phase 3 Website/控制面对齐
  -> Phase 4 构建纪律落地
  -> Phase 5 文档/证据治理
  -> 用户明确授权清理
  -> Phase 6 缓存/worktree 清理
  -> 用户明确授权远端发布
  -> Phase 7 推送与最终交接
```

任何阶段失败时立即停止在该阶段，保留日志和 patch，从上一阶段已验证快照恢复；不得为了继续推进而降低门禁。

## 8. 方案选择说明

### 采用：新 clean worktree 中收口

优点：不碰现有 dirty tree；能逐组应用 patch；失败可直接丢弃新工作树；构建来源明确。  
代价：短期保留更多工作树，占用一些磁盘。

### 不采用：在当前 dirty worktree 原地整理

原因：iOS 22 个修改和 Android 18 个修改跨多个责任域，原地 merge/rebase 会让恢复和审查困难。

### 不采用：把 build 374 临时目录整体复制回 Git

原因：该目录缺少邀请码实现且没有 Git 来源，整体覆盖会再次制造功能回退；只能用作差异证据。

## 9. 主要风险与缓解

| 风险 | 缓解措施 |
|---|---|
| 邀请码在 UI 合并时被覆盖 | 先以已含 activation/call-reject 的 clean 分支为基线；冲突逐文件解决；activation E2E 作为硬门禁 |
| build 374 独有频道修改丢失 | Phase 0 保存源码清单/hash；Phase 1 逐文件语义比较并形成 Git commit |
| iOS Local.xcconfig 再次缺失导致服务器回退 | 保持 Git 忽略；构建时验证非秘密配置指纹和包内服务器域名 |
| Android Core/UI/activation 冲突 | 拆成四个责任提交；每组运行定向测试，禁止整侧覆盖 |
| 远端仍缺关键本地提交 | 本地 bundle 先保护；验证后由用户独立授权 GitHub/LAN 普通推送 |
| 清理删掉唯一源码或证据 | 清理最后执行；要求非唯一、可重建、无进程、manifest 四项同时满足 |
| 文档再次相互矛盾 | `plans/README.md` 只指向一份 current 状态；旧文档标记 archive/historical |

## 10. 整理成功的定义

整理成功不是“目录看起来整齐”，而是同时满足：

1. 任意功能都能定位到权威分支和 commit。
2. 任意安装包都能定位到源码 SHA、构建配置和包哈希。
3. 任意 dirty 修改都有提交、明确废弃结论或可恢复 patch。
4. 任意临时目录都能说明是工作树、唯一源码证据、构建缓存还是可删除产物。
5. Android/iOS 的邀请码、消息发送门禁、重新安装、取消通话和频道流程通过规定验证。
6. Website 与移动端使用一致的邀请合约，生产变更有独立授权和回滚记录。
7. 不依赖无 Git 源码打包，不再发生“新包丢掉旧功能”。

