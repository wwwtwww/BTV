# Android TV (雷鸟/TCL等) OTA 升级卡死排查与解决方案

> 本文档用于记录从旧版本（如 `v0.4.6` 或原版 `newBV`）进行应用内 OTA 升级时，卡在「安装中 / 请坐和放宽」无响应界面的问题根因、当前设备的应急处理方式，以及代码层面的彻底根治架构。

---

## 📌 一、现象与问题描述

在 Android TV（典型设备如雷鸟 FFALCON、TCL、海信等搭载 Android 11+ / API 30+ 的智能电视或电视盒子）上：
1. 打开应用进入 **「设置」** -> **「关于」** -> **「检查更新」**；
2. 进度条正常走完至 100%，随后弹窗状态变为 **「安装中 / 请坐和放宽」**；
3. 底部按钮变为禁用状态且显示「你已经无路可逃！」，电视屏幕**没有任何反应（无系统安装弹窗、无闪烁、无报错提示）**；
4. 界面永久停留在该状态，导致 OTA 在线更新流程中断。

---

## 🔍 二、问题根因深入分析

经过对 Android 底层机制及 Android TV 定制 ROM（特别是雷鸟/TCL 固件）的排查，该问题由以下 **3 个因素叠加** 引起：

### 1. 缺少 `REQUEST_INSTALL_PACKAGES` 权限与 ROM 静默销毁机制
- Android 8.0（API 26）起，应用内调起系统安装器覆盖自身必须声明 `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`，且需获得系统的「允许安装未知应用」授权。
- 在 `v0.4.6` 与原版 `newBV` 中，Manifest 移除了该权限声明，且直接调用了 `context.startActivity(Intent(ACTION_VIEW))`。
- 在标准原生 Android 系统上，未授权时通常会弹出系统权限提示；但在雷鸟（FFALCON）/ TCL 等定制 TV ROM 上，系统内置的 `PackageInstallerActivity` 启动时进行权限校验，一旦检测到发起调用方没有声明该权限，**不会抛出任何 Java 异常，也不会弹出任何权限提示或安装对话框，而是在 `onCreate()` 中直接调用 `finish()` 静默销毁**。

### 2. FileProvider 跨进程 URI 读取权限受阻
- 原实现将 APK 下载在应用的内部私有目录 `context.cacheDir` 中，通过 `FileProvider` 生成 `content://` URI。
- 部分 TV ROM 在二次转发拉起系统后台安装进程（如 `com.android.packageinstaller` 或定制应用安装器服务）时，仅依赖 Intent 级别的 `FLAG_GRANT_READ_URI_PERMISSION` 可能会丢失对该 URI 的读取权限，导致安装器读取内部流失败而直接静默退出。

### 3. UI 状态机假死设计
- 原 `UpdateDialog` 在调用 `startActivity` 之前，将状态设为 `UpdateStatus.Installing`；
- 弹窗的关闭按钮在 `Downloading` 与 `Installing` 状态下均被禁用（`enabled = false`，文案为“你已经无路可逃！”）；
- 同时缺乏任何异步回调监听与超时看门狗保护。当底层系统安装器静默退出后，Compose UI 无法感知外部失败，造成用户界面假死。

---

## 🛠️ 三、旧版设备立即可用的应急升级办法

对于目前电视上已运行 `v0.4.6` 或原版 `newBV` 的设备，可通过以下任意一种途径完成升级并摆脱卡死：

### 方法 1：电视系统设置开启未知应用安装权限（免 U 盘）
1. 使用电视遥控器进入电视自带的 **【系统设置】**；
2. 依次进入 **【应用】 -> 【安全与限制】（或【特殊应用权限】 -> 【安装未知应用】）**；
3. 在应用列表中找到 **BTV**（或 **newBV**），将其开关开启为 **「允许」**；
4. 返回 BTV 应用的 **「设置 -> 关于 -> 检查更新」**。由于此前 APK 已完整下载并缓存在本地，点击更新即可直接秒起系统安装器。

