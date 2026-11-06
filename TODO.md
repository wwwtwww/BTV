# BTV 项目待办清单 (TODO)

> 本文档用于记录暂缓实现、待排期或长期规划的功能与重构事项。
> 配套文档：[PRD](docs/PRD.md) | [开发计划](docs/开发计划.md) | [UI 优化建议](docs/UI优化设计建议.md) | [真机测试指南](docs/真机测试与验收指南.md)

---

## 📌 当前进行与完成状态概览

- [x] **v0.5.5：播放器 OSD 快捷控制微菜单与遥控器事件消费规范（v0.5.5 已发布）**
  - **YouTube TV 风格底栏直达微卡片**：废除调整画质/倍速/音轨/字幕需要唤醒 450dp 右侧三级抽屉、横跨三列按键十多次的冗长链路；
  - **底部 OSD 快捷直达入口**：在点播底部操作栏常驻「画质」、「倍速」、「音轨」、「字幕」紧凑按钮；在直播底部操作栏常驻「画质」、「线路」按钮；
  - **垂直轻量悬浮微卡片（Popover）**：点击快捷入口即在底栏上方展开垂直半透明浮层，自动聚焦当前正在生效的选项；左右方向键无缝在「画质 ↔ 倍速 ↔ 音轨 ↔ 字幕」之间平移流转，按 OK 立即生效并自动收回；
  - **快捷微菜单遥控器按键消费优先级规范落地**：
    - 落地 `handleQuickMenuKeyEvent`：微菜单展开时，按物理「菜单键（Menu / Key 763）」或「返回键（Back）」独占消费事件并平滑收起微菜单，将焦点精准归还底栏对应按钮，根除“微菜单收回的同时侧边抽屉抢先滑入”的双浮层竞态冲突；
    - 首项拦截方向键向上（防止逃逸至背景进度条），尾项按向下方向键顺畅收回微菜单；
    - 顶层播放控制器事件分流至冒泡阶段（`onKeyEvent`），赋权子组件优先消费焦点按键；
  - **分 P 选集去胶囊化与垂直居中重塑**：
    - 主选集卡片（36dp）、二级子分 P 卡片（30dp）与 50 集分组卡片（28dp）彻底去胶囊化，全面收敛为 `BVShape.small (8.dp)` 标准微圆角矩形；
    - 注入 `.fillMaxHeight()` 确保 `Alignment.CenterVertically` 垂直绝对居中，彻底消灭“文字靠顶端、底部空出一整行”的留白缺陷；
  - **全站业务次级筛选与操作去胶囊化**：
    - **收藏夹二级横向列表**：彻底废除 `LazyRow` 胶囊，接入全站统一的 `TopNav`（极简纯文字 + 底部滑动单色指示条），并彻底解决快速连滑切换文件夹时的协程竞态与丢包问题；
    - **操作类按钮**：视频长按弹窗横向按钮（`ActionButtonItem`）、个人页「全部播放」按钮、搜索筛选弹窗底部重置/确定按钮及顶栏筛选按钮，统一收敛为 **`BVShape.medium (12.dp)`** 标准功能按钮；
    - **搜索历史词云**：由全圆角药丸统一收敛为 **`BVShape.small (8.dp)`** 标准轻量微圆角卡片；
    - **追番筛选弹窗**：由胶囊选项改造为极简纯文字高光与 `8dp` 微矩形。
  - **全链路单测覆盖与质量门禁**：新增 `PlayerQuickMenuLogicTest`（覆盖按键消费、定位算法保护与模型映射），加固 `LogViewerViewModelTest` 异步断言，`ktlintCheck` 与单元测试全部稳定通过。

- [x] **v0.5.4：个人页并发状态加固与大屏视觉语义优化（v0.5.4 已发布）**
  - **个人页协程与并发安全加固**：为 `PersonalViewModel.effect` 增加 16 容量缓冲与 `DROP_OLDEST` 防挂起保护；将历史、收藏、追番乐观删除由整表快照回滚重构为锚点级单条精准回滚，增加收藏夹与筛选上下文隔离；
  - **泳道卡片降噪与诊断增强**：精简 `SeeAllShelfCard` 移除卡片下方重复文案，保持大屏清爽对齐，并为横向泳道补充重复项去重 Debug 诊断日志；
  - **播放器覆盖层与网络工具规范化**：`PlayerErrorOverlay` 全面切换至 `MaterialTheme.colorScheme` 语义色，`PlayStateTips` 收敛为单一 `when` 互斥分支，`NetworkUtil` 收敛网络异常捕获范围并补全回归单测。

