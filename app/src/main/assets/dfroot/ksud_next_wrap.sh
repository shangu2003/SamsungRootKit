#!/system/bin/sh
# ═══════════════════════════════════════════════════════════════════════════════
# DFRoot — KernelSU-Next 路「入口包装器」  (asset: ksud_next_wrap.sh)      v12
#
# 作用：dirtyfrag.ko 的 UMH 命令（dirtyfrag.c:60-63 写死）只有一个入口 ——
#   <DE>/ksud late-load --package-name me.weishu.kernelsu --ro-partitions[ --soft-reboot]
# 官方 KernelSU 的 ksud 接受全部这些参数；KernelSU-Next 的 late-load 没有
# --ro-partitions（clap 解析期 EXIT=2），所以 Next 路把**本脚本**放到 <DE>/ksud 占住入口，
# 由它转发给真正的 <DE>/ksud_next，并补上官方有、Next 没有的两件收尾工作。
#
# ── 本脚本只做三件事，全部与「官方 ksud 的行为」对齐 ──────────────────────────
#  1) 切进 init 的挂载命名空间（官方 late_load.rs:50 utils::switch_mnt_ns(1)）
#     不做则本链所在的 UMH 视图里 /apex 不可达，late-load 的 service 阶段拉起
#     app_process(→lspd) 会报 `CANNOT LINK EXECUTABLE ... "libnativeloader.so" not found`
#     （该库只存在于 /apex/com.android.art/lib64/）⇒ LSPosed 必然失效。
#     做法：只把**最后一跳** ksud_next 包进 `nsenter -t 1 -m --`；
#     先用只读探针确认 nsenter 可用且子进程确实落在 init 视图，探针不过则降级直跑，
#     绝不中止整条链。
#  2) 环境升级（官方 late_load.rs:53 / 91-101 / 103-109）
#     按官方顺序克隆 /proc/1/environ → 用 getprop init.svc_debug_pid.zygote 取 pid 再克隆
#     → 把 /data/adb/ksu/bin 追加进 PATH。官方注释原文：
#       "Upgrade to a full android environ so that modules can be properly loaded"
#     Next 这三段全无。补在本脚本顶层 = 整条链的根，因此 ksud_next / hook / worker /
#     模块 service.sh（→ app_process → lspd）全部继承。
#  3) 软重启通道（Next 的 late-load 不支持 --soft-reboot，必须自己补）
#     官方是「同一进程内联」：objdump 实证 cli::run@0x30237c sleep(3) → @0x302380
#     bl soft_reboot。若由 dirtyfrag UMH 的 u:r:kernel:s0 直接发起，soft_reboot() 的
#     stop/start 走 property_service 会被 SELinux 拒（Permission denied ⇒ exit status 1），
#     软重启实际不会发生。KernelSU 只放开一个域：u:r:ksu:s0（permissive + allow ALL）。
#     ⇒ 借 KernelSU 自己的模块脚本通道 /data/adb/late-load.d/，让**已被
#       escape_to_root_for_init() 改标成 ksu 域的 late-load 主进程**去执行我们的 hook；
#       hook 立刻返回（run_stage 是阻塞的，而模块加载后 late-load 只剩约 0.28 s 的活），
#       把动作交给 detach 的 worker；worker 等 late-load 主进程真正退出、再 sleep 3
#       对齐官方时序，然后**只发起一次** `ksud_next soft-reboot`。
#
# ── round35：删掉「链内自动安装管理器」（原 1b 段）────────────────────────────
#  原 1b 段（A：UMH 里直接 `pm install`；B：late-load.d hook + detach worker）
#  **实测装不上**，用户拍板改走应用侧 ③ PackageInstaller ⇒ 链内不再尝试安装。
#  本文件随之移除 MGR_* 变量与 df_mgr.log / mgr_install.log 追踪。1a 段保留。
#
# ── round34 追加：链内清理 /data/local/tmp（两路共用同一段「1a」）──────────────
#  用户需求「DF 链路提权成功也需要清理 tmp 下缓存文件」。应用侧那半
#  （DfPostRootCleaner：su → Shizuku）在本机当下走不通（无管理器时 su 结构性不可用、
#  也无 Shizuku，见 1a 段注释），故把「必然可执行」的那半放在链内：UMH 是 uid 0 +
#  permissive，零外部依赖。（round35 删掉原 1b 后，本段是链内唯一的附加动作。）
#
# ── 退出码语义（dirtyfrag.c 据此打标记）──────────────────────────────────────
#   0  → ko 的 `&& touch /dev/dfm0`；非 0 → ko 的 `|| touch /dev/dfm1`。
#   本脚本另在最后一跳成功后立刻自己 `touch /dev/dfm0`：App 侧只轮询 5000 ms，
#   而 ko 的 `&&` 要等本脚本退出才执行，提前打标才能落在窗口内。
#
# 完整根因、对照证据链与排障过程见随源码交付的
#   《DFRoot_v4_KSU-Next软重启与LSPosed修复技术文档.md》。
# ═══════════════════════════════════════════════════════════════════════════════

