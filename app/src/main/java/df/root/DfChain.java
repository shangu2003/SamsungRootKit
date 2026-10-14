package df.root;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * 单包整合后的**进程内跑链入口**。
 *
 * <p>原 DFRoot（`df.root`）是独立 APK，SRK 只能经
 * `startActivity(TriggerActivity)` 跨 App 触发、再等广播/兜底文件收结果。
 * 本轮把漏洞引擎（`libexp.so` + 7 版 ko）与执行层整体搬进 SRK，
 * 于是触发退化为一次**进程内函数调用**，不再需要跨 App 通道、不再有安装步骤。
 *
 * <p>语义与原 {@code TriggerActivity} 的「分支 B：跑链」逐条对齐：
 * <ul>
 *   <li>{@code /dev/df} 是 tmpfs 上的 O_EXCL 互斥锁且从不删除 ⇒ 本次开机已注入则
 *       直接返回 {@link #RC_ALREADY_ARMED}，不重复跑（与原实现一致）。</li>
 *   <li>ksud 暂存发生在 {@link ExploitRunner#run} 内部（{@code stageFile}），
 *       目标 = **运行本代码的 App 自己的** DE 目录 ⇒ 单包后即
 *       {@code /data/user_de/0/com.samsung.root.kit/ksud}，与重编 ko 的 UMH 路径一致。</li>
 * </ul>
 *
 * <p>⚠️ 包名/类名保持 {@code df.root} 不可改：JNI 符号为
 * {@code Java_df_root_ExploitRunner_nativeRunAll}，且 {@code exp.c} 的
 * {@code JNI_OnLoad} 里写死 {@code FindClass("df/root/IReporter")}。
 */
public final class DfChain {

    private static final String TAG = "dfroot";

    /** 成功获取 Root（/dev/dfm0）。 */
    public static final int RC_OK = 0;
    /** ksud 执行出错退出（/dev/dfm1）。 */
    public static final int RC_KSUD_ERROR = 1;
    /** 执行超时。 */
    public static final int RC_TIMEOUT = 2;
    /** 文件补丁失败/异常。 */
    public static final int RC_PATCH_FAIL = 3;
    /** 本次开机已注入（/dev/df 已存在）。 */
    public static final int RC_ALREADY_ARMED = 9;

    private DfChain() {
    }

    /** 本次开机是否已注入过（零副作用、零权限：只 stat 一个 tmpfs 节点）。 */
    public static boolean isArmedThisBoot() {
        return new File("/dev/df").exists();
    }

    /**
     * 跑一次完整漏洞链（阻塞，调用方需在后台线程）。
     *
     * @param ctx        必须是**要使用其 DE 目录的** App 的 Context（单包后即 SRK 的 applicationContext）
     * @param reporter   进度回调（可为 null 语义安全，见 {@link IReporter}）
     * @param softReboot 是否软重启（Next 路必需）
     * @param ksuNext    true = KernelSU-Next 路
     * @return {@link #RC_OK} / {@link #RC_KSUD_ERROR} / {@link #RC_PATCH_FAIL} / {@link #RC_ALREADY_ARMED}
     */
    public static int run(Context ctx, IReporter reporter, boolean softReboot, boolean ksuNext) {
        if (isArmedThisBoot()) {
            Log.i(TAG, "DfChain: already armed this boot, skip");
            return RC_ALREADY_ARMED;
        }
        try {
            return ExploitRunner.run(ctx, reporter, softReboot, ksuNext);
        } catch (Throwable t) {
            Log.e(TAG, "DfChain.run exception", t);
            try {
                if (reporter != null) reporter.report("\n发生异常：" + t + "\n");
            } catch (Throwable ignored) {
            }
            return RC_PATCH_FAIL;
        }
    }
}