- [x] **v0.5.3：首页精选泳道「查看全部」大屏卡片与焦点孤岛消除（v0.5.3 已发布）**
  - **消除标题栏焦点死区**：彻底移除每条横向泳道标题栏右上角的独立文本按钮，解决 2D 焦点寻址中因垂直上下对齐被前后泳道卡片短路屏蔽的“焦点孤岛”死区，标题栏回归纯净纯文本大标题展示；
  - **泳道末端常驻大屏卡片**：每条横向泳道末尾固定常驻大屏等宽等高 `SeeAllShelfCard`（宽 260dp，圆环向右箭头），用户在任意泳道按右方向键滑到最末端即可直接获焦，按确认键秒级直达对应单分类海量瀑布流；
  - **焦点记忆与沉浸体验保障**：为末端卡片配置唯一恢复 Key（`FocusSaver`），从二级分类返回时焦点平稳恢复，且光标停留在末尾卡片时顶部常驻 Hero Showcase 看板平滑保持上一张视频展示，不闪烁、不黑屏；
  - **全链路测试与文档同步**：补齐纯 JVM 单元测试与 Compose UI 插桩测试，同步全面更新《使用指南.md》与《README.md》。

- [x] **v0.5.1：播放器容错加固与全局焦点安全治理（v0.5.1 已发布）**
  - **播放器异常统一处理与一键恢复卡片**：针对电视 Wi-Fi 弱网或 CDN 偶发异常，新增统一轻量错误恢复层（`PlayerErrorOverlay`），提供重新尝试、切换 CDN、画质降级重试等自救操作；
  - **大屏网络诊断弹窗**：新增 `NetworkDiagnosticsDialog`，支持一键探测网关、DNS 解析、B 站核心 API 与当前视频 CDN 节点连通性与 RTT 延迟；
  - **18 秒缓冲看门狗（Buffering Watchdog）**：缓冲超过 18 秒自动触发画质降级与 CDN 备用节点重试，解决长时间转圈问题；完善看门狗与播放器状态机竞态防护；
  - **全库焦点 API 规范化迁移**：全工程统一将 `requestFocus()` 迁移至 `safeRequestFocus()` 扩展，强制绑定 `Dispatchers.Main` 主线程调度与协程 `Job` 取消管理；
  - **遥控器光标防丢失兜底机制**：主内容区根容器接入 `Modifier.dpadFocusFallback`，彻底消除焦点逃逸丢失假死。

- [x] **v0.5.0：纯享观影增强套件（画面比例去黑边、杜比/Hi-Res徽标与声音菜单、播毕沉浸连播浮层）（v0.5.0 已发布）**
  - **画面比例与智能去黑边**：提供 6 档比例切换（原始/裁剪去黑边/拉伸铺满/16:9/4:3/21:9），OSD 快捷切换按钮与自定义快捷键联动；
  - **杜比 / Hi-Res 高品质音频与独立声音菜单**：独立「声音」Tab，提供详细音轨说明与高码率信息，顶部发光微徽标（Dolby Atmos / Hi-Res / 4K / 8K / HDR / Dolby Vision）；
  - **播毕沉浸连播倒计时微浮层**：播毕平滑滑入下一集推荐卡片，OK 键立即起播、返回键一键取消，加固外部取消防延后覆写；
  - **代码与架构规范治理**：重整 ControllerVideoInfo 签名与 KDoc 注释，视听徽标统一收敛至 BVColors 语义常量，补全对应回归单测。

- [x] **v0.4.14：播放器底部横向胶囊选集轨 (P2-2) 与首页 Hero Showcase 沉浸看板 (P2-3)（v0.4.14 已发布）**
  - **播放器底部悬浮横向选集微轨**：彻底废除左侧 300dp 垂直抽屉，重构为底部 20%~25% 悬浮微轨；统一 `BVShape.pill` 胶囊与 `focusInvertedColors` 高对比聚焦反色，支持当前集平滑横向居中对齐、50 集区间分组与嵌套 UGC 二级分 P 胶囊轨；
  - **首页 Hero Showcase 沉浸大看板**：在「精选」综合视图顶部引入电影级背景舞台（展示 960×540 WebP 高清海报、大标题、UP 主、播放量与弹幕等宽统计、双行简介），零焦点劫持，卡片 400ms 获焦防抖 + 300ms 交叉淡入淡出，LRU 按需补全简介；
  - **低配盒子降级保护与自由设置**：在设置中心提供「自动 / 始终开启 / 静态省流 / 关闭看板」4 种模式，低内存 TV 盒子自动降级保护，支持一键关闭恢复纯密集网格；
  - **代码与交互严谨加固**：修复选集轨切集索引重置、清理不可达按键与守卫、收敛关闭契约必传、加固内存检测与主线程缓存写入。

