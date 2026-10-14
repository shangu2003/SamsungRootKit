package com.samsung.rootkit.df

/**
 * app_process entry point. Runs as root (temp root via ghostlock etc.)
 *
 * MainActivity invokes this entry via `su`. Manual example:
 *   su -c 'CLASSPATH=/data/local/tmp/SamsungRootKit_v5.1.0.apk app_process /system/bin \
 *     --nice-name=df_inject com.samsung.rootkit.df.DfInject \
 *     --apk /data/local/tmp/df_reroot.apk [--xml /data/system/packages.xml] \
 *     [--targets android.uid.system] [--dry-run|--dump]'
 *
 * The signing key comes from --keyhex (GUI path: MainActivity reads it via
 * PackageManager, which handles v1/v2/v3 uniformly), from --apk (manual
 * path: this entry builds the System Context via ActivityThread.systemMain()
 * and reads the APK file through PackageManager — see [SysKey]), or from
 * --pkg (repair path: key of the already-installed package).
 *
 * Modes: --dump (parse + summarize only, zero writes; run FIRST),
 * --check (report per-target injected=true/false, zero writes),
 * --dry-run (parse + transform + verify, no write),
 * --uninstall (remove only our key, zero-op when absent),
 * default (full inject; FAILS when our key is already present).
 *
 * (see above for the full mode list).
 */
object DfInject {
    @JvmStatic
    fun main(args: Array<String>) {
        var apk = ""
        var keyHexArg = ""
        var xml = PackagesXml.PACKAGES_XML
        var pkg = "com.samsung.root.kit"
        var targets = listOf("android.uid.system")
        var dryRun = false
        var dump = false
        var check = false
        var uninstall = false
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--apk" -> apk = args.getOrElse(i + 1) { "" }.also { i += 2 }
                "--keyhex" -> keyHexArg = args.getOrElse(i + 1) { "" }.also { i += 2 }
                "--xml" -> xml = args.getOrElse(i + 1) { xml }.also { i += 2 }
                "--pkg" -> pkg = args.getOrElse(i + 1) { pkg }.also { i += 2 }
                "--targets" -> targets = args.getOrElse(i + 1) { "" }
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }.also { i += 2 }
                "--dry-run" -> { dryRun = true; i++ }
                "--dump" -> { dump = true; i++ }
                "--check" -> { check = true; i++ }
                "--uninstall" -> { uninstall = true; i++ }
                else -> i++
            }
        }
        if (!dump && keyHexArg.isEmpty() && apk.isEmpty() && pkg.isEmpty()) {
            System.out.println("用法：InjectMain [--keyhex <十六进制> | --apk <apk> | --pkg <已安装包>] [--xml ...] [--targets a,b] [--dry-run|--dump|--check|--uninstall]")
            kotlin.system.exitProcess(2)
        }
        val log = StringBuilder()
        try {
            log.appendLine("[*] uid=${android.os.Process.myUid()} apk=$apk")
            val raw = java.io.File(xml).readBytes()
            if (dump) {
                log.append(Abx.summarize(raw))
                System.out.println(log.toString())
                return
            }
            val keyHex = if (keyHexArg.isNotEmpty()) {
                val k = keyHexArg.trim().lowercase()
                require(Abx.isHex(k) && k.length > 100) { "--keyhex is not plausible hex" }
                log.appendLine("[+] our cert from --keyhex len=${k.length}")
                k
            } else if (apk.isNotEmpty()) {
                SysKey.keyHexFromApk(apk).also {
                    log.appendLine("[+] our cert from APK file $apk len=${it.length}")
                }
            } else {
                SysKey.keyHexInstalled(pkg).also {
                    log.appendLine("[+] our cert from installed $pkg len=${it.length}")
                }
            }
            if (check) {
                val doc = PackagesXml.parseToDom(raw)
                var all = true
                for (t in targets) {
                    val hit = PackagesXml.isInjected(doc, t, keyHex)
                    log.appendLine("[check] $t injected=$hit")
                    if (!hit) all = false
                }
                log.appendLine("[check] all_injected=$all")
                System.out.println(log.toString())
                return
            }
            if (uninstall) {
                PackagesXml.uninstallDirect(xml, keyHex, targets, log)
                val rc = Runtime.getRuntime().exec(arrayOf("/system/bin/restorecon", xml)).waitFor()
                log.appendLine("[*] restorecon rc=$rc")
                // Removal takes effect on the next framework start
                // (PMS re-reads packages.xml), like the inject path.
                log.appendLine("[+] DONE. 密钥已移除；软重启后生效")
                System.out.println(log.toString())
                return
            }
            PackagesXml.injectDirect(xml, keyHex, targets, log, dryRun)
            if (!dryRun) {
                val rc = Runtime.getRuntime().exec(arrayOf("/system/bin/restorecon", xml)).waitFor()
                log.appendLine("[*] restorecon rc=$rc")
                // PMS reads packages.xml only at boot (and on writeSettings),
                // so the DFReroot install MUST happen after a reboot, never before.
                log.appendLine("[+] DONE. 下一步：先重启，再安装 DFReroot APK")
            }
            System.out.println(log.toString())
        } catch (e: Exception) {
            System.out.println(log.toString())
            System.out.println("[x] FAILED: $e")
            e.printStackTrace(System.out)
            kotlin.system.exitProcess(1)
        }
    }
}
