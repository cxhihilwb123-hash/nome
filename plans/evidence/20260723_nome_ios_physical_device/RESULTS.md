# Nome iOS 真实设备验证记录

日期：2026-07-23（Asia/Shanghai）

## 结论

状态：**BLOCKED_INFRASTRUCTURE**

本轮从 `codex/nome-ios-v656-integration` 的
`2cb055358829168f02d78e24511309011c19c006` 继续，没有重做 Core 集成，也没有
修改 Android。

精确 Core 6.5.6.1 的 physical-device 预检通过，但当前不能形成可安装的签名
产物，也没有建立 Xcode 真实设备开发通道。因此安装、启动、真机通信、钥匙串、
通知、相机、分享扩展和前后台切换都没有被宣称为通过。

这不是 Core 或产品编译失败。当前阻塞属于设备连接和 Apple 签名基础设施。

## 已确认

- Xcode 26.6 / build 17F113 可用。
- Xcode 工程、`SimpleX (iOS)` scheme 和 generic iOS destination 可见。
- 已安装 device Core 是官方 6.5.6.1、arm64、Mach-O iOS platform 2。
- 两个 Core archive 都是 production-size，未发现 preview-core markers。
- Xcode 工程已经引用当前 6.5.6.1 archive 文件名。
- 工程仍保留兼容标识：主 App `chat.simplex.app`，以及对应通知扩展、分享扩展、
  App Group 和钥匙串组；本轮没有修改这些标识。
- 目标 iPhone 的配对记录和 Developer Mode 正常；macOS iPhone 镜像可以连接。
- iPhone 镜像只证明手机可达，不会建立 Xcode CoreDevice/DDI 开发通道，不能替代
  安装或运行证据。
- 设备上的既有 App 仍显示为 SimpleX；没有把既有安装当作当前 Nome 分支产物。

## 设备连接阻塞

实时 Xcode/CoreDevice 结果：

- CoreDevice state：`unavailable`
- DDI services：不可用
- developer tunnel：`unavailable`
- `xctrace`：设备位于 `Devices Offline`
- Xcode Devices：`Disconnected`
- USB 设备树：没有连接的 iPhone

Xcode 给出的恢复条件是：物理解锁设备，并用数据线连接，或确保设备与 Mac 位于
同一局域网且无线开发通道可用。iPhone 镜像连接后重新检查，开发通道仍未建立。

## 签名阻塞

### 当前工程团队

启用签名的 generic iPhoneOS Debug 构建在 provisioning 阶段停止：

- 没有当前工程配置团队对应的 Apple Development 证书私钥；
- 没有主 App、通知扩展和分享扩展的 development provisioning profiles。

编译依赖图和 Core 链接输入在签名检查前没有报告产品错误。

### 临时个人团队探测

随后通过命令行临时覆盖到本机已配置的 Personal Team，并允许 Xcode 自动准备
profile；该操作没有修改仓库。Apple 明确拒绝当前能力集合：

- Personal Team 不支持 Notification Service Extension 所需能力；
- Personal Team 不支持 Push Notifications；
- Personal Team 不支持 Associated Domains；
- Personal Team 不支持 In-App Purchase、User Assigned Device Name 和
  Multicast Networking；
- 兼容 App Group `group.chat.simplex.app` 对该团队不可用；
- 因此主 App、通知扩展和分享扩展都无法生成可安装 profile。

不能通过删除 entitlement 或禁用扩展来把本轮标记为通过，因为这会绕开本次必须
验证的钥匙串、通知和分享扩展能力。

## 代码整理

真实设备 smoke runner 的自动发现原先只接受 36 字符 UUID，而当前 iPhone 的
`xctrace` 标识是现代 UDID 格式。现在：

- `--device-id` 明确接受 UUID 或 UDID；
- 自动解析接受 20–40 字符的十六进制/连字符设备标识；
- 任意后续 `xctrace` section 都会终止在线设备解析；
- `Devices Offline` 中的设备不会被误报为已连接；
- 单元夹具覆盖现代 UDID 和 offline-only 场景。

`scripts/ios/test-run-ios-physical-device-smoke.sh`：

```text
[PASS] no connected device blocks before build
[PASS] connected modern-UDID fixture records skipped build install launch
[PASS] physical-device smoke tests passed
```

`scripts/ios/test-check-ios-device-readiness.sh`：

```text
[PASS] iOS device readiness xctrace parser fixtures
```

## 真机矩阵

| 项目 | 状态 | 说明 |
|---|---|---|
| 签名构建 | BLOCKED | 缺少有能力的 Apple Development team/profile |
| 安装 | BLOCKED | 没有可安装签名产物，设备开发通道也离线 |
| 启动 | BLOCKED | 未安装当前分支产物 |
| 双向通信 | BLOCKED | 未启动当前分支真机 App |
| 钥匙串 | BLOCKED | 不能绕开 App Group/keychain entitlement 验证 |
| 通知 | BLOCKED | Personal Team 不支持所需 Push/NSE 能力 |
| 相机 | BLOCKED | 当前分支未安装 |
| 分享扩展 | BLOCKED | 分享扩展 profile 无法生成 |
| 前后台切换 | BLOCKED | 当前分支未安装 |

## 继续条件

继续真实设备验证需要同时满足：

1. iPhone 物理解锁、信任并通过数据线或可用无线开发通道连接；
2. Xcode 显示设备为 connected，CoreDevice DDI/tunnel 可用；
3. 使用能够签发当前主 App、通知扩展、分享扩展、App Group、钥匙串、Push 和
   Associated Domains 能力的付费 Apple Development team；
4. 对兼容标识继续沿用，或 Nome 自有标识与数据迁移方案，作出明确签名决策；
5. 决策前不修改 bundle id、App Group、钥匙串组或 entitlement。

满足后从以下顺序继续：

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  scripts/ios/check-ios-device-readiness.sh

scripts/ios/check-real-core.sh --target physical-device
scripts/ios/sync-real-core-xcode-project.sh --check

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  scripts/ios/run-ios-physical-device-smoke.sh \
    --output /private/tmp/nome-ios-physical-device-smoke-current \
    --force
```

只有 signed build、install 和 launch 全部通过后，才进入通信、钥匙串、通知、
相机、分享扩展和前后台切换的真机人工矩阵。

## 证据保留

可提交的去敏结论是本文档。包含本机设备标识、序列号、团队名和完整 Xcode 输出
的原始日志保存在同目录的 Git 忽略 `private/` 子目录，不会提交到 PR。
