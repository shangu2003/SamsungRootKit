此目录由构建流程灌入 DFReroot 集成 APK；源码树里不放二进制。

文件：df_reroot.apk
  包名     com.polygraphene.df.reroot
  版本     versionName 2.2.0 / versionCode 40
  兼容     minSdk 32 · targetSdk 36 · compileSdk 36
  大小     12,413,370 字节
  SHA-256  6fd5e4856e5cfd68c8a889308300ed54c5f569c4cce0e1c9afb70fbe30f018bb
  签名     srk-release.jks（与 SamsungRootKit 同一把密钥，硬性要求）

为什么必须同密钥：
  DFReroot 以 android:sharedUserId="android.uid.system" +
  android:process="system" 运行。注入进 /data/system/packages.xml 的
  android.uid.system 的 <pastSigs> 是 **SamsungRootKit 自己的证书**；
  若 DFReroot 用别的密钥签名，安装期 sharedUserId 校验必然失败
  （INSTALL_FAILED_SHARED_USER_INCOMPATIBLE），装不成 uid 1000，
  持久化链路整条不成立。

安装顺序（硬约束）：
  1) 用 Shizuku 通道拿到临时 root
  2) 注入证书（app_process + su，入口 com.samsung.rootkit.df.DfInject）
  3) 软重启（kill $(pidof system_server)）—— PMS 只在启动时读 packages.xml
  4) 才允许安装本 APK
  5) 判据：getApplicationInfo("com.polygraphene.df.reroot", 0).uid == 1000

灌入方式（工程根目录 build.sh 在构建前完成）：
  · 构建 DFReroot release（keystore.properties + srk-release.jks 已就位）
  · 校验其签名证书 SHA-256 与 SamsungRootKit 一致
    （09d8801cf1457a32e5ad21d49b61f3e183876ef855e73ba4559b37581674baed）
  · 复制到本目录
  · 再构建 SamsungRootKit release

上游来源：DFReroot 由 polygraphene 开源 —— https://github.com/polygraphene/DFReroot

注：本目录资产**不在** assets/SHA256SUMS.txt 的登记范围内（该清单只登记 payload/ 与
    tools/ 下的 ksud / root helper），因此替换本文件不影响运行时资产校验。
