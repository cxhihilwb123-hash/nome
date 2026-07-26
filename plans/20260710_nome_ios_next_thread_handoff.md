# Nome iOS 新线程交接记录

更新时间：2026-07-10 10:11 CST

## 新线程第一句话

```text
继续 Nome iOS 开发。先读取 plans/20260710_nome_ios_next_thread_handoff.md
和 plans/20260710_nome_ios_project_progress.md，从真实核心与真机验证开始，
不要重新规划或重做已经通过验收的首页和主要页面。
```

## 仓库入口

- 工作目录：`/Users/forkman03/Documents/SimpleX`
- 当前分支：`codex/chinese-friendly-fork`
- Nome 代码基线：`ca41ed11`
- 基线提交标题：`feat(nome): establish iOS product and QA baseline`
- 上游基线：`d492606f`
- 远程只有 `upstream=https://github.com/simplex-chat/simplex-chat.git`。
- 不要向 `upstream` 推送 Nome 分支；需要发布时先配置用户自己的远程仓库。

新线程开始后先执行：

```bash
cd /Users/forkman03/Documents/SimpleX
git status --short --branch
git log -3 --oneline --decorate
```

预期工作树应为空；如果出现新改动，先识别来源，不要覆盖或回退。

## 必须保留的安全边界

- 当前是原生 SwiftUI App 改造，不是网页壳，也不是新协议实现。
- 保留 SimpleX 核心消息、加密、数据库、队列和服务器行为。
- 保留 `simplex:` URL scheme，除非发布兼容策略明确改变。
- 当前 `chat.simplex.*` 标识只适合兼容开发阶段，不代表 Nome 已具备独立上架资格。
- 不要把 Debug preview 页面当作真实邀请、真实群组或真实聊天证据。
- 不要在没有迁移方案时直接修改 App Group 或钥匙串组，否则可能破坏已有本地数据访问。

## 当前可见运行环境

- 模拟器：iPhone 17 Pro
- 模拟器 UDID：`95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`
- iOS Runtime：26.5
- 浏览器镜像：`http://127.0.0.1:3200/`
- Xcode 路径：`/Applications/Xcode.app/Contents/Developer`
- 系统 `xcode-select` 仍可能指向 Command Line Tools；shell 命令必须显式设置：

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
```

当前镜像服务的等效启动命令：

```bash
npx --yes serve-sim@latest 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4
```

## 已验证证据

- 最新 14 页烟测：`/tmp/nome-ios-smoke-trust-row-align-20260710`
- 最新 generic device 构建：`/tmp/nome-ios-generic-device-build-progress-20260710`
- 最新提交前目标审计：`/tmp/nome-ios-precommit-goal-audit-20260710`
- 目标审计结果：59 PASS、2 WARN、8 BLOCKED、0 FAIL。
- 人工 QA：132 项中 91 项完成，41 项未完成。
- 当前设计覆盖：7/7 页面视觉通过，14/14 烟测状态通过。

如果 `/tmp` 证据已被系统清理，使用以下命令重新生成 UI 证据：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
scripts/ios/smoke-nome-ui.sh \
  --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 \
  --output /tmp/nome-ios-smoke-next-thread
```

## 下一阶段首选路线

当前最短路径是实体 iPhone，而不是继续扩展预览页面。

1. 用数据线连接并信任一台 iPhone。
2. 检查设备与 device core：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
scripts/ios/check-ios-device-readiness.sh
```

3. 设备就绪后执行真机烟测：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
scripts/ios/run-ios-physical-device-smoke.sh \
  --output /tmp/nome-ios-physical-device-smoke-next \
  --force
```

4. 准备第二个独立账号或设备，按以下顺序验证：
   一次性邀请 -> 连接请求 -> 双向文字 -> 文件/语音 -> 群组 -> 公开联系方式。
5. 每完成一组真实功能，就更新人工 QA 清单和 QA 记录，不要只写聊天结论。

如果暂时没有实体 iPhone，可选路线是安装 Nix 并构建 arm64 simulator core，
或取得与当前 Apple Silicon 模拟器兼容的真实核心产物。现有 x86_64 模拟器
核心只能作为构建证据，iOS 26.5 arm64 模拟器无法安装该架构 App。

## 当前发布阻塞

- 真实核心：arm64 模拟器仍为 preview core。
- 真机：没有可信任设备证据。
- 人工 QA：剩余 41 项。
- App Store：4 张 `needs-real-core` 截图待替换。
- 发布身份：8 组标识与能力决策待确定。
- 法务/发布：隐私标签、加密出口说明和公开修改源码地址仍需最终确认。

不要把无签名 generic device 构建通过等同于 TestFlight 就绪。它只证明当前
源码能产出 `Nome.app`、通知扩展和分享扩展的 iPhoneOS 构建产物。

## 关键文档

- 当前进度：`plans/20260710_nome_ios_project_progress.md`
- Apple App 开发计划：`plans/20260709_nome_ios_app_development_plan.md`
- 人工 QA：`plans/20260709_nome_ios_manual_qa_checklist.md`
- 剩余 QA 批次：`plans/20260709_nome_ios_remaining_qa_execution_plan.md`
- 真实核心计划：`plans/20260709_nome_ios_real_core_testing_plan.md`
- 发布标识审查：`plans/20260709_nome_ios_release_gate_review.md`
- 隐私声明审查：`plans/20260709_nome_ios_privacy_claims_audit.md`
- 完整 QA 记录：`plans/20260709_nome_ios_qa_record.md`
- 品牌与设计记录：`design/NOME_DESIGN_RECORD.md`

## 下一里程碑完成标准

只有同时满足以下条件，才可以把下一阶段标记为完成：

- 至少两个真实账号完成一对一双向消息。
- 一次性邀请链接和二维码来自真实核心。
- 真实群组邀请和群组消息通过。
- 公开联系方式来自真实核心并可复制、分享和连接。
- 真机安装、启动、相机和系统分享路径通过。
- 没有新增 Nome 崩溃报告。
- 对应人工 QA 项已勾选并附证据路径。
- 4 张 `needs-real-core` App Store 截图已替换。

完成一个可审查的小批次后立即提交，避免再次积累数百个未提交文件。
