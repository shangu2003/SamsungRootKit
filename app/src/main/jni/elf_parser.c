#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/elf.h>

/* find_hook_target —— 定位 hook 点与 payload 落点（自 v3.5 移植的两段式版本）。
 *
 * payload 落位原先只有一个选择：**第一个可执行 PT_LOAD 段尾**（p_offset + p_filesz）。
 * 写原语 patch_file_cbc() 是按文件偏移写**页缓存**，以页为粒度 ⇒ 越过本页边界即越界写，
 * 会踩坏紧随其后的段 ⇒ 灾难性且极难定位。故硬界 = 页边界 - 落点起点：
 *
 *     page_end = (start & ~(ps-1)) + ps      ⇒ slack ∈ (0, ps]，恒 > 0
 *
 * 固件一升级，执行段长度变化几字节，段尾在页内的位置就可能从 0x040 退到 0xFC0。
 * 实测 S9180ZCS8FZI1 的 libc.so：exec 段尾 0x11bfc0，**页窗塌缩到 64 字节**，
 * 而 stage2 需要 0x100(入口 delta) + 864(pad16) = 1120 字节 ⇒ 越界写 1056 字节，
 * modprobe 一 exec 就 SEGV_ACCERR（fault addr = base + 0x11c0c0，正在越界区）。
 *
 * ★ 这不是漏洞失效、不是写原语损坏、不是 hook 符号丢失 —— 只是落点选错了。
 *
 * 因此落位改为**两段式**：
 *   首选：exec 段尾（与历史版本完全一致，不动任何 program header）
 *   不足：改选**纯只读 PT_LOAD 段**的尾部页内空隙，并把该段 p_flags 补上 PF_X。
 *
 * 备用落位四条准入（缺一不可）：
 *   (a) 段 p_flags **恰为** PF_R —— 排除 RW（避免造出 RWX），也排除已带 X 的段
 *   (b) 页内空隙 >= min_payload
 *   (c) 落位 vaddr **不在 PT_GNU_RELRO 内** —— 否则 runtime 会把该页 mprotect 回
 *       R-only，补的 PF_X 形同虚设，payload 一跳即 SIGSEGV
 *   (d) 段 (p_vaddr - p_offset) 与可执行段相同 —— 只有这样才能保证
 *       「文件偏移差 == 加载后 vaddr 差」，hook 处 B 指令的相对偏移才成立
 *
 * 与上游 HEAD 单段版的差异：本版带页窗准入、只读段回退与 ±128MiB 断言，
 * 并修复了旧基线「perror 后未 close(fd) 返回 1」的 fd 泄漏。
 */
