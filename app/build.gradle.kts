plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val uploadStoreFile = providers.gradleProperty("E8KB_UPLOAD_STORE_FILE")
    .orElse(providers.environmentVariable("E8KB_UPLOAD_STORE_FILE"))
val uploadStorePassword = providers.gradleProperty("E8KB_UPLOAD_STORE_PASSWORD")
    .orElse(providers.environmentVariable("E8KB_UPLOAD_STORE_PASSWORD"))
val uploadKeyAlias = providers.gradleProperty("E8KB_UPLOAD_KEY_ALIAS")
    .orElse(providers.environmentVariable("E8KB_UPLOAD_KEY_ALIAS"))
val uploadKeyPassword = providers.gradleProperty("E8KB_UPLOAD_KEY_PASSWORD")
    .orElse(providers.environmentVariable("E8KB_UPLOAD_KEY_PASSWORD"))
val uploadSigningProperties = listOf(
    uploadStoreFile,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword,
)
val uploadSigningConfigured = uploadSigningProperties.all { it.isPresent }

check(uploadSigningProperties.none { it.isPresent } || uploadSigningConfigured) {
    "Upload signing is only partially configured. Provide all E8KB_UPLOAD_* properties or none."
}

android {
    namespace = "com.maddogwarner.essential8kb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.maddogwarner.essential8kb"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (uploadSigningConfigured) {
            create("upload") {
                storeFile = file(uploadStoreFile.get())
                storePassword = uploadStorePassword.get()
                keyAlias = uploadKeyAlias.get()
                keyPassword = uploadKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
}