- [x] **v0.4.13：搜索页 3 列宽幅网格重构 (P2-1) 与 OTA 体验加固（v0.4.13 已发布）**
  - 搜索结果分类顶栏（视频 / 番剧 / 影视 / 用户 / 直播）全面接入全站统一的极简 `TopNav`，并在顶栏右侧槽位提供轻量筛选胶囊，解决卡片 `Menu` 键冲突；
  - 重构左右分栏比例（228dp 紧凑窄左栏 + 约 620dp 宽幅右栏）与扁平化 6×6 `SoftKeyboard`，彻底清除硬编码白色，完整适配深浅双主题；
  - 落地三阶段「双区接力协同」架构（`Discovery` 双列全站热搜榜 / `Suggests` 双列宽敞建议面板 / `Results` 宽幅网格 + 左栏下部关联建议接力列表）；
  - 升级视频、用户、直播为 3 列宽幅瀑布流网格（番剧/影视 4 列海报网格），单激活分类按需加载 + 150ms 防抖秒切；
  - 将未知来源设置跳转全链路移入 `Dispatchers.IO`、优化更新说明长文本遥控器连按目标累加与长按即时滚动、增加资产强校验与代码规范清理。

- [x] **v0.4.12：OTA 更新前展示详细更新说明与安装权限引导重塑（v0.4.12 已发布）**
  - 重构更新弹窗为 620dp 大屏宽卡片，清晰呈现版本对比、体积与日期，支持遥控器方向键上下平滑滚动阅读；
  - 移除下载完成后的前置权限拦截弹窗，未授权时直接调用已验证可用的 `ACTION_VIEW` 调起系统安装器，修复「点去开启权限无反应、点直接尝试能安装」的问题；
  - 增加定制 TV 系统设置空壳秒退 800ms 检测与应用详情/系统设置自动降级跳转；
  - 建立 `CHANGELOG.md` 与 Release 发布流水线自动提取机制，并新增关于页「版本更新说明」随时查阅入口。

- [x] **v0.4.11：OTA 在线升级全链路验证版（v0.4.11 已发布）**
  - 递增版本至 0.4.11（versionCode 193），用于在已安装 v0.4.10 的大屏真机上端到端验证 OTA 自动更新；
  - 验证 GitHub API + ghfast 代理下载、未知来源安装权限自动引导弹窗、PackageInstaller 安装与免 U 盘公共目录备份。

- [x] **v0.4.10：OTA 流程线程模型加固与交互细节优化（v0.4.10 已发布）**
  - 将 Failure 兜底安装与文件检查全链路调度至 Dispatchers.IO，彻底杜绝低配大屏 ANR；
  - 固化 installEvents 调用顺序契约并增加回归单测，防范 replay=1 陈旧事件重放；
  - 增加触发安装时的文件大小/路径审计日志，优化 SessionResult 分支代码可读性；
  - 优化弹窗关闭文案为「关闭（安装将继续）」，提升后台安装过程中的用户体验。

- [x] **v0.4.9：重构 OTA 更新架构解决雷鸟/TCL等系统卡死（v0.4.9 已发布）**
  - 引入 PackageInstaller Session 流式安装与广播回调监听，主动获知安装器拉起与失败状态；
  - 恢复 REQUEST_INSTALL_PACKAGES 权限声明并增加未知来源设置多级 Fallback 跳转；
  - 下载完成后自动导出 APK 副本至电视公共 Download 目录，支持免 U 盘一键直装；
  - UpdateDialog 引入 8 秒超时看门狗与退出能力，彻底消除「无路可逃」假死界面。

- [x] **P0：基础视觉规范收敛（v0.3.0 已发布）**
  - 全局 Shape Tokens 收敛（8/12/16/Pill）；
  - 双主题高对比色板收敛（深色深邃黑 vs 浅色冷瓷白）；
  - 字重提升与等宽时间码规范（`tnum`）。
- [x] **P1：焦点交互与微质感提升（v0.4.0 已发布）**
  - 全局 1.06x 物理卡片弹性缩放；
  - 双主题自适应获焦动效（深色 Neon Glow 辉光 / 浅色 Drop Shadow 环境阴影与高对比描边）；
  - 全仓清理硬编码暗色颜色（解决浅色主题下文字与抽屉发灰/隐形）。
