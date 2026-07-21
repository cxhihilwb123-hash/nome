# Nome iOS v6.5.6 迁移检查点

日期：2026-07-21

## Verdict

旧 Nome iOS 成果已安全复制到独立工作树，当前状态可以与 Android 并行进行 iOS 换皮和页面开发。原 iCloud 目录保持原位，Android 工作树没有被覆盖。本检查点证明源码恢复、模拟器编译、安装、启动和 14 个确定性 UI 状态；不证明真实通信、真机签名或 App Store 可发布。

## Workspace and provenance

- 新工作树：`/Users/forkman03/project/nome/simplex-chat-ios`
- 新分支：`codex/nome-ios-v656`
- 当前起点：`6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- 官方基线：`v6.5.6` / `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- iCloud 源目录：`/Users/forkman03/Library/Mobile Documents/com~apple~CloudDocs/Documents/SimpleX`
- 旧 iOS 代码检查点：`ca41ed118d12ba2af5913ec848945067cbcbaffb`
- 旧 iOS 文档检查点：`24337a6205ca72ff6d02ba43a0f5459ad5ea6b04`
- 本地归档引用：`refs/archive/nome-ios-20260710` -> `24337a6205ca72ff6d02ba43a0f5459ad5ea6b04`

## Migrated scope

- `apps/ios/**`：旧代码提交中的 115 个变更路径。
- `scripts/ios/**`：旧代码提交中的 69 个变更路径。
- `design/**`：旧代码提交中的 65 个设计与历史证据路径。
- `plans/**`：旧代码提交中的 10 份 iOS 计划，以及旧文档提交中的 2 份进度/交接文档。
- 当前 product/spec/CODE 文档补充与本次迁移记录。
- Git 忽略的 `apps/ios/Libraries/sim`：仅复制 5 个 preview-core archive 以恢复旧模拟器开发路径。

旧代码白名单共 259 个文件，当前工作树对象哈希与 `ca41ed11` 全部一致；两份旧交接文档与 `24337a62` 全部一致。旧提交路径清单排序 SHA-256：

`4cb2a0e79eda1ceabe797bc1c29cdd2a9974247884064e5a2744b1f654548b61`

## Preview-core copy evidence

| File | Bytes | SHA-256 |
|---|---:|---|
| `libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik-ghc9.6.3.a` | 10104 | `f673ed13fb55bbc1aed83e26f82455106edb00062102d704019e8931bb6cca73` |
| `libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik.a` | 10104 | `f673ed13fb55bbc1aed83e26f82455106edb00062102d704019e8931bb6cca73` |
| `libffi.a` | 10104 | `5b56e4e521282940559ecd923c7976f1e3ee48ce80d79b98b1d7ab8d3d264ae8` |
| `libgmp.a` | 10104 | `5b56e4e521282940559ecd923c7976f1e3ee48ce80d79b98b1d7ab8d3d264ae8` |
| `libgmpxx.a` | 10104 | `5b56e4e521282940559ecd923c7976f1e3ee48ce80d79b98b1d7ab8d3d264ae8` |

这些 archive 仅用于确定性预览 UI。它们不是生产 Haskell core，不能作为邀请、建群、消息送达或加密实现的运行证据。

## Verification

| Gate | Current result |
|---|---|
| 迁移 allowlist | PASS；越界路径 0，禁止路径 0 |
| Git 对象完整性 | PASS；259/259 + 2/2 |
| `git diff --check` | PASS |
| 私钥/证书/描述文件与高置信密钥扫描 | PASS；未发现 |
| shell 语法 | PASS；失败 0 |
| `scripts/ios/test-*.sh` | PASS；26/26 |
| Nome 品牌文案 | PASS |
| Nome 设计覆盖 | PASS |
| product/spec 源码链接 | PASS；1,028 个引用，无缺失或越界 |
| Xcode 工程发现 | PASS；`apps/ios/SimpleX.xcodeproj`，scheme `SimpleX (iOS)` |
| 模拟器编译 | PASS；Xcode 26.6，iPhone 17 Pro / iOS 26.5 |
| 安装与启动 | PASS；`chat.simplex.app`，Nome 聊天列表预览可见 |
| UI smoke | PASS；14/14，运行后无新增 Nome crash |
| 截图完整性 | PASS；14/14，1206x2622，字节数与 SHA-256 一致 |

本次冒烟证据：

- `design/migration/smoke-20260721/manifest.tsv`
- `design/migration/smoke-20260721/contact-sheet.png`
- manifest SHA-256：`d345d9290ebe214b590ce8151504eaa20104567ba453130dc2a1529576e74d6c`
- contact sheet SHA-256：`94b7608e85cc436fd26576807de1dec00c88a5e55f49150e7911f88d223ddd79`

## Important diagnostic boundary

首次使用 `CODE_SIGNING_ALLOWED=NO` 构建时，Swift 编译和链接成功，但运行期因为 `getGroupContainerDirectory()` 无法取得 App Group 容器而触发 nil unwrap。随后使用 Xcode 的本地模拟器签名和 simulated entitlements 重建，构建、安装、启动和 14 状态 smoke 全部通过，且没有新增崩溃报告。因此该首次崩溃属于错误的无签名运行配置，不是迁移源码回归。

XcodeBuildMCP 已按要求先检查 session defaults，但系统 `xcode-select` 指向 `/Library/Developer/CommandLineTools`，MCP 无法发现 `xcodebuild`/`simctl`。没有修改系统级 `xcode-select`；使用显式完整 Xcode `DEVELOPER_DIR` 完成构建和运行验证。

## Preserved boundaries

- iCloud 源目录未移动、未删除、未写入。
- Android 工作树仍在 `codex/nome-android-v656` / `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`，既有未跟踪文件保持原样。
- 没有恢复旧 Android/共享 Kotlin 改动。
- 没有修改 Haskell/native core、`Core.kt`、协议、数据库或消息状态机。
- 没有修改最终 Bundle ID、App Group、钥匙串组、域名或签名策略。
- 已创建本地迁移检查点 `3edd26fcd`；没有 push。

## Remaining release blockers

1. arm64 模拟器仍使用 preview core；真实双账号通信尚未验证。
2. iCloud 中的真机 archive 实体版本为 6.5.5 并通过文件名链接兼容 6.5.6.1，本次未复制，不能当作可信 v6.5.6 真机核心。
3. 未完成连接真机、签名安装、通知扩展、分享扩展和迁移数据的真机验证。
4. 最终 Nome-owned Bundle ID、App Group、钥匙串组、associated domains 尚未决定。
5. 人工可访问性、浅色/深色、动态字体、App Store 最终截图和发布身份门禁仍需完成。

## Safe next step

在 `/Users/forkman03/project/nome/simplex-chat-ios` 内继续 iOS-only 收口；每个批次只修改 `apps/ios/**`、`scripts/ios/**`、`design/**` 和对应 iOS 文档。深色模式可读性后续见 `plans/20260721_04.md`。真实通信阶段必须先取得与 `v6.5.6` 匹配的可信 arm64 simulator/device core，再执行真机和双账号证据门禁。
