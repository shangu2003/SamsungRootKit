# Samsung Root Kit — 离线版 Android App（DF 链首选 · SRK 链回退 · DFE 持久化）

**版本 1.0.0**（versionCode 100，2026-10-14 GitHub 首发）·

完全离线的 Samsung Galaxy 临时 Root 工具箱。
内置两套互相独立的提权链路 —— **DF 链首选、SRK 链自动回退**；DF 链为快速提权，仅 SRK 链成功后进入 **DFReroot（DFE）持久化配置**。

> ⚠️ 仅供学习与安全研究使用，可能会导致 KNOX 熔断、保修失效，请先完整阅读文末免责声明。

---

## 双链提权架构（本工具的核心设计）

| 链路 | 提权方式 | 定位 | 提权成功后 |
|---|---|---|---|
| **DF 链（首选）** | DirtyFrag 内核漏洞引擎 | 一键快速提权，最快路径 | 完成（不涉及 DFE） |
| **SRK 链（回退）** | CVE-2026-43499 + Shizuku（ADB 级 shell，UID 2000） | DF 链失败 / 不适用时自动接管 | 引导进入 DFE 持久化配置 |

**标准流程**：

```
① DF 链首选快速提权（DirtyFrag）
   │
   ├─ 成功 ──▶ 完成（快速提权，不涉及 DFReroot / 持久化配置）
   │
   └─ 失败或固件不适用
        │  连续 3 次失败 ⇒ 3-strike 自动关停 DF 链（防无限重启循环，可手动重开）
        ▼
② 自动回退 SRK 链（CVE-2026-43499）
   │  · payload 按固件适配：内置 21 板名 / 94 条固件配置 / 85 份 payload
   │  · 不在列表内的固件，可经 SAF 自行导入 payload + root helper
   │
   └─ 成功 ──▶ ③ 引导进入 DFReroot（DFE）持久化配置
```

两条链均提供 **KernelSU / KernelSU-Next** 两个管理器变体，选定后重启续跑沿用同一变体，不会串版本：

| 变体 | 内核模块名 | 管理器包名 |
|---|---|---|
| 🔴 **KernelSU** | `kernelsu` | `me.weishu.kernelsu` |
| 🔵 **KernelSU-Next** | `kernelsu_next` | `com.rifsxd.ksunext` |

---

## 功能

- **完全离线**：payload、ksud、root helper、Manager APK 全部打包在 App 内，运行时不需要任何网络
- **双链协同**：DF 链（DirtyFrag）首选快速提权，失败自动回退 SRK 链（CVE-2026-43499）；SRK 链成功后引导进入 DFReroot（DFE）持久化配置
- **Shizuku 权限集成**：SRK 链通过 Shizuku 获取 shell（UID=2000）权限，等价于 `adb shell`，无需电脑
- **完整复刻 run_root.sh 流程**：推送文件 → warmup → 触发 exploit → 验证 root → late-load 加载 KernelSU → 安装 Manager
- **自动机型匹配**：读取 `ro.build.fingerprint` 自动匹配对应 payload；KMI 自动检测选择 root helper
- **固件级 payload 适配**：SRK 链 payload 按固件逐条适配（94 条配置 / 85 份 payload），自选固件支持 SAF 导入
- **资产完整性校验**：推送前按内置 `SHA256SUMS.txt` 逐文件校验 SHA-256，损坏或被替换的二进制一律拒绝执行
- **3-strike 失败保护**：DF 链连续失败 3 次自动关停总开关并回退 SRK 链，防止无限重启循环；成功一次即清零计数，OTA 升级后自动解锁
- **提权耗时统计**：结果页显示「400 次内核预热结束 → root 提权成功」的总耗时

---

## 支持机型

共 **21 个板名 / 94 条固件配置**，其中 **85 条**带独立 payload（其余 9 条经 `aliasOf` 复用别名指向的源 payload）。

