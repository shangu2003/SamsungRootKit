# BUILD_NOTES — SamsungRootKit v1.0.0（GitHub 首发版）

> 本文件为发布打包说明，不属于构建系统必需文件，可安全删除。

## 本包身份

- versionName **1.0.0** / versionCode **100**（`app/build.gradle.kts` 的 `appVersion` 为唯一版本来源）
- **GitHub 首发版（2026-10-14）**：版本号体系自此重置，此后所有更新基于 1.0.0 递增（编码规则 `major*100 + minor*10 + patch`）。
- 行为基线 = 内部 6.2.x 定版形态（无行为差异，仅版本号重置）。
- ⚠️ **降级提示**：`100 < 620`（内部 6.2.0）。从内部 620 构建的真机改装本版，需先卸载或 `adb install -r -d`。
- 签名证书 SHA-256 `09d8801cf1457a32e5ad21d49b61f3e183876ef855e73ba4559b37581674baed`
  （DN=CN=Samsung Root Kit, OU=Release, O=Independent, L=Shanghai, C=CN）。
  **keystore.properties 与 srk-release.jks 未入包**（签名机密）；模板见 `keystore.properties.example`。

## 包内非源码编译的二进制（预置产物，务必知悉）

| 文件 | 身份 | 说明 |
|---|---|---|
| `assets/dfreroot/df_reroot.apk` | 内嵌 **DFReroot v2.2.0**（versionCode 40），sha256 `6fd5e485…018bb` | SRK 内嵌的 DFReroot 集成 APK（KSU 路分派直 exec 修复 + 双路真机验证通过）。对外命名统一为 **DFReroot v2.2.0**，不再缀内部 build 号 |
| `assets/dfroot/ksud_next_wrap.sh` | DF 链 Next 路包装器 v12，sha256 `6e950345…` | 正常工作基准（与内嵌 DFReroot 的 wrapper 各自独立维护） |
| `assets/dfroot/ksud_wrap.sh` | DF 链 KSU 路包装器，sha256 `12474035…` | 预置脚本资产 |
| `assets/payload/*`（85 个） | SRK 链 payload 集（按固件适配） | 清单 `assets/payloads.list`（94 条 targets）+ `assets/SHA256SUMS.txt`（98 条） |
| `assets/tools/ksu_tools/` · `ksu_next_tools/` | KernelSU v3.3.0 / KernelSU-Next v3.4.0 双变体物料 | 统一多-KMI ksud + root helper + Manager APK |
| `jni/ko/dirtyfrag-*.ko`（7 个） | DF 链预编译内核模块（android12-5.10 ~ android17-6.18） | 闭源二进制资产，不可重编；由 `exp.c` 经 `.incbin` 内联进 `libexp.so` |
| `lib/arm64-v8a/libexp.so` | SRK/DF 共用漏洞利用引擎 | **由包内 `jni/` 源码编译**，需 NDK 27.2.12479018 |

## 构建注意

- NDK **27.2.12479018** / compileSdk 37 / minSdk 31 / targetSdk 35；StringFog release 启用（产物 dex 无明文，`com.samsung.rootkit.df.*` 桥接包已排除）。
- 输出文件名由 `appVersion` 统一生成：`SamsungRootKit_v1.0.0.apk`。
- 本机构建曾用直启 GradleMain 方式（无 cygpath，勿用 `./gradlew`）；标准环境用仓库自带 wrapper（Gradle 9.4.1）即可。
- 构建前需写 `local.properties`（`sdk.dir`；已被 `.gitignore` 排除，不入库）。

## 首发重置说明

- 历史内部版本记录（v2.x – v6.2.x）原样归档于 `docs/CHANGELOG_HISTORY.md`，仅作溯源，不属于当前版本文档。
- 打包清单与校验：根目录 `SHA256SUMS.txt` 覆盖包内全部文件（除其自身）。
