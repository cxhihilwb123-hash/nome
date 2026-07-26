# Nome iOS 新用户交互与双模拟器页面验收

日期：2026-07-25（Asia/Shanghai）

## 结论

状态：**PASS_WITH_PRODUCTION_LIMITS**

本轮仅修改和验证 iOS 页面、交互及简体中文文案。Android、SimpleX Core、
协议、数据库和消息状态机均未修改。

两台专用 iOS 26.5 模拟器同时完成验收：

- iPhone 14 Pro：全新 App 容器，无预览参数，使用
  `Nome验收测试20260725` 走通真实首次启动、资料创建、网络说明、使用条件和
  空白首页。
- iPhone SE（第 3 代）：使用 `-NomeChatListPreview` 的三条确定性测试数据，
  回归首页、联系人、设置、明暗模式、小屏布局和三轮搜索。

主要验收源码提交：`a51728769991bebad3fbc405ae9b3882173434dd`。
上游预置联系人清理跟进提交：`9609b6b56`。
流程页 Logo 与频道中文跟进提交：`d1b08e551`。
二级流程页重复 Logo 清理提交：`d9c4b6531`。
最终模拟器产物：Nome `6.5.6 (337)`，`** BUILD SUCCEEDED **`。

## 本轮发现并修复

1. 创建资料成功后虽然数据库已有用户，但当前进程没有同步
   `ChatModel.onboardingStage`，界面会留在被清空的资料页。现在持久化阶段和
   内存阶段一起更新，当前进程直接进入网络说明页。
2. 创建、网络继续等相邻全屏页的主按钮位于同一区域，快速双击可能把第二次
   触摸传给下一页。现在仅这两个全屏跳转按钮使用系统单击/双击互斥识别，一次
   双击只产生一次跳转；普通弹窗和完成操作仍保持即时按钮响应。
3. 创建资料和接受条件增加明确的进行中状态、进度图标、禁用状态和失败恢复，
   防止重复提交。
4. 首次资料名改为使用与普通资料一致的 Core 名称规范校验；空值、全空格、
   不支持字符或过长内容不能提交，非空非法内容会显示红色说明。
5. 空白首页缺少的简体中文文案已补齐。
6. Debug 页面预览现在会随系统明暗模式同步主题，修复深色预览中的混合配色。
7. 搜索“取消”从文字点击手势改为原生 `Button`，保留最小点击区域并恢复正确
   的辅助功能按钮语义。
8. “添加”页不再显示 `Ask SimpleX Team` 和 `SimpleX Status` 两个上游预置
   联系人卡片；Nome 启动时会精确识别并从本地删除这两个未连接的预置卡片，
   普通联系人及其他联系卡片不受影响。
9. “添加”、加好友、加入群组和公开联系方式等流程页不再把方形 App 图标作为
   品牌页头；统一使用透明背景、自动适配明暗模式的 Nome 横向 Logo，并增大
   导航栏和页面内展示尺寸。
10. “创建公开频道”首屏及创建进度、频道链接、中继状态、错误和取消提示补齐
    简体中文；加好友失败兜底提示也不再回退到英文。
11. 加好友、加入群组和公开联系方式等二级页删除内容区重复 Logo，只保留顶部
    导航栏 Nome Logo；公开联系方式同时改为内联导航，避免大标题与内容标题
    重复。
12. 修复带 Nome 私有服务器配置的新用户引导仍读取、展示并可能重新启用上游
    运营商的问题。配置存在时，网络页现在只显示不可点击的
    `Nome 官方服务器` 状态卡；进入首页前会再次应用 Nome SMP/XFTP 配置，
    成功后才完成引导，失败时保留在当前页重试且不会改用其他服务器。

### 服务器配置生效顺序跟进验证

本次跟进使用 Git 忽略的 `apps/ios/Local.xcconfig` 构建，未在记录、截图、命令
输出或提交中保存真实 SMP/XFTP 地址。编译产物只以布尔检查确认两个配置项分别
以 `smp://` 和 `xftp://` 生效，本地配置文件仍未被 Git 跟踪。

- iPhone 14 Pro 专用验收模拟器：卸载旧 App 后安装 Nome `6.5.6 (337)`，
  使用 `Nome服务器修复测试` 完整走通欢迎、创建本机身份、网络说明、使用条件
  和首页。
- 网络页：PASS，只显示 `Nome 官方服务器` 和
  `消息与文件服务器已自动配置`；没有 SimpleX/Flux 运营商 Logo、列表或选择
  入口。
