#!/system/bin/sh
# ═══════════════════════════════════════════════════════════════════════════════
# DFRoot — 官方 KernelSU 路「入口包装器」   (asset: dfroot/ksud_wrap.sh)    v2
#
# 位置：暂存为 <DE>/ksud —— dirtyfrag.ko 的 UMH 命令**唯一**会执行的入口
#   （dirtyfrag.c 写死）：
#     /system/bin/sh -c "<DE>/ksud late-load --package-name me.weishu.kernelsu
#                         --ro-partitions[ --soft-reboot]
#                         && touch /dev/dfm0 || touch /dev/dfm1"
#   注意 UMH 本来就是 `/system/bin/sh -c "..."` ⇒ <DE>/ksud 由 sh 执行，
#   把它从 ELF 换成脚本对内核侧零差异（Next 路早已这么做）。
#
# 本脚本做两件事：① 以 root 清掉本工具留在 /data/local/tmp 的缓存文件（1a 段，
# 吃 UMH 的 uid0 + permissive 窗口，零外部依赖）；② 原样透传全部参数给真 ksud
# （<DE>/ksud_real）。
#
# ── round35：删掉「链内自动安装管理器」（原 1b 段）─────────────────────────────
#   round33 那套双通道（A：UMH 里直接 `pm install`；B：late-load.d hook + detach
#   worker）在本机**实测装不上**（round32/33 上机取证），用户 2026-10-06 拍板：
#   管理器一律改走**应用侧 ③ PackageInstaller 通道**（无需 root，弹一次系统安装
#   确认，用户点一下即可），链内**不再做任何安装尝试**。
#   ⇒ 1a 段（tmp 清理）保留不变；1b 段连同它的 MGR_* 变量、tr_ 追踪函数、
#     df_mgr.log / mgr_install.log 一并移除。
#
# ── 与 ksud_next_wrap.sh 的分工（两者互不替代）──────────────────────────────
#   官方 ksud 接受 UMH 追加的全部参数 ⇒ 本脚本**不做任何参数翻译**；
#   Next 的 late-load 没有 --ro-partitions（clap 解析期 EXIT=2）⇒ 那份脚本才需要
#   翻译 + init 命名空间对齐 + 自补软重启。**仅下面「1a 段」两边逐字相同。**
#
# 退出码：透传真 ksud 的退出码（ko 据此 `&& touch /dev/dfm0` / `|| touch /dev/dfm1`）。
# ═══════════════════════════════════════════════════════════════════════════════

DE=/data/user_de/0/com.samsung.root.kit
B=/system/bin
AD=/data/adb
LLD="$AD/late-load.d"

# 本脚本不再输出任何日志；UMH 的 stdout 一律丢弃，避免污染调用方（与 Next 路一致）。
exec >/dev/null 2>&1

# ══════════════════════════════════════════════════════════════════════════════
# 1a. 链内清理 /data/local/tmp（root · permissive 窗口）
#   ★ 本段在 ksud_wrap.sh 与 ksud_next_wrap.sh 中**逐字相同**（校验脚本逐字节比对）。
#
#   需求（2026-10-06 用户）：DF 链路提权成功后，清掉本工具留在 /data/local/tmp 的缓存文件。
#   为什么必须落在链内：应用侧那条路（DfPostRootCleaner，su → Shizuku）在本机当下走不通 ——
#   round31 实测：无管理器时 su 结构性不可用（late-load 不注册 sucompat kprobe、本应用不在
#   allowlist、/data/adb 对普通身份 0700 不可达），且未装 Shizuku。而本段跑在 UMH
#   ⇒ **uid 0 + 整机 permissive**（dirtyfrag.c:84 置 selinux_state=false 且从不恢复，
#   直到 ksud 收尾才 setenforce(true)）⇒ 零外部依赖、必然可执行。
#   应用侧仍会在链返回后（或软重启后下次打开 SRK 时）再兜一次 —— 两处都不是可选项。
#
#   必须早于官方路的 `exec <DE>/ksud_real`（进程映像会被整块替换，之后没机会执行）。
#   （round35 删掉原「1b 链内安装管理器」后，本段是链内唯一的附加动作，位置不变。）
#
#   名单与 PayloadManager.TMP_ARTIFACTS **逐项一致**（校验脚本断言两边相等）。
#   带 * 的两项靠 shell 自行做路径展开，故 $TMPD/$_dfn **有意不加引号**；其余是字面名。
#   整段不设 set -e、每步 2>/dev/null ⇒ 清理失败绝不阻断主链。
#
#   ⚠️ **刻意不在名单里**的两项（考古查证，别顺手加回来）：
#     · srk-ksud-probe —— RootExecutor.KSUD_PROBE_PATH，注释明写「刻意不列入清理名单」：
#       它是「推一次、长期复用」的 5.7 MB 缓存，列入会让每次跑链都再推一遍；
#     · dfroot_supported —— DfRootFailureRecord.SYSTEM_ZONE_FILE，是**系统级证据记录**
#       （"df 链在这台设备上成功过"），不是缓存；清掉等于抹证据。
# ══════════════════════════════════════════════════════════════════════════════
TMPD=/data/local/tmp
for _dfn in cve-2026-43499 cve-2026-43499-root ksud-selected .ksud-stage \
            ksud-s25u-kdp temp_su.sock ksu_manager.apk 'reboot_guard*' \
            df_reroot.apk df_sr.claim df_sr.log df_wd.log \
            df_mgr.log df_mgr.lock '.dfw.*'; do
    "$B/rm" -rf $TMPD/$_dfn 2>/dev/null
done


# ── 交棒真 ksud（参数原样透传：官方 ksud 接受 UMH 追加的全部参数）──────────────
exec "$DE/ksud_real" "$@"
