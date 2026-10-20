plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.skillsprout.billing"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.amazon.device:amazon-appstore-sdk:3.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