| 系列 | 板名 | 机型 | 型号 | 固件条目 | KMI |
|---|---|---|---|---|---|
| **S23** | dm1q / dm2q / dm3q | Galaxy S23 / S23+ / S23 Ultra | SM-S9110 / SM-S9160 / SM-S9180 · S918U1 | 3 / 3 / 8 | android13-5.15 |
| **S24** | e1q / e2q / e3q | Galaxy S24 / S24+ / S24 Ultra | SM-S9210 / SM-S9260 / SM-S9280·B·U1 | 4 / 3 / 13 | android14-6.1 |
| **S25** | pa1q / pa2q / pa3q / psq | Galaxy S25 / S25+ / S25 Ultra / S25 Edge | SM-S9310 / S9360 / S9380·B·N·U / S9370 | 4 / 1 / 23 / 3 | android15-6.6 |
| **S26** | m1q / m2q / m3q | Galaxy S26 / S26+ / S26 Ultra | SM-S9420·U1 / S9470 / S9480 | 2 / 1 / 2 | android16-6.12 |
| **Z Flip** | b5q / b6q / b7q | Z Flip5 / Flip6 / Flip7 | SM-F7310·N·U / F7410·B·U / F7660 | 4 / 4 / 1 | 13-5.15 / 14-6.1 / 15-6.6 |
| **Z Fold** | q5q / q6q / q7q | Z Fold5 / Fold6 / Fold7 · W26 | SM-F9460·N / F9560·U / F9660·B·U1 · W9026 | 3 / 2 / 7 | 13-5.15 / 14-6.1 / 15-6.6 |
| **W 系列** | w9025 | Galaxy W25 | SM-W9025 | 2 | android14-6.1 |
| **平板** | gts9u | Galaxy Tab S9 Ultra | SM-X910 | 1 | android13-5.15 |

> KMI 决定「系列」：`5.15 → S23` / `6.1 → S24` / `6.6 → S25` / `6.12 → S26`。系列决定用哪一份 **root helper**；
> **ksud 不随系列分文件**，两个变体各用一份内嵌全部 KMI 的统一版（KSU → `ksud-samsung-multi-kdp`；Next → `ksud-samsung-multi-kdp-next`）。

---

## 使用前提

1. **安装 Shizuku**：从 https://shizuku.rikka.app/ 下载安装
2. **启动 Shizuku 服务**：
   - 方式一：无线调试启动（Android 11+，手机设置 → 开发者选项 → 无线调试）
   - 方式二：USB 连接电脑用 `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh` 启动
3. **授权本应用**：在 Shizuku 中授予本应用权限

> DF 链不依赖 Shizuku；SRK 链（含自选 payload 导入）需要 Shizuku 就绪。


## 工作原理

```
┌──────────────────────────────────────────────────────────┐
│                 Samsung Root Kit App (v1.0.0)            │
│                                                          │
│  ┌───────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │   UI 层   │  │  PayloadMgr  │  │  DfRootManager    │  │
│  │ (Compose) │  │ (提取/匹配)  │  │ (DF链编排+3strike)│  │
│  └───────────┘  └──────────────┘  └───────────────────┘  │
│        │              │                    │              │
│        ▼              ▼                    ▼              │
│  ┌────────────────────────────┐   ┌───────────────────┐  │
│  │ ① DF 链（首选）            │   │ ② SRK 链（回退）  │  │
│  │ DirtyFrag 引擎 libexp.so   │   │ RootExecutor      │  │
│  │ (快速提权)                 │   │ CVE-2026-43499    │  │
│  └─────────────┬──────────────┘   └─────────┬─────────┘  │
└────────────────┼────────────────────────────┼────────────┘
                 ▼                            ▼
       ┌────────────────────┐       ┌────────────────────┐
       │ DF 成功 → 完成     │       │ Shizuku (shell)    │
       │ (快速提权，不经DFE)│       │ UID=2000 = adb     │
       └────────────────────┘       └─────────┬──────────┘
                                              ▼
                                   ┌────────────────────┐
                                   │ SRK 成功 → DFE     │
                                   │ 持久化配置          │
                                   └────────────────────┘
```

### 执行流程（SRK 链，与 run_root.sh 一致）

1. **安全确认** → 用户确认风险
2. **Shizuku 检查** → 确保权限就绪
3. **变体 × 系列 判定** → 解析系列并写入日志（KSU / Next 均覆盖 S23–S26 全系列）
4. **机型匹配** → 自动读取 `ro.build.fingerprint` 匹配 payload
5. **KMI 检测** → 判定系列并选择对应系列的 root helper（ksud 用变体统一版）
6. **推送文件** → payload / root helper / ksud → `/data/local/tmp/`（推送前逐文件校验 SHA-256）
7. **chmod 755** → 设置可执行权限
8. **Warmup** → 400 次 `/system/bin/true`
9. **触发 Exploit** → `LD_PRELOAD=payload /system/bin/true`
10. **验证 Root** → `uid=0(root)` 确认，并统计提权耗时
11. **late-load 加载内核模块** → 先 **staging ksud**，再单次 `--late-load`（不重试）
12. **安装 Manager** → `pm install` 对应变体的 Manager APK（非关键路径，失败不影响 root 结果）
13. **进入持久化配置** → root 成功后引导配置 DFReroot 持久化（开机自启 / 自动软重启 / 变体）

## 内置资源