- 辅助功能：PASS，Nome 官方服务器状态卡识别为静态文本而不是无效按钮；
  通知方式和继续按钮保持可操作。
- 完成顺序：PASS，点击进入首页时先应用 Nome 官方配置。模拟器系统日志记录
  `Nome official message and file servers are active; preset operators are disabled`。
- 冷启动恢复：PASS，完成后完全结束 App 再启动，直接回到首页，没有返回资料
  或运营商设置页。
- 设置页：PASS，服务器与 Tor 页面只显示 `Nome 官方服务器` 顶层入口，没有
  SimpleX 官方运营商列表。
- iPhone SE（第 3 代）：同一产物完成 14 个现有 UI 烟雾页面，全部截图成功、
  哈希无重复，前后没有新增 Nome 崩溃报告；网络页和使用条件页的小屏布局通过。
- 构建：PASS，Xcode 26.6、iOS 26.5，`** BUILD SUCCEEDED **`。
- 脚本：`test-check-ios-preview-tooling.sh`、
  `test-capture-nome-accessibility-previews.sh`、
  `check-ios-preview-tooling.sh` 全部通过。

敏感截图和烟雾输出仅保存在 Git 忽略的 `private/server-fix-*` 路径。

## 模拟器 A：全新用户

| 检查 | 结果 |
| --- | --- |
| 冷启动欢迎页、标题、说明、插图与按钮 | PASS |
| 欢迎页进入资料页、边缘返回、导航返回 | PASS |
| 资料页前后台恢复 | PASS |
| 未创建资料时完全退出并冷启动 | PASS，按设计回到欢迎页 |
| 空值和全空格 | PASS，创建按钮禁用 |
| 英文、中文、中英混合 | PASS，创建按钮启用 |
| 不支持或过长名称 | PASS，按钮禁用并显示校验说明 |
| 键盘、光标、清空及底部按钮可见性 | PASS |
| 快速双触创建 | PASS，只创建一次；立即显示“正在创建…” |
| 创建后当前进程跳转 | PASS，停在网络说明页，不白屏、不跳过 |
| 创建后冷启动恢复 | PASS，恢复网络说明页 |
| 网络页前后台恢复 | PASS |
| 快速双触“继续” | PASS，只进入使用条件页一次 |
| 使用条件页冷启动恢复 | PASS |
| 快速双触“同意并进入 Nome” | PASS，只完成一次 |
| 完成后冷启动 | PASS，直接进入空白首页 |
| 空白首页引导文案 | PASS，简体中文且入口清楚 |
| 欢迎、网络、条件、首页、设置明暗模式 | PASS |
| 扫码权限时机 | PASS，进入扫码动作后才请求相机权限 |
| 拒绝相机权限后的恢复提示 | PASS，显示“允许相机权限”入口 |
| Nome 服务器页 | PASS，只显示 `Nome 官方服务器` 入口；无 SimpleX 官方列表 |

通知权限在完成 onboarding 后才出现；相机权限在选择扫码后才出现。模拟器只
验证请求时机、拒绝态和恢复入口，不能替代真实相机取景。

## 模拟器 B：已有用户与小屏回归

三条预览会话均为仓库测试数据，不含真实联系人、资料或聊天。

| 检查 | 结果 |
| --- | --- |
| 首页 Logo、标题、头像、时间、未读数、行高 | PASS |
| 首页不出现“联系人、群、附注”筛选项 | PASS |
| 群聊名称后的紧凑“群”标识 | PASS |
| 联系人页分组、搜索和间距 | PASS |
| 首页、联系人、设置明亮/深色 | PASS |
| 底部导航、FAB 和页面切换 | PASS |
| iPhone SE 小屏安全区、底部遮挡、横向溢出 | PASS |
| 超长双语群名 | PASS，以省略号收束，无控件重叠 |

搜索连续三轮通过：

1. `林`：只保留 `林晓 / Lin Xiao`，清空后恢复三条会话，取消后恢复底部导航。
2. `不存在`：固定在搜索区下方显示“找不到聊天”，键盘变化时不漂移；清空、
   取消后恢复。
3. `产品`：只保留产品群；原生辅助功能树识别“取消”为按钮；清空和取消后
   恢复全部内容。

三轮使用同一存活进程完成；第三轮后进程采样命令返回成功，页面仍可响应，未
观察到此前的 SwiftUI 布局循环。

