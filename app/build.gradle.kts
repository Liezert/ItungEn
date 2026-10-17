import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Konfigurasi Firebase dari Firebase Console. Tanpa file ini app tetap ter-build dan berjalan sebagai tamu
// (tombol "Masuk dengan Google" menampilkan pesan belum dikonfigurasi).
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

// Kunci rilis: dari keystore.properties di akar proyek (tidak di-commit) atau variabel lingkungan ITUNGEN_*.
// Tanpa keduanya assembleRelease tetap jalan, tetapi APK-nya tidak ditandatangani.
val keystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun signingValue(key: String, env: String): String? = keystore.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.liezert.itungen"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.liezert.itungen"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            signingValue("storeFile", "ITUNGEN_KEYSTORE")?.let { storeFile = file(it) }
            storePassword = signingValue("storePassword", "ITUNGEN_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "ITUNGEN_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "ITUNGEN_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
            // R8: buang kode/resource yang tidak dipakai dan optimalkan. Room, Firebase, Credential Manager, dan
            // coroutines membawa aturan keep sendiri; kode ItungEn tidak memakai refleksi (lihat proguard-rules.pro).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Salinan rilis untuk mengukur performa di HP (docs/PERFORMANCE.md): paket terpisah (.bench) agar data
        // asli tidak tersentuh, ditandatangani kunci debug, dan berisi pengisi data uji (src/bench).
        create("bench") {
            initWith(getByName("release"))
            applicationIdSuffix = ".bench"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // UI: Indonesia (values/, bawaan) dan Inggris (values-en/); resource library lain dibuang dari APK.
        localeFilters += listOf("in", "en")
    }
    lint {
        // Setiap string harus punya terjemahan Inggris; lupa menerjemahkan = build lint gagal.
        error += "MissingTranslation"
    }
}

// Laporan stabilitas & skippability Compose: gradlew.bat :app:assembleBench -PcomposeReports
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
    if (project.hasProperty("composeReports")) {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

// Skema JSON di-commit agar setiap migrasi bisa diuji (§7.4).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":glass"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    constraints {
        // room-testing butuh 1.8+, sedangkan classpath test dipaksa sama dengan runtime app (1.7.3 transitif).
        implementation(libs.kotlinx.serialization.core) { because("MigrationTestHelper AbstractMethodError di 1.7.3") }
    }
}