DE=/data/user_de/0/com.samsung.root.kit
NX="$DE/ksud_next"
B=/system/bin
SR=0
for a in "$@"; do
    [ "$a" = "--soft-reboot" ] && SR=1
done

# ── ksu 域通道（late-load.d 脚本）落点 ────────────────────────────────────────
AD=/data/adb
LLD="$AD/late-load.d"
DFD="$AD/dfroot"                 # worker 落点。**不能**放进 late-load.d ——
                                 # 该目录下任何带 exec 位的文件都会被
                                 # module.rs exec_common_scripts 无条件当 stage 脚本执行。
SR_SH="$LLD/99_dfroot_sr.sh"
SR_WK="$DFD/sr_worker.sh"
SR_CLAIM=/data/local/tmp/df_sr.claim


# 本脚本不再输出任何日志；UMH 的 stdout 一律丢弃，避免污染调用方。
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

# ══════════════════════════════════════════════════════════════════════════════
# ★ v11-① 命名空间对齐：让 ksud_next 本体跑在 init 的挂载命名空间里
#   对齐官方 diabl0w/KernelSU@783d3d87 late_load.rs:50 utils::switch_mnt_ns(1)?
#   Next 3.4.0 无此句（二进制串 "failed to switch to init mnt ns"：官方 1 / Next 0）。
#   本节只做**只读探针**并据此决定 §3 的调用形态，探针不过则降级直跑，绝不中止整条链。
# ══════════════════════════════════════════════════════════════════════════════
NSE="$B/nsenter"
[ -x "$NSE" ] || NSE="$AD/ksu/bin/busybox"       # 静态 busybox 兜底（同样有 nsenter applet）
NS_OK=0

mkdir -p "$DE/files" 2>/dev/null
_NSP="$DE/files/ns_probe.sh"
# 探针脚本里每一步都用绝对路径，因为 UMH 的 PATH 只有 /system/bin。
printf '%s\n' \
 '#!/system/bin/sh' \
 'echo "ns_self=$(readlink /proc/self/ns/mnt) ns_init=$(readlink /proc/1/ns/mnt)"' \
 'exit 0' >"$_NSP" 2>/dev/null
"$B/chmod" 0755 "$_NSP" 2>/dev/null

_probe=$("$NSE" -t 1 -m -- "$B/sh" "$_NSP" 2>&1)
_prc=$?
_pns=$(printf '%s\n' "$_probe" | "$B/grep" -m1 -o 'ns_self=[^ ]*' 2>/dev/null)
_pinit=$("$B/readlink" /proc/1/ns/mnt 2>&1)
if [ "$_prc" = "0" ] && [ -n "$_pinit" ] && [ -n "$_pns" ] && [ "$_pns" = "ns_self=$_pinit" ]; then
    NS_OK=1
fi
"$B/rm" -f "$_NSP" 2>/dev/null

