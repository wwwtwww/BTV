# BTV 签名证书安全说明与 v0.5.0 证书轮换方案

## 1. 历史背景与现状

在 `v0.4.1 ~ v0.4.4` 版本迭代期间，`btv-release.jks` 曾被提交至 git 仓库并由 `app/build.gradle.kts` 明文读取。这导致任何获取仓库历史者均拥有该签名身份，存在被恶意伪造相同签名 APK 进行同签名覆盖安装的潜在风险。

## 2. 当前缓解措施（v0.4.x 系列）

1. **工作区与构建脚本彻底净化**：
   - 物理删除 `btv-release.jks`，剔除 `.gitignore` 中的例外豁免；
   - 彻底删除 `app/build.gradle.kts` 中的明文口令硬编码；
   - 签名配置强制收敛至环境变量或 `signing.properties`（禁止提交 git），本地开发默认产出未签名/Debug 包，CI 发布强制要求注入 `SIGNING_PROPERTIES` 与 `SIGN_KEY`；
2. **发布校验门禁**：
   - `.github/workflows/release.yml` 增加 `apksigner verify` 指纹核验与 `versionCode` 单调递增校验，杜绝意外签发非预期私钥版本。

## 3. v0.5.0 证书轮换规划（Key Rotation）

由于 Android 系统在无特定密钥轮换机制时，若签名证书变更会以 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 拒绝覆盖安装，因此采取分阶段迁移策略：

### 阶段一：v0.4.x 末尾维护版本
- 保持当前证书签发，确保 v0.4.x 存量用户具备连续的 OTA 升级链；
- 在更新弹窗逻辑中加入大版本跨证书检测：当检测到目标版本为 `>= v0.5.0` 时，展示专门文案引导：“**检测到全新大版本 v0.5.0，由于签名安全证书加固轮换，本次更新需手动重新安装**”，引导用户直接在浏览器下载最新 APK 进行全新安装。

### 阶段二：v0.5.0 正式上线
- 在 GitHub Actions Secrets 中注入全新生成、不可公开的 `SIGNING_PROPERTIES` 与 `SIGN_KEY`；
- 正式废弃旧证书。
- **旧证书离线归档与封存策略**：
  在存量 v0.4.x 用户全量迁移完成前，原 `btv-release.jks` 旧私钥不得随意物理销毁。应由仓库核心维护者将其导出并置于离线加密介质（如受密码保护的离线安全硬件或严格访问受限的独立密钥管理库）中进行冷备份，以备在紧急情况下签发 `v0.4.x` 存量系列的紧急热修复（Hotfix）包；在 `v0.4.x` 维护周期彻底终结后，转入只读安全归档状态。

### 阶段三：git 历史清理（可选/协同重写）及对 versionCode / OTA 的影响评估
若团队决定对公开仓库历史进行深度净化，可使用 `git-filter-repo` 彻底抹除旧提交中的 jks 文件：
```bash
# 安装 git-filter-repo 后执行
git filter-repo --path btv-release.jks --invert-paths
```

> [!CAUTION]
> **执行 `filter-repo` 对 `versionCode` 与 OTA 升级链的关键影响：**
> 1. **提交总数变动风险**：项目默认使用 `git rev-list --count HEAD` 计算派生 `versionCode`。若执行 `git filter-repo` 删除了包含私钥的提交节点或重写了历史拓扑，提交计数可能骤降（例如低于已发布版本 `v0.4.4` 的 `versionCode: 183`）。
> 2. **CI 发布门禁拦截**：`.github/workflows/release.yml` 包含强校验规则，要求待发布的 `versionCode` 必须严格大于上一 release 资产的 `versionCode`。提交数下降会导致 CI 直接以 `Current versionCode is not greater than latest release` 阻断发布流程。
> 3. **存量 OTA 链断裂**：如果以低于或等于 183 的 `versionCode` 产出更新包，客户端由于版本号比较机制无法正常触发应用内升级检测。
> 4. **应对方案**：执行 `git filter-repo` 重写历史后，发布流程**必须**通过环境变量显式注入 `BTV_VERSION_CODE`（例如 `BTV_VERSION_CODE=184` 或更高），或者修改 `AppConfiguration.kt` 中的基础偏移量，确保生成的版本号单调递增，平稳衔接存量 OTA 链。
