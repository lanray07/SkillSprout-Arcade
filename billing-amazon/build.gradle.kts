plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.skillsprout.billing"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    // These tests exercise signed-cache and HTTP configuration code without starting the store SDK.
    // The SDK receiver is validated on Fire/App Tester, not in a desktop JVM sandbox.
    testOptions {unitTests.isIncludeAndroidResources=false}
}
dependencies {
    api("com.amazon.device:amazon-appstore-sdk:3.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.6.1")
}
