import java.util.Properties
import java.net.URI

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
val amazonConfigFile = file(providers.environmentVariable("SKILLSPROUT_AMAZON_PROPERTIES").orNull
    ?: "${System.getProperty("user.home")}/.skillsprout/amazon.properties")
val amazonConfig = Properties().apply {if(amazonConfigFile.isFile) amazonConfigFile.inputStream().use {load(it)}}
fun configString(key:String)= "\"" + amazonConfig.getProperty(key, "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "").replace("\r", "") + "\""
android {
    namespace = "com.skillsprout.arcade"
    compileSdk = 35
    val instrumentationVariant = providers.gradleProperty("skillsproutInstrumentationVariant").orElse("amazonPreview").get()
    require(instrumentationVariant in setOf("amazonPreview", "voiceQa")) { "Use a dedicated debug QA variant for instrumentation" }
    testBuildType = instrumentationVariant
    defaultConfig {
        applicationId = "com.skillsprout.arcade"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.2-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "DEVELOPMENT_CONTENT", "true")
        buildConfigField("boolean", "DEVELOPMENT_ACCESS", "true")
        buildConfigField("boolean", "PURCHASING_ALLOWED", "false")
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
        buildConfigField("boolean", "DEVELOPMENT_ACCESS", "false")
        if (releaseSigningFile.isFile) signingConfig = signingConfigs.getByName("production")
    } }
    buildTypes {
        create("voiceQa") {
            initWith(getByName("debug"));matchingFallbacks += "debug"
            applicationIdSuffix = ".voiceqa"
            versionNameSuffix = "-voice-qa"
        }
        create("amazonPreview") {initWith(getByName("debug"));matchingFallbacks += "debug"}
        create("amazonRelease") {initWith(getByName("release"));matchingFallbacks += "release"}
        create("amazonReview") {
            initWith(getByName("release"));matchingFallbacks += "release"
            versionNameSuffix = "-review"
            buildConfigField("boolean","DEVELOPMENT_CONTENT","true")
            buildConfigField("boolean","DEVELOPMENT_ACCESS","false")
            buildConfigField("boolean","PURCHASING_ALLOWED","false")
        }
        listOf("amazonPreview","amazonRelease","amazonReview").forEach {name->getByName(name).apply {
            buildConfigField("boolean","DEVELOPMENT_ACCESS","false")
            buildConfigField("boolean","PURCHASING_ALLOWED",if(name=="amazonReview") "false" else "true")
            buildConfigField("String","AMAZON_PRODUCT_MAPPINGS",configString("productMappings"))
            buildConfigField("String","VERIFIER_URL",configString("verifierUrl"))
            buildConfigField("String","RVS_ENVIRONMENT",configString("environment"))
        }}
    }
    sourceSets {
        listOf("debug","release","voiceQa").forEach {getByName(it).java.srcDir("src/offline/java")}
        listOf("amazonPreview","amazonRelease","amazonReview").forEach {getByName(it).apply {
            java.srcDir("src/amazon/java");manifest.srcFile("src/amazon/AndroidManifest.xml")
            assets.srcDir(layout.buildDirectory.dir("generated/amazonAssets"))
        }}
        getByName("amazonPreview").manifest.srcFile("src/amazonPreview/AndroidManifest.xml")
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED"); it.maxHeapSize = "3g" }
    }
}
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
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
tasks.matching { it.name == "preReleaseBuild" || it.name == "preAmazonReleaseBuild" }.configureEach { dependsOn(verifyReleaseEvidence, verifyProductionSigning) }
val prepareAmazonAssets by tasks.registering(Sync::class) {
    val publicKey=amazonConfig.getProperty("entitlementPublicKey")
    val authentication=amazonConfig.getProperty("authenticationPem")
    if(!publicKey.isNullOrBlank()) from(file(publicKey)) {rename {"entitlement-public.der"}}
    if(!authentication.isNullOrBlank()) from(file(authentication)) {rename {"AppstoreAuthenticationKey.pem"}}
    into(layout.buildDirectory.dir("generated/amazonAssets"))
}
val verifyAmazonConfiguration by tasks.registering {
    doLast {
        check(amazonConfig.getProperty("environment")=="production") {"Amazon release requires production receipt verification"}
        val url=URI(amazonConfig.getProperty("verifierUrl", ""))
        check(url.scheme=="https" && !url.host.isNullOrBlank() && url.userInfo==null && url.query==null && url.fragment==null) {"Configure the deployed HTTPS verifier"}
        val mappings=amazonConfig.getProperty("productMappings", "").split(',').map {it.split('=').map(String::trim)}
        check(mappings.isNotEmpty() && mappings.all {it.size==2 && it.all(String::isNotBlank)} && mappings.map {it.first()}.distinct().size==mappings.size) {"Configure actual Amazon purchase=receipt SKU mappings"}
        listOf("entitlementPublicKey","authenticationPem").forEach {key->check(file(amazonConfig.getProperty(key, "missing")).isFile) {"Missing $key"}}
    }
}
tasks.matching {it.name in listOf("preAmazonPreviewBuild","preAmazonReleaseBuild","preAmazonReviewBuild")}.configureEach {dependsOn(prepareAmazonAssets)}
tasks.matching {it.name=="preAmazonReleaseBuild"}.configureEach {dependsOn(verifyAmazonConfiguration)}
tasks.matching {it.name=="preAmazonReviewBuild"}.configureEach {dependsOn(verifyProductionSigning)}
dependencies {
    add("voiceQaImplementation","androidx.compose.ui:ui-test-manifest")
    add("amazonPreviewImplementation","androidx.compose.ui:ui-test-manifest")
    add("amazonReviewImplementation",project(":billing-amazon"))
    add("amazonPreviewImplementation",project(":billing-amazon"))
    add("amazonReleaseImplementation",project(":billing-amazon"))
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
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
