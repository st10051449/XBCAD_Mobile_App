plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.opsc7311poe.xbcad_antoniemotors"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.opsc7311poe.xbcad_antoniemotors"
        minSdk = 27
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        abortOnError = false // Prevent build failure on lint errors
        checkReleaseBuilds = false // Skip lint checks for release builds
        warningsAsErrors = false // Optional: Don't treat warnings as errors
    }
}

dependencies {

    // Splash API
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation ("com.airbnb.android:lottie:6.0.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.biometric:biometric:1.4.0-alpha02")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.biometric.ktx)
    implementation (libs.androidx.biometric)
    implementation (libs.firebase.auth.ktx.v2101)
    implementation(libs.firebase.storage.ktx)

    //Testing
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.androidx.recyclerview)
    //implementation(libs.firebase.functions)

    implementation ("com.google.firebase:firebase-bom:32.2.0")
    implementation ("com.google.firebase:firebase-appcheck-playintegrity:16.0.0")
    implementation ("com.google.android.gms:play-services-auth:20.1.0") // or the latest version
    implementation ("com.google.android.gms:play-services-tasks:17.2.1")
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.navigation.fragment.ktx) // or the latest version
    //implementation ("com.google.firebase:firebase-functions:21.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    //annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

//testing
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.3.1")
    testImplementation("androidx.test.ext:junit:1.1.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.mockito:mockito-inline:4.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // For AndroidJUnit4
    androidTestImplementation("androidx.test:core-ktx:1.5.0") // Core KTX
    androidTestImplementation("androidx.test:rules:1.5.0") // For UI rules
    androidTestImplementation("androidx.test.ext:truth:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    //for graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
