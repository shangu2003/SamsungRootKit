package com.samsung.rootkit.df

import android.content.pm.PackageManager

object SigKey {
    fun fromApk(pm: PackageManager, apkPath: String): String {
        // minSdk 32, so SigningInfo (API 28+) is always available.
        val pi = pm.getPackageArchiveInfo(
            apkPath, PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: throw RuntimeException("getPackageArchiveInfo returned null for $apkPath")

        val si = pi.signingInfo
            ?: throw RuntimeException("no signingInfo in $apkPath")

        // Current signer only (NOT the rotation history): this is the
        // cert PMS will see when the APK is installed.
        val signers = si.apkContentsSigners
            ?: throw RuntimeException("no apkContentsSigners in $apkPath")

        if (signers.isEmpty()) throw RuntimeException("empty signers in $apkPath")

        val der: ByteArray = signers[0].toByteArray()
        val hex = Abx.toHex(der)
        require(Abx.isHex(hex) && hex.length > 100) { "implausible cert bytes in $apkPath" }
        return hex
    }
}
