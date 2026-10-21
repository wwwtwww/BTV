# BTV 项目待办清单 (TODO)

> 本文档用于记录暂缓实现、待排期或长期规划的功能与重构事项。
> 配套文档：[PRD](docs/PRD.md) | [开发计划](docs/开发计划.md) | [UI 优化建议](docs/UI优化设计建议.md)

---

## 📌 当前进行与完成状态概览

- [x] **P0：基础视觉规范收敛（v0.3.0 已发布）**
  - 全局 Shape Tokens 收敛（8/12/16/Pill）；
  - 双主题高对比色板收敛（深色深邃黑 vs 浅色冷瓷白）；
  - 字重提升与等宽时间码规范（`tnum`）。
- [x] **P1：焦点交互与微质感提升（v0.4.0 已发布）**
  - 全局 1.06x 物理卡片弹性缩放；
  - 双主题自适应获焦动效（深色 Neon Glow 辉光 / 浅色 Drop Shadow 环境阴影与高对比描边）；
  - 全仓清理硬编码暗色颜色（解决浅色主题下文字与抽屉发灰/隐形）。
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