- [x] **v0.4.1：全站顶部导航栏统一重构（v0.4.1 已发布）**
  - 彻底去除胶囊（Pill）背景框与彩色粗边框；
  - 采用全站统一的「极简文字 + 底部滑动单色黑白指示条」；
  - 统一靠左对齐（24dp）排版，零几何缩放平稳字阶对比；
  - 全量覆盖主页、个人页、分区页（UGC）、番剧页（PGC）。
- [x] **v0.4.2：关于菜单开源地址修正与统一发布签名固化（v0.4.2 已发布）**
  - 内置永久发布密钥 `btv-release.jks`，彻底解决自更新签名不匹配问题；
  - 修正设置中心「关于」页面的项目开源地址为 `https://github.com/wwwtwww/BTV` 并优化前缀说明；
  - 全面清理仓库中残留的旧项目与模板引用。
- [x] **v0.4.3：应用内更新存储与权限底层健壮性增强（v0.4.3 已发布）**
  - 将更新包存储迁移至外部公共应用缓存（`externalCacheDir`），避免旧进程被系统杀死时 FileProvider 管道与私有存储权限断开；
  - 为更新 APK 补充全局可读标志（`file.setReadable(true, false)`）；
  - 显式通过 `grantUriPermission` 为系统包安装器底层服务授予跨进程读取权限；
  - 完善 `file_paths.xml` 声明 `<external-cache-path>` 与 `<external-files-path>`。
- [x] **v0.4.4：OTA 在线更新验证版（v0.4.4 已发布）**
  - 纯版本号递增至 0.4.4，用于端到端验证在电视端通过 v0.4.3 执行 OTA 应用内无缝更新覆盖。
- [x] **v0.4.5：发布签名安全加固与密钥轮换门禁（v0.4.5 已发布）**
  - 从仓库移出硬编码 release 签名密钥至 GitHub Secrets，引入 apksigner 与 versionCode 门禁；
  - 完善 docs/SECURITY_SIGNING_ROTATION.md 规范。
- [x] **v0.4.6：全面对齐原版 newBV 纯粹 OTA 机制（v0.4.6 已发布）**
  - 彻底移除 `revokeUriPermission`，修复调起安装器后权限被提前撤销导致系统读取失败的致命 Bug；
  - 彻底移除 `AndroidManifest.xml` 中的 `REQUEST_INSTALL_PACKAGES`，避免国产 TV 系统因缺失 AppOps 授权界面触发安全拦截；
  - 彻底收敛更新包至内部私有目录 `cacheDir/ota_packages`，彻底规避 Android 10/11+ 外部存储沙盒限制；
  - 恢复纯粹的隐式系统安装器调用机制。
- [x] **v0.4.7：全新大屏专属经典粉蓝应用图标与 Leanback Banner 横幅（v0.4.7 已发布）**
  - 采用全新方案 4：经典 B 站粉蓝对角流光渐变背景与呆萌小电视造型；
  - 重新渲染全密度 `mipmap` 正方形 Squircle 与圆形 Round 应用图标；
  - 重新设计并生成 16:9 Leanback 电视横幅 `ic_banner.png` (640x360)；
  - 修正 Adaptive Icon 默认底色为 `#FB7299`，杜绝电视桌面上出现多余暗黑边框。
- [x] **v0.4.8：CI 自动化构建测试全线打通与单元测试死循环根治（v0.4.8 已发布）**
  - 修复 `PlayerViewModelTest` 分 P 详情逻辑中的死循环问题：彻底移除在 `runTest` 虚拟时间调度下会无休止推进的后台无限轮询协程（`initSession`），改为确定性的无副作用状态注入（`updateUiState`），彻底消除 CI 卡死超时问题；
  - 修复连续播放单测状态机前置分 P 上下文，同步清理与多集画质继承机制相冲突的过时测试；
  - CI `Build BTV APK` 与全量测试套件（250+ Tasks）实现 3 分钟内 100% 稳定绿标通过。
- [x] **P2：大屏高级布局革新（P2-1 搜索宽网格 / P2-2 底部胶囊选集轨 / P2-3 首页 Hero 沉浸看板已全部完成，待统一发版）**
  - 三大核心大屏视觉与交互特性均已落地并通过验证。

---

## 🎨 大屏 UI/UX 优化 P2 阶段（高级大屏布局革新）

