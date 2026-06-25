import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.dailyspark.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dailyspark.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0_test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.generateKotlin", "true")
        }

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }


    tasks.register<Copy>("renameReleaseApk") {

        val date = SimpleDateFormat(
            "yyyy-MM-dd_HHmm",
            Locale.getDefault()
        ).format(Date())

        from(layout.buildDirectory.dir("outputs/apk/release"))

        include("*.apk")

        rename {
            "DailySpark_v${android.defaultConfig.versionName}_${android.defaultConfig.versionCode}_$date.apk"
        }

        into(layout.buildDirectory.dir("renamed-apk"))
    }

}




dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Lifecycle (MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // UI & Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Room (Database)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Retrofit (Network)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Lifecycle (ViewModel & Flow)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.tbuonomo:dotsindicator:5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))

    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.android.gms:play-services-ads:25.4.0")
}