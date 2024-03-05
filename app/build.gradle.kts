import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("com.google.dagger.hilt.android")
    id ("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
}

fun getProperties(key  : String) : String {
    return  gradleLocalProperties(rootDir).getProperty(key)
}

android {
    namespace = "kr.co.sbsolutions.newsoomirang"
    compileSdk = 34

    defaultConfig {
        applicationId = "kr.co.sbsolutions.newsoomirang"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "1.2.0"
        buildConfigField ("String", "SERVER_URL", "\"https://svc1.soomirang.kr/\"")
        buildConfigField ("String", "KAKAO",getProperties("kakao"))
        buildConfigField ("String", "ivData",getProperties("ivData"))
        buildConfigField ("String", "key",getProperties("key"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }


}




dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation (composeBom)
    androidTestImplementation (composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.compose.material3:material3")
    implementation ("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    implementation ("androidx.compose.material:material-icons-core")
    // Optional - Add full set of material icons
    implementation ("androidx.compose.material:material-icons-extended")
    // Optional - Add window size utils
    implementation ("androidx.compose.material3:material3-window-size-class")

    // Optional - Integration with activities
    implementation ("androidx.activity:activity-compose:1.8.2")
    // Optional - Integration with ViewModels
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")


    //스플레시 스크린
    implementation("androidx.core:core-splashscreen:1.1.0-alpha02")
    implementation("androidx.activity:activity:1.8.0")

    //라이프 사이클
    val lifecycle_version = "2.6.2"
    implementation ("androidx.lifecycle:lifecycle-service:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")

    implementation ("androidx.activity:activity-ktx:1.8.2")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.work:work-runtime-ktx:2.8.0")
    //리사이클러뷰
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    //프레그먼트
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation ("androidx.annotation:annotation:1.7.1")
    implementation ("androidx.fragment:fragment-ktx:1.6.2")

    //파이어베이스
    /*implementation ("com.google.firebase:firebase-crashlytics:18.6.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation ("com.google.firebase:firebase-analytics-ktx")
    implementation ("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.firebase:firebase-config-ktx")*/

    implementation("com.google.firebase:firebase-crashlytics:18.3.2")
    implementation("com.google.firebase:firebase-analytics:21.2.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1")
    implementation(platform("com.google.firebase:firebase-bom:31.1.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx:23.1.2")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    //주입
    val hiltVersion = "2.50"
    implementation("androidx.hilt:hilt-work:1.1.0")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
//    ksp ("com.google.dagger:dagger-compiler:$hiltVersion") // Dager
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    ksp("androidx.hilt:hilt-compiler:1.1.0")


    //통신
    val retrofitVersion = "2.9.0"
    implementation ("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation ("com.squareup.retrofit2:converter-gson:$retrofitVersion")

    val okhttpVersion = "4.12.0"
    implementation ("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation ("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    //차트
    val mpAndroidChartVersion = "v3.1.0"
    implementation ("com.github.PhilJay:MPAndroidChart:$mpAndroidChartVersion")

    //이미지
    val glideVersion = "4.16.0"
    implementation ("com.github.bumptech.glide:glide:$glideVersion")

    //DB
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    //CSV
    val openCsvVersion = "5.9"
    implementation ("com.opencsv:opencsv:$openCsvVersion")

    //캘린더
    implementation ("com.kizitonwose.calendar:view:2.2.0")
    implementation ("io.github.florent37:shapeofview:1.4.7")                      // Shape of view
    //텐서 플로우
    implementation ("org.tensorflow:tensorflow-lite-task-audio:0.4.0")
    //권한
    implementation ("io.github.ParkSangGwon:tedpermission-normal:3.3.0")          // TED Permission
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("com.airbnb.android:lottie:6.3.0")                            // Lottie

    implementation ("com.kakao.sdk:v2-user:2.19.0") // 카카오 로그인 API 모듈
    implementation ("com.kakao.sdk:v2-share:2.11.2") // 카카오 공유하기 API 모듈
}
