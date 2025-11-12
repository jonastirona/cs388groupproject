import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization) // Serialization plugin
    id("kotlin-kapt")

}

android {
    namespace = "com.example.modmycar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.modmycar"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Supabase credentials from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }


        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )

        buildConfigField(
            "String",
            "REDDIT_CLIENT_ID",
            "\"${localProperties.getProperty("REDDIT_CLIENT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "REDDIT_CLIENT_SECRET",
            "\"${localProperties.getProperty("REDDIT_CLIENT_SECRET", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.serialization.json) // Serialization library
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // ViewModelScope
    implementation(libs.androidx.lifecycle.runtime.ktx)   // Lifecycle support
    implementation(libs.kotlinx.coroutines.android)       // Main dispatcher for coroutines
    implementation("com.prof18.rssparser:rssparser:6.0.4") // RSS Parser
    implementation("io.coil-kt:coil:2.6.0") // Coil image loader


    // Supabase - using BOM for version management
    implementation(platform("io.github.jan-tennert.supabase:bom:${libs.versions.supabase.get()}"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")

    // Reddit API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

}