### 上游预置联系人移除复验

在模拟器 B 安装签名 Debug 产物并使用真实 App 状态启动，打开“添加”页后确认
两个上游预置联系人均未出现，底部显示“还没有联系人”。随后完全结束 App
进程、冷启动、重新打开“添加”页并滚动到底部，结果保持不变，App 进程仍存活。

- `Ask SimpleX Team`：PASS，不显示
- `SimpleX Status`：PASS，不显示
- 五个 Nome 添加动作：PASS，保持可见
- 普通联系人边界：PASS，过滤及清理仅匹配 `isContactCard` 与两个精确名称
- 公开截图：`b-add-sheet-without-upstream-presets.png`

### 流程页 Logo 与频道中文复验

在模拟器 B 安装最新签名 Debug 产物后完成以下复验：

- “添加”入口卡片：PASS，透明 Nome Logo 无白底，尺寸增大
- 加好友、加入群组页：PASS，每页只保留一个顶部 Nome Logo
- 公开联系方式页：PASS，只保留一个顶部 Nome Logo，标题只显示一次
- 明亮/深色模式：PASS，Logo 随外观切换并保持清晰
- “创建公开频道”首屏：PASS，标题、输入框、中继设置、创建按钮及说明均为中文
- 频道后续状态：PASS，创建进度、链接、错误和取消所需词条均有简体中文
- Xcode 签名模拟器构建：PASS，`** BUILD SUCCEEDED **`
- 公开截图：`b-add-sheet-transparent-logo.png`、
  `b-create-public-channel-zh-hans.png`、`b-join-group-single-logo.png`、
  `b-public-address-single-logo.png`

## Android/iOS 视觉对照

Android 参考只读使用，未改 Android 文件。并排图保留 Android 左、iOS 右：

- `android-ios-home-side-by-side.png`
- `android-ios-contacts-side-by-side.png`

Logo 左边距、28pt/28sp 标题层级、搜索框、44pt/dp 头像、72pt/dp 会话行、
紧凑群标识和底部导航整体一致。iOS 保留原生安全区和小屏省略行为，不机械复制
Android。

## 构建与脚本检查

- Xcode：26.6（Build 17F113）
- Project：`apps/ios/SimpleX.xcodeproj`
- Scheme：`SimpleX (iOS)`
- Destination：iPhone SE（第 3 代），iOS 26.5
- 结果：`** BUILD SUCCEEDED **`
- `git diff --check`：PASS
- `plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings`：PASS
- `scripts/ios/test-check-ios-preview-tooling.sh`：PASS
- `scripts/ios/test-capture-nome-accessibility-previews.sh`：PASS
- `scripts/ios/check-ios-preview-tooling.sh`：PASS

## 服务器和安全边界

本次普通 Debug 构建没有注入 Git 忽略的 SMP/XFTP 私有值，因此只证明：

- 新测试用户可以完成本地初始化并进入首页；
- 品牌服务器页只展示 `Nome 官方服务器` 顶层入口；
- SimpleX 官方服务器列表没有重新出现；
- 仓库和本目录证据中没有提交真实服务器地址或凭据。

本轮没有重新执行腾讯云通信。腾讯云消息/文件服务器配置、模拟器真实 Core
3+3 双向消息，以及 Personal Team 真机双向通信已经分别由
`../20260722_nome_ios_v6561_build1460001/RESULTS.md` 和
`../20260723_nome_ios_physical_device/RESULTS.md` 确认。

## 仍需付费账号或正式生产环境

免费 Personal Team 和模拟器不能完成以下生产验收：

- 正式 bundle 与付费团队签名下的数据/钥匙串兼容迁移；
- APNs 前台、后台、锁屏到达及 Notification Service Extension；
- Share Extension 和完整 App Group；
- 真机相机取景、拍摄、二维码识别；
- 真实后台保活和系统回收后的恢复；
- TestFlight/App Store 分发签名及第二台真实设备的生产配置闭环。

这些项目保持为生产门槛，不能由本轮模拟器页面 PASS 代替。

## 证据说明

本目录根部 PNG 均为去敏后的测试数据截图；`private/` 只提交忽略规则，原始 UI
层级、构建/运行日志、进程采样和中间截图留在本机且不会进入 Git。公开证据不
含设备标识、Apple 团队信息、真实联系人、邀请链接、二维码、服务器地址或
凭据。
