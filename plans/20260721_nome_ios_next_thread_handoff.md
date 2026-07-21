# Nome iOS 下一线程交接

## Resume point

```bash
cd /Users/forkman03/project/nome/simplex-chat-ios
git branch --show-current
git status --short
```

期望分支为 `codex/nome-ios-v656`，起点 HEAD 为 `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`。工作树中的大量 iOS 变更是本次已验证迁移结果，不要 reset、clean 或 stash。

权威当前记录：`plans/20260721_nome_ios_migration_checkpoint.md`。

## Current verified state

- 旧 iOS Git 白名单对象恢复完整：259/259，旧交接文档 2/2。
- iCloud 原目录只读保留；Android 工作树未修改。
- 模拟器 preview-core 已复制并逐文件 SHA-256 校验；路径由 `apps/ios/.gitignore` 忽略。
- `scripts/ios/test-*.sh`：26/26 PASS。
- Nome brand/design checks：PASS。
- Xcode 26.6 signed simulator build：PASS。
- iPhone 17 Pro / iOS 26.5 install + launch：PASS。
- UI smoke：14/14 PASS；当前证据位于 `design/migration/smoke-20260721/`。
- 没有 commit，没有 push。

## Build command

XcodeBuildMCP 会受当前系统 `xcode-select` 限制。不要为了本项目擅自修改系统设置；使用显式完整 Xcode 路径：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild -project apps/ios/SimpleX.xcodeproj \
  -scheme 'SimpleX (iOS)' -configuration Debug \
  -destination 'platform=iOS Simulator,id=95CA9F4F-F85B-4AC9-ADAE-62098924E3B4' \
  -derivedDataPath /private/tmp/nome-ios-migration-deriveddata build
```

不要用 `CODE_SIGNING_ALLOWED=NO` 的产物做运行证据；App Group simulated entitlement 缺失会在 `FileUtils.swift:getGroupContainerDirectory()` 启动崩溃。

## Mandatory boundaries

- iOS-only；不得修改 Android、共享 Kotlin、Haskell/native core、`Core.kt`、协议、数据库和消息状态机。
- preview UI 不等于真实 core、真实连接或消息送达。
- iCloud 真机库为 6.5.5 实体并以文件名链接适配 6.5.6.1；未复制，不得默认为可信 v6.5.6 真机核心。
- 未经用户明确授权，不 commit、不 push、不改远端。
- 不删除 iCloud 原目录，不清理 Android 用户未跟踪文件。

## Recommended next batch

先以当前 14 状态为视觉回归基线，同步推进 iOS 的 Nome 设计批次；随后单独开真实核心/真机批次，获取与 v6.5.6 匹配的可信 simulator/device archives，再做双账号通信、通知、分享扩展、迁移数据、可访问性和发布身份验证。
