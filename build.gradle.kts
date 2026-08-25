// Top-level build file
plugins {
    // 8.6.1 (not 8.5.x) is the minimum AGP version tested/supported for compileSdk = 35 --
    // see https://d.android.com/r/tools/api-level-support. Gradle wrapper (8.7, below)
    // already satisfies AGP 8.6's minimum Gradle requirement.
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
