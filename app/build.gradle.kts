import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val releaseSigningFile = file(
    providers.environmentVariable("SKILLSPROUT_SIGNING_PROPERTIES").orNull
        ?: "${System.getProperty("user.home")}/.skillsprout/release-signing/signing.properties"
)
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}
android {
    namespace = "com.skillsprout.arcade"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.skillsprout.arcade"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "DEVELOPMENT_CONTENT", "true")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        if (releaseSigningFile.isFile) create("production") {
            storeFile = file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes { release {
        isDebuggable = false
        isMinifyEnabled = false
        buildConfigField("boolean", "DEVELOPMENT_CONTENT", "false")
        if (releaseSigningFile.isFile) signingConfig = signingConfigs.getByName("production")
    } }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED"); it.maxHeapSize = "3g" }
    }
}
val verifyReleaseEvidence by tasks.registering(Exec::class) {
    commandLine("python", rootProject.file("tools/verify_release.py").absolutePath)
}
val verifyProductionSigning by tasks.registering {
    doLast {
        check(releaseSigningFile.isFile) { "Production signing is missing. Run tools/setup-signing.ps1 or set SKILLSPROUT_SIGNING_PROPERTIES." }
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach {
            check(!releaseSigning.getProperty(it).isNullOrBlank()) { "Missing production signing property: $it" }
        }
        check(file(releaseSigning.getProperty("storeFile")).isFile) { "Production keystore is missing." }
        check(releaseSigning.getProperty("keyAlias") != "androiddebugkey") { "A debug key cannot sign production." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseEvidence, verifyProductionSigning) }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