# ══════════════════════════════════════════════════════════════════════════════
# ★ v10-② 环境升级（对齐官方 late_load::run 的三段：clone init / clone zygote / PATH）
#   只接受「合法标识符 + 值中不含空格/引号/反斜杠/$/反引号」的项，其余静默跳过
#   （与官方 clone_pid_environ 的 if-let-Ok 同语义）。
# ══════════════════════════════════════════════════════════════════════════════
_clone_env() {                       # $1=pid
    _cp="$1"
    [ -r "/proc/$_cp/environ" ] || return 0
    _craw=$("$B/tr" '\0' '\n' < "/proc/$_cp/environ" 2>/dev/null)
    [ -n "$_craw" ] || return 0
    while IFS= read -r _ckv; do
        [ -z "$_ckv" ] && continue
        case "$_ckv" in
            [A-Za-z_]*=*) ;;
            *) continue ;;
        esac
        _ck=${_ckv%%=*}
        case "$_ck" in
            *[!A-Za-z0-9_]*) continue ;;
        esac
        case "$_ckv" in
            *\ *|*"'"*|*'"'*|*'\'*|*'$'*|*'`'*) continue ;;
        esac
        export "$_ckv" 2>/dev/null
    done <<EOEOENV
$_craw
EOEOENV
    return 0
}

_clone_env 1
_zygpid=$("$B/getprop" init.svc_debug_pid.zygote 2>/dev/null)
case "$_zygpid" in
    ''|*[!0-9]*) ;;                        # 非纯数字 → 跳过（官方此分支亦为跳过）
    *)           _clone_env "$_zygpid" ;;
esac
case ":$PATH:" in
    *":$AD/ksu/bin:"*) ;;
    *) PATH="${PATH}:$AD/ksu/bin"; export PATH ;;
esac

# ══════════════════════════════════════════════════════════════════════════════
# 1. 把 ksu 域通道装到 /data/adb/late-load.d/（此时 SELinux 仍 Permissive）
#    SR=0 时反而要清掉，避免非软重启轮次被上一次留下的脚本误触发。
# ══════════════════════════════════════════════════════════════════════════════
if [ "$SR" = "1" ]; then
    "$B/mkdir" -p "$LLD" 2>/dev/null
    "$B/mkdir" -p "$DFD" 2>/dev/null

    # ★ 关键：先清掉该目录里**所有** DFRoot 历史通道文件。
    # 上游 module.rs exec_common_scripts 遍历 late-load.d 的每个条目，只判 exec 位、
    # 无扩展名过滤，逐个 exec_script(path, wait=true)。v4.7 曾把 worker 也放在这里，
    # v4.8 只改了自己新脚本的落点、没删掉它 ⇒ 它被当 stage 脚本又执行一遍，
    # 造成第二次 soft-reboot，且与 run_stage 互相等待、死锁 33 s 把 late-load 收尾一起拖住。
    "$B/rm" -f "$LLD/99_dfroot_sr.sh" "$LLD/99_dfroot_sr_worker.sh" 2>/dev/null

    cat > "$SR_SH" <<'DFSR_HOOK'
#!/system/bin/sh
# DFRoot — KernelSU-Next 软重启通道（hook）
# 由 ksud_next late-load 的 init_event::run_stage("late-load", true) 拉起，
# 因此运行在 u:r:ksu:s0 + uid 0。必须立刻返回：run_stage 是阻塞的，
# 而模块加载后 late-load 只剩约 0.28 s 的活要干。
BB=/data/adb/ksu/bin/busybox
[ -x "$BB" ] || BB=/system/bin/busybox
WK=/data/adb/dfroot/sr_worker.sh
# 本脚本由 KernelSU 用 `busybox sh <script>` 执行（fork+exec），
# 故 $PPID 就是 ksud_next late-load 主进程 —— 直接交给 worker，无需遍历祖先。
DFSR_LLPID=$PPID "$BB" setsid "$BB" sh "$WK" >/dev/null 2>&1 &
exit 0
DFSR_HOOK

    cat > "$SR_WK" <<'DFSR_WORKER'
#!/system/bin/sh
# DFRoot — KernelSU-Next 软重启通道（worker）
# 由 hook 以 & 派发，继承 ksu 域 + uid 0。职责：
#   ① 先搬进 init 挂载视图（busybox nsenter -t 1 -m；失败则就地直跑）
#   ② 等内核 uapi 就绪（反复 ksud_next debug info，与 ensure_uapi_version_matched 同判据）
#   ③ 等 late-load 主进程真正退出（读 /proc/<pid>，不猜延时）
#   ④ exec ksud_next soft-reboot（其 stop/start 由 ksu 域子进程执行 ⇒ 不被 SELinux 拒）
# 全部外部命令走静态 busybox + /system/bin 绝对路径（UMH 的 PATH 只有 /system/bin）。
BB=/data/adb/ksu/bin/busybox
[ -x "$BB" ] || BB=/system/bin/busybox
B=/system/bin
NX=/data/user_de/0/com.samsung.root.kit/ksud_next
SR_CLAIM=/data/local/tmp/df_sr.claim
NULL=/dev/null

# ── ① 搬进 init 挂载视图（只做一次，用一个环境变量打标）─────────────────────
if [ -z "$DFSR_NSD" ]; then
    export DFSR_NSD=1
    "$BB" nsenter -t 1 -m "$BB" sh "$0" >"$NULL" 2>&1
    _rc=$?
    if [ "$_rc" != "0" ]; then
        exec "$BB" sh "$0"
    fi
    exit 0
fi

# ── 原子 once 锁（mkdir 是原子的）：只有一个实例能拿到 ⇒ soft-reboot 只发起一次 ──
"$BB" mkdir "$SR_CLAIM" 2>/dev/null || exit 0

# ── ② 等内核 uapi 就绪（不看 /proc/modules：do_init_module 先挂链表、后跑 init，
#      而 uapi 是在 init 里注册的，故 /proc/modules 判据必然偏早约 0.26 s）──
KSUAPI=4
_v=$("$NX" --version 2>/dev/null)
case "$_v" in
    *"uapi: "*) _v=${_v##*uapi: }; _v=${_v%%[!0-9]*}; [ -n "$_v" ] && KSUAPI=$_v ;;
esac
_p=0; _ready=0
while [ "$_p" -lt 400 ]; do
    _p=$((_p+1))
    _o=$("$NX" debug info 2>/dev/null)
    case "$_o" in
        *"uapi_version: 0"*) ;;
        *"uapi_version: "*)
            _k=${_o##*uapi_version: }
            _k=${_k%%[!0-9]*}
            if [ "$_k" = "$KSUAPI" ]; then _ready=1; break; fi
            break ;;                          # 真不匹配（kernel != ksud）→ 放弃
        *) ;;
    esac
    "$BB" sleep 0.05
done
[ "$_ready" = "1" ] || exit 1

# ── ③ 取 late-load 主进程 pid：优先用 hook 传来的 PPID（精确值）───────────────
LPID="${DFSR_LLPID:-}"
if [ -n "$LPID" ]; then
    case "$(tr '\0' ' ' < "/proc/$LPID/cmdline" 2>/dev/null)" in
        *ksud*) ;;
        *) LPID="" ;;
    esac
fi
# 回退（仅当 hook 未传时）：向上遍历祖先，找 cmdline 含 ksud 的那一级。
if [ -z "$LPID" ]; then
    _cur=$$
    _i=0
    while [ "$_i" -lt 6 ]; do
        _i=$((_i+1))
        _pp=$(sed -n 's/^PPid:[[:space:]]*//p' "/proc/$_cur/status" 2>/dev/null)
        [ -z "$_pp" ] && break
        case "$(tr '\0' ' ' < "/proc/$_pp/cmdline" 2>/dev/null)" in
            *ksud*) LPID=$_pp; break ;;
        esac
        _cur=$_pp
    done
fi
# 拿不到就放弃：宁可不动，也不抢跑（抢跑正是 v4.7/v4.8 LSPosed 起不来的根因）。
[ -n "$LPID" ] || exit 2

# ── ③b 等 late-load 主进程**真正退出**（硬信号；run() 返回即全部收尾已完成）──
_n=0
while [ -d "/proc/$LPID" ] && [ "$_n" -lt 1800 ]; do      # 1800 * 0.1s = 180s 上限
    _n=$((_n+1))
    "$BB" sleep 0.1
done
[ -d "/proc/$LPID" ] && exit 3

# ── ③c 对齐官方时序：late-load 完成后再等 3 s ────────────────────────────────
#   官方 ksud 同进程内联：objdump cli::run@0x30237c `mov w0,#3; bl sleep`
#   → @0x302380 `bl soft_reboot`。
"$BB" sleep 3

