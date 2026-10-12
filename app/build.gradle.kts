plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.group14.expensemanager"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.group14.expensemanager"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    // LiveData support
    implementation("androidx.room:room-runtime:2.6.1")

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