### 方法 2：电脑无线 ADB 一键授权或安装（开发者推荐）
确保电脑与电视处于同一局域网并开启电视的 ADB 调试：
```powershell
# 1. 连接电视
adb connect <电视IP>:5555

# 方式 A：强行授予 BTV / newBV 应用内安装权限（授权后可在电视内直接 OTA）
adb shell appops set dev.frost819.btv REQUEST_INSTALL_PACKAGES allow
adb shell appops set dev.frost819.newbv REQUEST_INSTALL_PACKAGES allow

# 方式 B：直接覆盖安装新版本 APK
adb install -r BTV_xxx_release.apk
```

### 方法 3：U 盘传统覆盖安装
从 GitHub Releases 下载最新 APK 拷贝至 U 盘，插入电视通过自带文件管理器手动覆盖安装一次。
> 升级到包含本次修复的新版本后，后续即可享受完整的自动导出与免 U 盘直装。

---

## 🚀 四、代码工程彻底根治方案

在 BTV 代码库中，已落地了全链路防护与双轨安装架构：

### 1. 恢复权限声明与安装广播接收器
- 在 `app/src/main/AndroidManifest.xml` 中补齐关键权限与 Android 11+ 包可见性：
  ```xml
  <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />

  <queries>
      <intent>
          <action android:name="android.settings.MANAGE_UNKNOWN_APP_SOURCES" />
          <data android:scheme="package" />
      </intent>
      <intent>
          <action android:name="android.intent.action.VIEW" />
          <data android:mimeType="application/vnd.android.package-archive" />
      </intent>
  </queries>
  ```
- 注册 `OtaInstallResultReceiver` 广播组件，接收底层安装状态异步回调。

### 2. 双轨安装引擎 + 公共 Download 目录同步导出 (`OtaUpdateInstaller`)
新增 `OtaUpdateInstaller.kt`，包含三大核心能力：
- **PackageInstaller Session 流式安装（主轨）**：通过 `PackageInstaller.createSession()` 直接向底层系统写入 APK 字节流，精准捕获 `STATUS_PENDING_USER_ACTION`、`STATUS_SUCCESS` 与 `STATUS_FAILURE_BLOCKED` 广播事件，彻底解决无回调盲等；
- **显式 URI 授权兼容安装（副轨）**：在调用传统 `ACTION_VIEW` 时，显式遍历系统所有候选 PackageInstaller Activity，并使用 `context.grantUriPermission()` 逐一授权，确保跨进程流安全读取；
- **免 U 盘公共 Download 目录同步备份**：下载完成后，利用 Android 10+ 的 `MediaStore.Downloads`（或公共外部存储）将 APK 同步复制一份至电视公共 **`Download/`** 目录。即使极端定制 TV ROM 彻底禁止第三方应用内发起安装，用户也无需 U 盘，直接打开电视自带的 **【文件管理器 / 应用助手 -> 下载】** 即可直接点击一键安装；
- **多级安全跳转设置**：提供针对未知来源权限设置页的多级 Fallback 跳转（按包名跳转 -> 通用未知来源设置 -> 安全设置 -> 主设置）。

### 3. 更新弹窗状态机重构与 8 秒看门狗守卫 (`UpdateDialog`)
- **废除假死逻辑**：弹窗在任何状态下均允许通过遥控器返回键或点击“关闭”按钮退出，杜绝“无路可逃”；
- **8 秒看门狗守卫**：启动安装后开启 8 秒计时器，若电视系统因策略静默丢弃安装弹窗，自动将状态切换为 `InstallError`，并动态展示：
  - 【兼容模式重试】按钮；
  - 【去开启安装权限】按钮（从系统设置授权返回后自动继续安装）；
  - 显式展示已导出的本地公共存储路径（例如 `Download/BTV_xxx.apk`），告知用户可使用电视自带文件管理器免 U 盘一键安装。

### 4. 自动化测试验证
在 `OtaUpdateInstallerTest` 与 `UpdateStatusTest` 中补全了针对以下维度的单元测试：
- 未知来源权限检查在 Android 8.0+ 与低版本间的行为；
- 系统设置跳转失败时的自动降级链路；
- `PackageInstaller` 广播结果在 Kotlin Flow 中的异步分发（`Success` / `Failure`）；
- 空文件防护与异常处理；
- 代码已全量通过 `ktlintMainSourceSetCheck` 与单元测试。