int find_hook_target(const char *libcxx, const char* symname,
                     uint64_t *hook_target, uint64_t *payload_target,
                     uint32_t* first_instruction,
                     uint64_t *payload_slack, uint64_t *next_seg_gap,
                     uint64_t min_payload,
                     uint64_t *pflags_foff, uint32_t *pflags_val) {
    Elf64_Ehdr hdr;

    *hook_target = 0;
    *payload_target = 0;
    if (payload_slack) *payload_slack = 0;
    if (next_seg_gap)  *next_seg_gap  = ~(uint64_t)0;   /* 默认：无后继段 */
    /* 默认「无需改权限」。pflags_foff 非 0 表示走了备用落位，
     * 调用侧须把该偏移处的 4 字节 p_flags 写成 pflags_val（原值 | PF_X）。 */
    if (pflags_foff)   *pflags_foff   = 0;
    if (pflags_val)    *pflags_val    = 0;

    int fd = open(libcxx, O_RDONLY);
    if(fd < 0){
                return 1;
    }
    if(read(fd, (char *)&hdr, sizeof(hdr)) < (ssize_t)sizeof(hdr)){
                close(fd);
        return 1;
    }
    if(strncmp((char *)hdr.e_ident, "\x7f""ELF", 4) != 0){
                close(fd);
        return 1;
    }
    if(lseek64(fd, hdr.e_phoff, SEEK_SET) < 0){
                close(fd);
        return 1;
    }
    if(hdr.e_phentsize != sizeof(Elf64_Phdr)) {
                close(fd);
        return 1;
    }
    uint64_t executable_off = 0;
    uint64_t executable_vaddr = 0;
    uint64_t executable_end = 0;
    /* 一次扫全 program header table，缓存全部 LOAD 段与 GNU_RELRO。
     * 单段实现找到第一个 PF_X 段就 break，只够算「exec 段尾」；
     * 备用落位要先看全部段才能挑出可用只读段，故遍历到底。 */
    struct { uint32_t idx, flags; uint64_t off, vaddr, filesz, memsz; } loads[24];
    int nload = 0;
    uint64_t relro_vaddr = 0, relro_memsz = 0;
    for(int i = 0; i < hdr.e_phnum; i++){
        Elf64_Phdr phdr;
        if(read(fd, (char *)&phdr, sizeof(phdr)) < 0){
                        close(fd);
            return 1;
        }

        if(phdr.p_type == PT_LOAD){
            if (nload < (int) (sizeof(loads) / sizeof(loads[0]))) {
                loads[nload].idx    = (uint32_t) i;
                loads[nload].flags  = phdr.p_flags;
                loads[nload].off    = phdr.p_offset;
                loads[nload].vaddr  = phdr.p_vaddr;
                loads[nload].filesz = phdr.p_filesz;
                loads[nload].memsz  = phdr.p_memsz;
                nload++;
            }
            /* 只认**第一个**带 PF_X 的段 —— 与单段实现语义一致 */
            if((phdr.p_flags & PF_X) && executable_end == 0){
                executable_end   = phdr.p_offset + phdr.p_filesz;
                executable_off   = phdr.p_offset;
                executable_vaddr = phdr.p_vaddr;
            }
        } else if (phdr.p_type == PT_GNU_RELRO) {
            relro_vaddr = phdr.p_vaddr;
            relro_memsz = phdr.p_memsz;
        }
    }
    if (executable_off == 0 && executable_vaddr == 0) {
                close(fd);
        return 1;
    }
    /* 安全上界 = 页窗（硬界）。
     * 页边界必须取**严格大于**起点的那个：(end & ~(ps-1)) + ps。
     * 若写成 (end + ps - 1) & ~(ps-1)，当 end 页对齐时会得到 end 本身 ⇒ slack=0，
     * 把一个正常的整页空隙误判成「无余量」。 */
    long ps = sysconf(_SC_PAGESIZE);
    if (ps <= 0) ps = 4096;                       /* 兜底：4 KiB */

    if (executable_end) {
        uint64_t page_end_exec = (executable_end & ~((uint64_t) ps - 1)) + (uint64_t) ps;
        uint64_t slack_exec    = page_end_exec - executable_end;   /* ∈ (0, ps] */

        if (slack_exec >= min_payload) {
            /* ── 首选：可执行段尾。与历史版本完全一致，不触碰任何 program header。 ── */
            *payload_target = executable_end;
            if (payload_slack) *payload_slack = slack_exec;
            if (pflags_foff) *pflags_foff = 0;
            if (pflags_val)  *pflags_val  = 0;
        } else {
            /* ── 备用：某**纯只读** LOAD 段尾部页内空隙，并把该段补上 PF_X ── */
            uint64_t best_off = 0, best_slack = 0, best_pfoff = 0;
            uint32_t best_pval = 0;
            uint64_t exec_delta = executable_vaddr - executable_off;

            for (int k = 0; k < nload; k++) {
                if (loads[k].flags != (uint32_t) PF_R) continue;              /* (a) */
                if ((loads[k].vaddr - loads[k].off) != exec_delta) continue;  /* (d) */

                uint64_t seg_end = loads[k].off + loads[k].filesz;
                uint64_t pe      = (seg_end & ~((uint64_t) ps - 1)) + (uint64_t) ps;
                uint64_t sl      = pe - seg_end;
                if (sl < min_payload) continue;                               /* (b) */

                uint64_t gap_va = loads[k].vaddr + (seg_end - loads[k].off);
                if (relro_memsz != 0 &&                                       /* (c) */
                    gap_va >= relro_vaddr && gap_va < relro_vaddr + relro_memsz)
                    continue;

                best_off   = seg_end;
                best_slack = sl;
                best_pfoff = hdr.e_phoff + (uint64_t) loads[k].idx * (uint64_t) hdr.e_phentsize + 4;
                best_pval  = loads[k].flags | (uint32_t) PF_X;
                break;                                                        /* 取 phdr 顺序第一个 */
            }

            if (best_off == 0) {
                                close(fd);
                return 1;
            }
            *payload_target = best_off;
            if (payload_slack) *payload_slack = best_slack;
            if (pflags_foff) *pflags_foff = best_pfoff;
            if (pflags_val)  *pflags_val  = best_pval;
        }

        /* payload 起点 → 下一 PT_LOAD 起点的距离（仅上报，不作门禁） */
        if (next_seg_gap) {
            uint64_t gap = ~(uint64_t) 0;
            for (int k = 0; k < nload; k++) {
                if (loads[k].off >= *payload_target &&
                    (loads[k].off - *payload_target) < gap)
                    gap = loads[k].off - *payload_target;
            }
            *next_seg_gap = gap;
        }
    }
    if(lseek64(fd, hdr.e_shoff + hdr.e_shstrndx * sizeof(Elf64_Shdr), SEEK_SET) < 0){
                close(fd);
        return 1;
    }
    Elf64_Shdr str_shdr;
    if(read(fd, (char *)&str_shdr, sizeof(str_shdr)) < 0){
                close(fd);
        return 1;
    }
    uint64_t dynstr = 0;
    uint64_t dynsym_offset = 0;
    uint64_t dynsym_size = 0;
    for(int i = 0; i < hdr.e_shnum; i++){
        Elf64_Shdr shdr;
        if(lseek64(fd, hdr.e_shoff + i * sizeof(Elf64_Shdr), SEEK_SET) < 0){
                        close(fd);
            return 1;
        }
        if(read(fd, (char *)&shdr, sizeof(shdr)) < 0){
                        close(fd);
            return 1;
        }
        if(lseek64(fd, shdr.sh_name + str_shdr.sh_offset, SEEK_SET) < 0){
                        close(fd);
            return 1;
        }
        char name[100];
        if(read(fd, name, sizeof(name) - 1) < 0){
                        close(fd);
            return 1;
        }
        name[sizeof(name) - 1] = 0;
        if(strcmp(name, ".dynstr") == 0){
            dynstr = shdr.sh_offset;
        }
        if(strcmp(name, ".dynsym") == 0){
            dynsym_offset = shdr.sh_offset;
            dynsym_size = shdr.sh_size;
        }
    }

    if(dynstr == 0 || dynsym_offset == 0){
                close(fd);
        return 1;
    }
    for(int i = 0; i < (int)(dynsym_size / sizeof(Elf64_Sym)); i++){
        Elf64_Sym sym;
        if(lseek64(fd, dynsym_offset + i * sizeof(Elf64_Sym), SEEK_SET) < 0){
                        close(fd);
            return 1;
        }
        if(read(fd, (char *)&sym, sizeof(sym)) < 0){
                        close(fd);
            return 1;
        }
        if(lseek64(fd, dynstr + sym.st_name, SEEK_SET) < 0){
                        close(fd);
            return 1;
        }
        char name[200];
        if(read(fd, name, sizeof(name) - 1) < 0){
                        close(fd);
            return 1;
        }
        name[sizeof(name) - 1] = 0;
        if(strcmp(name, symname) == 0){
            *hook_target = sym.st_value - executable_vaddr + executable_off;
        }
    }
    if(*hook_target == 0){
                close(fd);
        return 1;
    }
    /* Extract first instruction */
    if(lseek64(fd, *hook_target, SEEK_SET) < 0){
                close(fd);
        return 1;
    }
    if(read(fd, first_instruction, sizeof(*first_instruction)) < 0){
                close(fd);
        return 1;
    }
    if(*first_instruction == 0xd503233fU || *first_instruction == 0xd503245f){
        /* Hook next instruction if PACIASP or BTI is detected. */
                *hook_target += 4UL;

        if(read(fd, first_instruction, sizeof(*first_instruction)) < 0){
                        close(fd);
            return 1;
        }
    }

    /* hook 与 payload 的相对距离必须落在 AArch64 B/BL 的 ±128 MiB 内。
     * 同一 ELF 内必然满足，此处只作显式断言——防止未来落位算法引入跨镜像地址
     * 后仍「编译通过、真机跳飞」。 */
    if (*payload_target != 0) {
        int64_t dist = (int64_t) (*hook_target) - (int64_t) (*payload_target);
        if (dist < 0) dist = -dist;
        if (dist >= (int64_t) (128LL << 20)) {
                        close(fd);
            return 1;
        }
    }

    close(fd);
    return 0;
}