`assets/` 共 102+ 个文件，未压缩约 56 MB：

| 路径 | 内容 | 大小 |
|---|---|---|
| `payloads.list` | 设备元数据 JSON（94 条 targets） | 71 KB |
| `SHA256SUMS.txt` | 全部资产的 SHA-256 清单（98 条） | 10 KB |
| `payload/<固件>/<profileId>__payload` | 85 份漏洞载荷（按固件适配） | 7.38 MB |
| `dfreroot/` | 内嵌 DFReroot v2.2.0 集成 APK（持久化链路） | 11.8 MB |
| `dfroot/` | DF 链 Next 路包装脚本（`ksud_wrap.sh` / `ksud_next_wrap.sh`） | — |
| `tools/ksu_tools/` | KernelSU 全部物料（7 个文件） | 16.13 MB |
| `tools/ksu_next_tools/` | KernelSU-Next 全部物料（6 个文件） | 16.57 MB |

### KernelSU 物料（`tools/ksu_tools/`）

| 文件 | 说明 |
|---|---|
| `KernelSU_Manager_v3.3.0_32601.apk` | 管理器（10.28 MB） |
| `ksud-samsung-multi-kdp` | 统一多-KMI ksud（单文件内嵌 8 个 KMI 的 `kernelsu.ko`，含 `android16-6.12`） |
| `root/root-samsung-android{13-5.15,14-6.1,15-6.6,16-6.12}` | 4 份 root helper（按系列） |
| `patch_map.txt` | payload 补丁点记录 |

### KernelSU-Next 物料（`tools/ksu_next_tools/`）

| 文件 | 说明 |
|---|---|
| `KernelSU_Next_v3.4.0_33294.apk` | 管理器（11.31 MB） |
| `ksud-samsung-multi-kdp-next` | 统一多-KMI ksud（单文件内嵌 8 个 KMI 的 `kernelsu.ko`，含 `android16-6.12`） |
| `root/root-samsung-android{13-5.15,14-6.1,15-6.6,16-6.12}-next` | 4 份 root helper（按系列） |

### 原生层（`src/main/jni/`）

DF 链的漏洞利用引擎 `libexp.so` 由包内源码编译（`exp.c` + `elf_parser.c` + `libcxx.S` + `splicehelper.c`），
经 `.incbin` 内联 7 个预编译 `dirtyfrag-*.ko`（覆盖 android12-5.10 ~ android17-6.18 共 7 个 KMI）。
NDK 版本必须保持 `27.2.12479018` 与预编译 ko 一致。

## 技术细节

### 资产完整性校验

App 启动执行前会：

1. 解析 `assets/SHA256SUMS.txt`，得到「资产相对路径 → SHA-256」的受信清单；
2. 对最终选中的 payload 与 root helper **先读入内存并校验 SHA-256**，不一致立即抛 `SecurityException` 中止；
3. ksud 与 Manager APK 同样校验后才推送。

因此**任何被篡改或损坏的内置二进制都无法被推送到设备执行**。

### late-load staging（重要）

`ksud` 的 `late_load` 模块会把 `/data/local/tmp/.ksud-stage` 当作**已暂存的 ksud** 来消费（内部执行 `rename(".ksud-stage" → "/data/adb/ksud")`），失败即报：

```
Error: Failed to stage ksud
Caused by: 0: Failed to rename /data/local/tmp/.ksud-stage to /data/adb/ksud
           1: No such file or directory (os error 2)
```

但 **ksud 自己不会创建该文件**。包内 root helper 存在两种版本：

| root helper | 是否自带 staging | 说明 |
|---|---|---|
| 26 840 字节版（S23 / S24） | ✅ 有 `stage_ksud_copy()`，自己创建 `.ksud-stage` | 自洽 |
| 27 232 字节版（S25 / S26 / 部分 S23 固件） | ❌ 已移除 staging 逻辑，二进制内不含 `.ksud-stage` | **必须由调用方预先 stage** |

因此 App 在 `--late-load` **之前**，于 shell 层统一补一次 staging：

```bash
cp /data/local/tmp/ksud-selected /data/local/tmp/.ksud-stage
chmod 755 /data/local/tmp/.ksud-stage
```

该操作对自带 staging 的 26 840 版无副作用（`cp` 为幂等覆盖），失败仅告警不中断。
**KernelSU 与 KernelSU-Next 两条路径都执行此步骤**。

### 3-strike 失败保护（DF 链）

