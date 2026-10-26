import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Reads google-services.json (see README section 9). IMPORTANT: unlike
    // some Gradle plugins, this one does NOT silently no-op when that file
    // is missing -- it hard-fails the whole build with "File google-services.json
    // is missing from module root folder". You must complete the Firebase
    // Console setup in README section 9 and place that file at app/google-services.json
    // before this project will build at all, now that this plugin is applied.
    id("com.google.gms.google-services")
}

// Read local.properties for secrets that must NOT be committed to version control.
val localProps = Properties().also { props ->
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) props.load(localFile.inputStream())
}

android {
    namespace = "com.resqlink.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.resqlink.app"
        minSdk = 26 // Android 8.0 -- required for reliable Nearby Connections / BLE mesh features
        targetSdk = 35 // Google Play's 16 KB page size requirement (mandatory since the grace period ended
                        // May 31, 2026) applies to apps targeting Android 15 (API 35) and above -- see README section 6.
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Injected at build time; never commit real keys. See README "Secrets" section.
        manifestPlaceholders["MAPS_API_KEY"] = System.getenv("MAPS_API_KEY") ?: "YOUR_MAPS_API_KEY_HERE"

        // Master Dashboard URL — configured once in local.properties (see MASTER_DASHBOARD_URL).
        // Change that single line to point the app at a LAN server, cloud host, or ngrok tunnel.
        // Falls back to the Android emulator loopback if the property is missing.
        val dashboardUrl = localProps.getProperty("MASTER_DASHBOARD_URL", "http://10.0.2.2:9000")
        buildConfigField("String", "MASTER_DASHBOARD_URL", "\"$dashboardUrl\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Material 3 APIs like TopAppBar/Scaffold are still marked experimental
        // upstream; opting in at the module level avoids needing @OptIn on
        // every screen that uses them. Same story for flatMapLatest and other
        // Flow operators still behind ExperimentalCoroutinesApi.
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        // Explicit even though AGP 8.5.1+ defaults to this: uncompressed,
        // page-aligned native libraries are what actually satisfies Google
        // Play's 16 KB page size requirement -- see README section 6.
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    // Core / Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // Room (offline-first local storage)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Networking: kotlinx-serialization-json is kept for the mesh relay's
    // wire format (MeshSosPayload, see NearbyMeshManager) even though
    // Retrofit/OkHttp are no longer used -- the backend is Firestore now
    // (see data/remote/firestore and README section 9), not a REST API.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase (backend: Firestore for SOS/hazards/first-aid -- see README section 9)
    // KTX modules were merged into the main modules and discontinued in July
    // 2025 (BoM 34+); use the plain firebase-firestore artifact, not -ktx.
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-firestore")

    // Maps
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Nearby Connections (offline SOS mesh relay -- future-ready module)
    implementation("com.google.android.gms:play-services-nearby:19.3.0")

    // WorkManager (background sync / alert polling / mesh relay retry)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // DataStore (settings, theme, language prefs)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Splash screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // CameraX (AI damage-assessment photo capture, future module)
    // Pinned >= 1.4.0: versions before this shipped libimage_processing_util_jni.so
    // with 4 KB ELF alignment, which fails Google Play's 16 KB page size check
    // outright (see README section 6). Re-check for a newer stable release
    // before shipping -- this line is a compliance floor, not a ceiling.
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // QR (offline identity module)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.3")

    // Charts (analytics dashboard)
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
