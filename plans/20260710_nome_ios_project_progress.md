# Nome iOS 项目进度记录

更新时间：2026-07-10 10:11 CST

## 当前结论

Nome iOS 已经形成可重复构建、可在模拟器完整演示的产品与视觉基线，
但真实通信功能和正式发布条件尚未完成。

- 可演示产品完成度：约 90%。
- 真实通信功能验证：约 45%。
- TestFlight / App Store 上架准备：约 35%。
- 综合开发进度：约 65% - 70%。

以上百分比是基于当前功能和发布风险的工程估算，不是简单按文件或页面数量计算。

## 代码检查点

- 仓库：`/Users/forkman03/Documents/SimpleX`
- 分支：`codex/chinese-friendly-fork`
- Nome 开发基线提交：`ca41ed11 feat(nome): establish iOS product and QA baseline`
- 基线提交规模：301 个文件，27250 行新增，1015 行删除。
- 远程：只有上游 `upstream`，当前提交未推送到任何远程仓库。
- 协议兼容边界：保留 `simplex:` 链接、核心消息逻辑、数据库语义、
  服务器协议和当前兼容标识；本阶段主要修改品牌、文案、导航和 SwiftUI 交互。

## 已完成范围

- Nome Logo、App 图标、横向标识、深色与浅色品牌资源。
- iOS 首页、聊天列表、聊天页、联系人页和底部导航。
- 添加朋友、一次性邀请、扫码和粘贴连接入口。
- 加入群组、公开联系方式、身份中心和隐藏身份页面。
- 首次启动、本地身份、网络设置和网络条件确认流程。
- 简化设置页、完整设置入口、隐私、安全、备份迁移和服务器入口。
- 聊天安全状态、阅后即焚提示和会话详情预览。
- 中文用户文案和 Nome 品牌回归检查。
- 7 个批准设计页面、14 个模拟器烟测状态和 App Store 候选截图流程。
- 真实核心、真机、发布标识和截图阻塞的自动化检查脚本与执行计划。

## 最新验证证据

### 综合门禁

命令：

```bash
scripts/ios/check-nome-ios-goal-audit.sh \
  --smoke-manifest /tmp/nome-ios-smoke-trust-row-align-20260710/manifest.tsv \
  --generic-device-build-dir /tmp/nome-ios-generic-device-build-progress-20260710 \
  --output /tmp/nome-ios-precommit-goal-audit-20260710 \
  --allow-blockers
```

结果：

- PASS：59
- WARN：2
- BLOCKED：8
- FAIL：0

### 模拟器 UI

- 14/14 个关键状态构建、安装、启动和截图成功。
- 14/14 个截图通过尺寸、哈希、非空和重复图检查。
- 没有产生新的 `Nome-*.ips` 崩溃报告。
- 最新烟测：`/tmp/nome-ios-smoke-trust-row-align-20260710`
- 当前浏览器镜像：`http://127.0.0.1:3200/`，检查时返回 HTTP 200。

### iPhoneOS 构建

当前源码已完成无签名 generic iOS device 构建：

- `Nome.app`：PASS，约 252412 KB。
- `SimpleX NSE.appex`：PASS，约 2324 KB。
- `SimpleX SE.appex`：PASS，约 4480 KB。
- 构建证据：`/tmp/nome-ios-generic-device-build-progress-20260710`

### 人工 QA

- 总项目：132
- 已完成：91
- 未完成：41
- 未完成项集中在真实邀请、双账号聊天、群组、真实公开地址、
  文件与语音、身份迁移、真机相机和发布配置。

## 当前阻塞

1. arm64 模拟器仍链接约 10 KB 的 preview core，不能证明真实通信行为。
2. 本机没有安装 Nix，无法直接在本地构建 arm64 模拟器真实核心。
3. 当前没有连接可信任的实体 iPhone 或 iPad，真机安装和功能烟测未执行。
4. 41 个人工 QA 项仍需要真实核心、双账号或真机证据。
5. App Store 最终截图中有 4 张必须替换为真实核心数据截图：
   添加朋友、公开联系方式、加入群组、双账号会话。
6. 正式发布仍有 8 组标识与能力决策，包括 Bundle ID、App Group、
   钥匙串组、后台任务标识和关联域名。

## 下一里程碑

下一阶段不应再做大范围首页重设计。优先完成真实核心功能验证：

1. 连接可信任的实体 iPhone，使用现有 arm64 device core 构建并安装 Nome。
2. 建立两个独立账号，验证一次性邀请、公开联系方式、双向聊天和群组。
3. 完成文件、语音、回执、回复、删除、反应和安全码状态检查。
4. 完成身份切换、备份迁移、服务器与 Tor 路径检查。
5. 替换 4 张真实功能截图。
6. 最后确定 Nome 自有发布标识并执行迁移脚本。

详细批次见 `plans/20260709_nome_ios_remaining_qa_execution_plan.md`。
