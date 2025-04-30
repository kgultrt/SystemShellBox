import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}


android {
    namespace = "com.rk.terminal"
    android.buildFeatures.buildConfig = true
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug{
            
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }


}

dependencies {
    api(libs.appcompat)
    api(libs.material)
    api(libs.constraintlayout)
    api(libs.navigation.fragment)
    api(libs.navigation.ui)
    api(libs.asynclayoutinflater)
    api(libs.navigation.fragment.ktx)
    api(libs.navigation.ui.ktx)
    api(libs.activity)
    api(libs.lifecycle.livedata.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.runtime.ktx)
    api(libs.activity.compose)
    api(platform(libs.compose.bom))
    api(libs.ui)
    api(libs.ui.graphics)
    api(libs.material3)
    api(libs.navigation.compose)
    api(libs.terminal.view)
    api(libs.terminal.emulator)
    api(libs.utilcode)
    //api(libs.commons.net)
    api(libs.okhttp)
    api(libs.anrwatchdog)
    api(libs.androidx.palette)
    api("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    api(project(":termcore:resources"))
    api(project(":termcore:components"))

    api(project(":termcore:rish"))
}
