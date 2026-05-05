plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.bombavafrontmovil"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.bombavafrontmovil"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            buildConfigField("String", "API_URL", "\"https://bombava-backend-vbgv.onrender.com/\"")
            buildConfigField("String", "SOCKET_URL", "\"https://bombava-backend-vbgv.onrender.com\"")
        }
        debug {
            buildConfigField("String", "API_URL", "\"http://10.0.2.2:3000/\"")
            buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:3000\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("io.socket:socket.io-client:2.1.0") { exclude(group = "org.json", module = "json") }
}