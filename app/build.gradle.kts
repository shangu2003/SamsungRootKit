import java.util.Properties
import java.io.InputStreamReader
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.va.stringfog")
}

// 单一版本来源：versionName 与 APK 文件名共用，避免不一致
val appVersion = "1.0.0"

android {
    namespace = "com.samsung.rootkit"
    compileSdk = 37
    // ── 单包整合（round24）：把 df 的漏洞利用引擎（libexp.so）编进本 APK ──
    // NDK 版本必须与 df 原工程一致（df/app/build.gradle.kts 亦为 27.2.12479018），
    // 否则 __ANDROID_API__ / 内联汇编 / max-page-size 行为可能漂移，破坏与重编 ko 的字节级一致性。
    ndkVersion = "27.2.12479018"

    defaultConfig {
        // 应用包名改为 com.samsung.root.kit；namespace 仍为 com.samsung.rootkit（R/BuildConfig/源码包）。
        // provider authorities 用 ${applicationId} 占位符，会自动跟随；改包名后需卸载旧包、Shizuku 重新授权。
        applicationId = "com.samsung.root.kit"
        minSdk = 31
        targetSdk = 35
        // 2026-10-14：GitHub 首发版（v1.0.0）——版本号自此重置，此后所有更新基于 1.0.0 递增。
        // 行为 = 内部 6.2.x 定版形态不变（仅版本号重置，无行为差异）。
        // 编码规则（沿用）：major*100 + minor*10 + patch ⇒ 1.0.0 = 100。
        // ⚠️ 100 < 历史值 620（内部 6.2.0）：从内部 620 构建降级安装需先卸载或 `adb install -r -d`。
        // 注意与历史值 36（6.0.3）不是同一套规则，但同套规则内单调递增，覆盖安装与升级判定正常。
        versionCode = 100
        versionName = appVersion

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            // 优先读取工程根目录 keystore.properties 指向的【专用 release 私钥】（含密码，不进 git/源码包）。
            // 若该文件缺失（如他人拿到纯源码），回退 debug 密钥以保证仍可编译；
            // 但这种包不是官方签名，提权路径的 SignatureGuard 会拒绝执行。
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply {
                    // 以 UTF-8 读取，避免中文路径被默认 ISO-8859-1 解析成乱码导致 keystore 找不到
                    propsFile.inputStream().use { load(InputStreamReader(it, Charsets.UTF_8)) }
                }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            } else {
                val debugStore = File(System.getProperty("user.home"), ".android/debug.keystore")
                storeFile = debugStore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ── 原生层（df 漏洞利用引擎）：src/main/jni 原样来自 df 4.20 ──
    // CMakeLists 与 df 逐字节相同（project(exp) / add_library(exp SHARED exp.c libcxx.S elf_parser.c)），
    // 产物即 libexp.so，内含 .incbin 内联的 7 个 dirtyfrag-*.ko 与 splicehelper。
    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 与 df 一致：legacy packaging ⇒ manifest 自动 extractNativeLibs=true，
        // libexp.so 被解压到 /data/app/…/lib/arm64/，加载路径与 df 原行为完全相同。
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// 统一 APK 文件名：APP名 + 版本号 -> SamsungRootKit_v<appVersion>.apk（版本号只在上面的 appVersion 里改）
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("SamsungRootKit_v${appVersion}.apk")
        }
    }
}

dependencies {
    // Compose BOM (aligned with BuSung-dev/Root-My-Galaxy)
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Dynamic color (Material You) — same lib as Root-My-Galaxy
    implementation("com.materialkolor:material-kolor:4.1.1")

    // Shizuku API (hidden API stub)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // JSON parsing
    implementation("org.json:json:20240303")

    // AppCompat (per-app locale via AppCompatDelegate)
    implementation("androidx.appcompat:appcompat:1.7.1")

    // 背景模糊：自包含实现（ui/glass/GlassBlur.kt），替换原 `com.github.skydoves:cloudy`。
    // 原因：JitPack 对 skydoves/cloudy 所有版本构建状态均为 Error，Maven Central 亦无该 artifact，
    // 无法解析。GlassBlur 用 GraphicsLayer + RenderEffect(API31+) 复刻同一公开 API，
    // 行为与 OdinBox 的 BlurBackgroundView / RealtimeBackdropDrawable 对齐。

    // 真·实时玻璃模糊（Phase 3 弹窗接入）：Kyant0/AndroidLiquidGlass（Backdrop，Apache-2.0）。
    // blur 依赖 RenderEffect(API31+，本工程 minSdk=31 满足)；lens 折射依赖 RuntimeShader(API33+)。
    // 库内部已做能力探测：API 不足时对应 effect 自动 no-op，不崩溃（源码验证：Blur.kt/Lens.kt）。
    implementation("io.github.kyant0:backdrop:2.0.0")
    // StringFogPro 运行时解密器（AGP 9.2 适配 fork）：与 R8 混淆不冲突，仅加密 com.samsung.rootkit 下敏感字符串
    implementation("com.github.Xiaomao136.StringFogPro:stringfog:2.3.1")
}

// StringFogPro：编译期加密 dex 中字符串字面量（am start 命令、/data/local/tmp 路径、ksu 包名等），运行时解密。
// fogPackages 仅覆盖自有代码包，第三方库与资源字符串不参与。
stringfog {
    enabled.set(true)
    algorithm.set("xor")
    fogPackages.add("com.samsung.rootkit")
    // ── 集成版新增（v5.1.0）：把 DFReroot 桥接包排除在加密之外 ──
    // 理由：com.samsung.rootkit.df.* 的代码要跑在 **app_process** 进程里（不是 App 进程），
    //       属于另一个运行环境；这些字符串（packages.xml 路径、shared-user 名、
    //       app_process 入口类名）在那种环境下能否解密属于"未验证面"，没必要引入。
    //
    // 已做的独立核验（javap 字节码级，2026-09-25）表明**不排除也不会崩**：
    //   · stringfog-2.3.1.jar 全部 6 个 class 对 "android.*" 零引用
    //     （AesFog/IStringFog/RandomKeyGenerator/StringFogRuntime/XorFog）；
    //   · StringFogRuntime.decrypt(String) 只做 java.util.Base64 解码 + XorFog 字节 XOR + UTF-8 解码，
    //     静态字段仅 DEFAULT_KEY/REGISTRY/DEFAULT_FOG，不碰 Context/Log/ActivityThread；
    //   · 插件侧 PackageFilter.shouldInstrument(className, fogPackages, excludePackages)
    //     先查 excludePackages（StringsKt.startsWith 前缀匹配，命中即 return false），再查 fogPackages
    //     ⇒ exclude 优先于 fog。
    // 所以这行是"减少不确定面"的加固，不是修 bug。
    // 验证手段：构建后反查 dex 里 "com.samsung.rootkit.df.DfInject" 是否为明文（见工程根 build.sh）。
    excludePackages.add("com.samsung.rootkit.df")
}
