plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("com.google.dagger.hilt.android")
    id ("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp")
}

android {
    namespace = "kr.co.sbsolutions.newsoomirang"
    compileSdk = 34

    defaultConfig {
        applicationId = "kr.co.sbsolutions.newsoomirang"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

}




dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //라이프 사이클
    val lifecycle_version = "2.6.2"
    implementation ("androidx.lifecycle:lifecycle-service:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")

    implementation ("androidx.activity:activity-ktx:1.8.2")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")


    //프레그먼트
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation ("androidx.annotation:annotation:1.7.1")
    implementation ("androidx.fragment:fragment-ktx:1.6.2")

    //파이어베이스
    implementation ("com.google.firebase:firebase-crashlytics:18.6.0")
    implementation ("com.google.firebase:firebase-analytics:21.5.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation ("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation ("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation ("com.google.firebase:firebase-config-ktx")

    //주입
    val hiltVersion = "2.50"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
//    ksp ("com.google.dagger:dagger-compiler:$hiltVersion") // Dager
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")

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


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