> **预估总工作量**：约 6.5 ~ 8.5 人天  
> **涉及核心模块**：`app/src/main/kotlin/dev/frost819/newbv/app/ui/` (`screen/search`, `component/player`, `screen/home`) + `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/` (`player`, `home`)

---

### 1. [已完成] 搜索页紧凑键盘与宽网格（3 列卡片）+ TopNav 分类栏统一

- **优先级**：P2-1（已完成）
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/TopNav.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/search/SoftKeyboard.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/search/SearchKeyword.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/search/UpCard.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/search/SearchResultFilter.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/search/SearchInputContent.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/search/SearchInputViewModel.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/search/SearchResultViewModel.kt`

---

### 2. [已完成] 播放器底部横向胶囊选集轨

- **优先级**：P2-2（已完成）
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/EpisodeTrackModel.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/VideoListController.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/ControllerVideoInfo.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/VideoPlayerController.kt`
  - `app/src/main/res/drawable/playlist_play_24px.xml`

#### 落地摘要
1. **形态蜕变**：废除原左侧 300dp 全高竖向抽屉，重构为底部悬浮横向胶囊微轨（占屏高度约 20%~25%，半透明黑夜渐变遮罩），视频背景持续播放且不遮挡主体画面；
2. **胶囊交互与居中滚动**：统一采用 `BVShape.pill` 胶囊，支持 `focusInvertedColors` 高对比聚焦反色与当前播放集徽标高亮；呼出时自动通过 `animateScrollToItem` 横向平滑居中对齐并落焦；
3. **超长分组与合集子分 P 协同**：超过 50 集/分 P 自动生成 `1-50`、`51-100` 等横向区间分组胶囊；单视频多 P 直接展平至主轨，UGC 合集内含多 P 时按需在下方展开紧凑二级分 P 胶囊轨；
4. **遥控器与 OSD 协同**：将 OSD 按钮拆分为独立的「选集」与「相关推荐」（补齐番剧 PGC 的 OSD 选集入口），支持主画面 `DPAD_UP` 呼出选集（单集轻提示）、OSD 操作栏 `DPAD_DOWN` 展开选集、顶行 `DPAD_UP` / `Back` 收起选集以及 `Menu` 键互斥切换设置面板。

---

### 3. [已完成] 首页 Hero Showcase 沉浸看板

- **优先级**：P2-3（已完成）
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/home/HomeHeroBanner.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/home/HomeShelvesContent.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/home/HomeShelfRow.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/home/HomeContent.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/home/HomeViewModel.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/settings/content/UISetting.kt`
  - `bili-api/src/main/kotlin/dev/frost819/newbv/biliapi/entity/home/VideoBriefMeta.kt`
  - `data/src/main/kotlin/dev/frost819/newbv/data/datastore/PrefEnums.kt`

#### 落地摘要
1. **顶部常驻联动看板（零焦点劫持）**：在主页「精选」综合视图顶部引入沉浸式 `HomeHeroBanner`（展示高清横版海报、泳道/分区/推荐理由胶囊、大标题、UP 主与等宽统计、双行简介），焦点保持在下方横向泳道卡片与顶部导航之间一键直达，完美适配深浅双主题；
2. **400ms 获焦防抖 + 300ms 丝滑过渡 + LRU 按需补全**：卡片获焦执行严格 400ms 防抖取消机制，停留满 400ms 后以 300ms 交叉淡入淡出切换海报与正文，并通过 48 条容量 LRU 缓存按需补全缺失的分区名与视频简介；
3. **显存保护与低配设备自动降级**：海报统一裁剪为 `960x540` WebP 并启用 `RGB_565` 限流解码，进入播放器或退后台（`ON_STOP`）即时卸载纹理释放显存；支持低内存设备自动切换为无大图纹理/无双缓冲动画的静态省流看板，并在「设置 -> 界面设置 -> 首页沉浸看板」提供四档模式开关。

---

## 🛠️ 其他长期技术与体验优化待办

- [x] **[已在 v0.5.0 完成] 播放器多音轨切换与独立声音菜单**：已在播放器 OSD 落地独立「声音」Tab，支持杜比全景声 (Dolby Atmos)、Hi-Res 无损音质及 192K/132K/64K 码率列表选择，并在顶部高亮展示对应高级视听发光微徽标。
1. **离线缓存大屏适配**：
   - 考虑 TV 外接 U 盘/移动硬盘场景下的分片离线下载与本地视频管理。
2. **局域网 DLNA / 投屏接收端**：
   - 探索接收手机端 B 站 App 投屏并调用原生 Media3 渲染。