# ── ④ 发起软重启（只此一次）─────────────────────────────────────────────────
"$NX" soft-reboot >"$NULL" 2>&1
exit 0
DFSR_WORKER

    "$B/chmod" 0755 "$SR_SH" "$SR_WK" 2>/dev/null

    # 清掉上一轮遗留的 once 锁（claim 是目录）与历史诊断文件
    "$B/rm" -rf "$SR_CLAIM" /data/local/tmp/df_sr.log /data/local/tmp/df_wd.log \
                  "$DE/files/df_next.log" "$DE/files/df_wd.sh" 2>/dev/null
else
    "$B/rm" -f "$SR_SH" "$LLD/99_dfroot_sr_worker.sh" 2>/dev/null
fi


# ══════════════════════════════════════════════════════════════════════════════
# 2. late-load —— 与官方路一致的调用形态 + 命名空间对齐
#    NS_OK=1 → `nsenter -t 1 -m --` 包起来（`--` 显式终止 nsenter 自己的选项解析；
#    实测不加必报 `Unknown option 'package-name'`）；
#    NS_OK=0 → 降级为原视图直跑，保证 late-load 一定会被执行。
# ══════════════════════════════════════════════════════════════════════════════
if [ "$NS_OK" = "1" ]; then
    "$NSE" -t 1 -m -- "$NX" late-load
else
    "$NX" late-load
fi
_rc=$?
[ "$_rc" = "0" ] || exit 1        # 非 0 → 交回 ko 打 dfm1

# ── 3. 立刻打成功标记（ko 的 `&& touch /dev/dfm0` 要等本脚本退出才执行，
#       而 App 侧只轮询 5000 ms，故这里必须先打）─────────────────────────────
"$B/touch" /dev/dfm0 2>/dev/null

exit 0
