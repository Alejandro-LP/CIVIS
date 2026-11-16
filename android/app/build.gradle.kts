plugins {
    id("com.android.application")
    // Android Gradle Plugin 9.2 incluye soporte Kotlin, sin kotlin-android adicional.
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.civis.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.civis.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"
    }

    // Android/D8 no ejecuta bytecode Java 26. La compilación se inicia con JDK 26,
    // pero el bytecode de la app se genera para Java 17 (compatible con Android).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
