# BTV 项目待办清单 (TODO)

> 本文档用于记录暂缓实现、待排期或长期规划的功能与重构事项。
> 配套文档：[PRD](docs/PRD.md) | [开发计划](docs/开发计划.md) | [UI 优化建议](docs/UI优化设计建议.md) | [真机测试指南](docs/真机测试与验收指南.md)

---

## 📌 当前进行与完成状态概览

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
- [ ] **P2：大屏高级布局革新（暂缓实现，记录于下方）**
  - 暂缓原因：改动范围广、涉及播放器控制器重构与大屏全局看板生命周期管理，后续建议作为单独的大特性分批立项推进。

---

## 🎨 大屏 UI/UX 优化 P2 阶段（高级大屏布局革新）

> **预估总工作量**：约 6.5 ~ 8.5 人天  
> **涉及核心模块**：`app/src/main/kotlin/dev/frost819/newbv/app/ui/` (`screen/search`, `component/player`, `screen/home`) + `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/` (`player`, `home`)

---

### 1. 搜索页紧凑键盘与宽网格（3 列卡片）

- **优先级**：P2-1
- **预估工期**：1.5 ~ 2 人天
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/search/SearchInputContent.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/search/SearchScreen.kt`

#### 现状分析与痛点
- 当前左侧全拼/首字母九宫格与全键盘占据屏幕宽度的近 40%，空间冗余度高；
- 右侧搜索结果仅为 2 列卡片，每行横向拉得过宽，信息密度偏低，纵向浏览翻页负担大。

#### 重构设计方案
1. **分栏比例优化**：
   - 键盘宽度由原先的 40% 压缩至 **28% ~ 30%**；
   - 键盘按键适当紧凑化，保持舒适的 10-foot 大屏遥控器命中率。
2. **结果网格升级**：
   - 右侧视频卡片网格由 2 列扩展为 **3 列宽幅瀑布流网格**；
   - 卡片宽高比保持 16:9 黄金封面，文字标题与 UP 主角标采用紧凑排版。
3. **遥控器寻焦协同（Focus Navigation）**：
   - 从键盘向右按下 `DPAD_RIGHT` 时，自然寻焦至右侧视口内第一行最近的卡片；
   - 从右侧网格最左列按下 `DPAD_LEFT` 时，平滑返回键盘当前激活的按键或功能区。

---

### 2. 播放器底部横向胶囊选集轨

- **优先级**：P2-2
- **预估工期**：2 ~ 3 人天
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/VideoListController.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/player/VideoPlayerController.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/player/PlayerViewModel.kt`

#### 现状分析与痛点
- 当前选集列表为左侧 300dp 的大竖向抽屉面板，呼出时严重遮挡视频画面超 1/3；
- 与 YouTube TV 和主流大屏 App（如 Netflix、Apple TV）底推横向轨道体验不符。

#### 重构设计方案
1. **形态重构**：
   - 废除左侧大遮挡抽屉，改建为**底部悬浮水平微轨（Horizontal Capsule Track）**；
   - 高度控制在屏幕下方的 20%~25%，背景采用毛玻璃或黑色半透渐变遮罩，最大程度保留主画面播放。
2. **胶囊选集项交互**：
   - 采用标准 `BVShape.pill` 或 `BVShape.medium` 胶囊，高亮显示「当前播放中」集数；
   - 遥控器呼出时，播放列表自动横向平滑居中滚动至当前播放集（`scrollToItem(currentIndex)`）。
3. **控制器协同与按键映射**：
   - 在主播放视图按下 `DPAD_DOWN` / 菜单键呼出选集微轨；
   - 针对合集/多 P 超过 50 集的情况，设计分组分页胶囊（如 `1-50`、`51-100`），避免单行列表过长。

---

### 3. 首页 Hero Showcase 沉浸看板

- **优先级**：P2-3
- **预估工期**：3 ~ 3.5 人天
- **涉及文件**：
  - `app/src/main/kotlin/dev/frost819/newbv/app/ui/screen/home/HomeScreen.kt`
  - 新建 `app/src/main/kotlin/dev/frost819/newbv/app/ui/component/home/HomeHeroBanner.kt`
  - `app/src/main/kotlin/dev/frost819/newbv/app/viewmodel/home/HomeViewModel.kt`

#### 现状分析与痛点
- 当前首页顶部仅有 Filter Chips，缺少顶级 TV 客户端的品牌感与沉浸式大视觉抓手；
- 优质推荐视频的海报与背景氛围未能在大屏上充分施展。

#### 重构设计方案
1. **视觉看板结构**：
   - 首页顶部呈现沉浸式全屏渐变 Hero 看板：左侧展示超大标题、UP 主、分区标签与简介，背景投射当前精选视频的高清横版海报；
   - 底部与下方视频泳道之间采用丝滑的从黑到透渐变过渡。
2. **防抖与联动机制（Debounce）**：
   - 遥控器在下方泳道快速游走浏览时，**必须引入 400ms 获焦防抖**，避免背景频繁无意义切换造成闪烁与眩晕；
   - 焦点停留超过 400ms 后，触发平滑交叉淡入淡出（Crossfade 300ms）更新背景看板。
3. **低配大屏设备性能保护**：
   - TV 盒子内存敏感，必须对海报 Bitmap 严格复用，离开首页或视频播放时及时释放纹理显存；
   - 在低配电视模式（或检测到低内存设备时）提供静音/静态看板降级模式。

---

## 🛠️ 其他长期技术与体验优化待办

1. **播放器多音轨切换与无缝多码率**：
   - 支持杜比全景声、Hi-Res 无损音轨切换 UI；
   - Media3 码率平滑切换体验调优。
2. **离线缓存大屏适配**：
   - 考虑 TV 外接 U 盘/存储卡场景下的分片离线下载管理。
3. **局域网 DLNA / 投屏接收端**：
   - 探索接收手机端 B 站 App 投屏并调用原生 Media3 渲染。