- DF 链**真的跑了且在本固件上失败**（rc=1/2/3）才计次；「已注入」（rc=9）与「链没跑」（rc=-1）不计
- 连续失败达 **3 次** ⇒ 自动关停 DF 链总开关，主页按钮回退 SRK 链入口（防无限重启循环）
- **fail-open**：计次写盘失败不关总开关；**成功即清零**（含开机自启成功路径）
- 记录与固件绑定（`Build.VERSION.INCREMENTAL`），OTA 后自动解锁；存储使用 DE（设备保护）区，锁屏期写入不错配

### 两套物料完全隔离

按变体分别取用，路径互不交叉：

| 取用项 | KernelSU | KernelSU-Next |
|---|---|---|
| ksud | `tools/ksu_tools/ksud-samsung-multi-kdp` | `tools/ksu_next_tools/ksud-samsung-multi-kdp-next` |
| root helper | `tools/ksu_tools/root/root-samsung-<kmi>` | `tools/ksu_next_tools/root/root-samsung-<kmi>-next` |
| Manager APK | `tools/ksu_tools/KernelSU_Manager_*.apk` | `tools/ksu_next_tools/KernelSU_Next_*.apk` |
| 内核模块判据 | `kernelsu` / `samsung_kernelsu` / `ksu` | `kernelsu_next`（优先判定） |

> root helper 按「**系列（KMI）+ 变体**」共享存放；其中 KernelSU-Next 的 S24 / S25 / S26 root helper 与 KernelSU 版**内容相同**（同哈希，按变体各自独立命名存放）。

### Shizuku 权限

通过 `Shizuku.newProcess()` 执行 shell 命令，权限等同于 `adb shell`：

- 可读写 `/data/local/tmp/`
- 可执行 `chmod`、`pm install` 等命令
- 可设置环境变量（`S25U_KSUD_PATH`、`LD_PRELOAD` 等）

### 文件推送

由于 Shizuku 不支持 `adb push`，通过 `cat` + stdin 直接传输原始字节：

- 将本地文件读取为 `byte[]`
- 分块通过 `cat > '<remote>'` 写入设备（首块 `>`，后续 `>>` 追加）
- 无 base64 编码开销，传输效率高

## 安全说明

- Root 是**内存态**：重启后失效，重新运行 App 即可再次获取（DF 链的开机自启可在每次开机自动重新提权）
- KNOX e-fuse 熔断不可逆（Secure Folder / Samsung Pay / 保修失效）
- **root 后不能刷入更改分区的模块，不能改刷面具，会砖！**
- release 包启用 R8 混淆 + 资源压缩 + 字符串加密（StringFog）
- 本应用不会收集或篡改个人隐私数据

## 致谢

本项目基于以下开源工作，特此致谢（与 APP「关于」页所列一致）：

- [KernelSU](https://kernelsu.org/) —— 内核级 ROOT 方案
- [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) —— 内核级 ROOT 方案
- [DFRoot by @diabl0w](https://github.com/diabl0w/DFRoot) —— DF 链提权实现参考
- [DFReroot by @polygraphene](https://github.com/polygraphene/DFReroot) —— 持久化（DFE）方案，本项目内嵌其 **v2.2.0**

Root 所用的 payload 文件多数收集于网络，感谢原作者作出的贡献；并感谢参与机型适配与测试的机友。

## 免责声明

仅供学习和研究使用。使用本工具可能导致：

- 设备 KNOX 可能熔断
- 保修失效
- 部分应用（银行、支付等）无法运行
- 设备不稳定或变砖

使用即代表你接受全部风险。

## 更新日志

### v1.0.0 (2026-10-14 · GitHub 首发)

**首个公开发布版本**

- **双链提权架构**：DF 链（DirtyFrag，内嵌 DFReroot **v2.2.0**）首选提权；失败自动回退 SRK 链（CVE-2026-43499 + Shizuku）；SRK 链成功后引导进入 DFE 持久化配置
- **固件级 payload 适配**：内置 21 板名 / 94 条固件配置 / 85 份 payload，覆盖 Galaxy S23–S26 全系、Z Flip5–7、Z Fold5–7、W25/W26、Tab S9 Ultra；自选固件支持 SAF 导入 payload
- **3-strike 失败保护**：DF 链连续失败 3 次自动关停并回退 SRK 链，fail-open + 成功清零 + OTA 自动解锁
- **资产完整性校验**（98 条 SHA-256 清单）、**断点续跑**（变体绑定）、**提权耗时统计**、**签名自校验 + 反调试护栏**
- KernelSU v3.3.0 / KernelSU-Next v3.4.0 双变体物料，统一多-KMI ksud（含 android16-6.12）
- 版本号体系自本版起重置，此后所有更新基于 1.0.0 递增；首发前的内部版本沿革见 [docs/CHANGELOG_HISTORY.md](docs/CHANGELOG_HISTORY.md)
