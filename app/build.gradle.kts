import org.gradle.api.tasks.testing.Test
import java.net.URI

val competitionDemoToken = providers.environmentVariable("WAREHOUSE_DEMO_ACCESS_TOKEN").orElse("").get()
require(competitionDemoToken.isEmpty() || competitionDemoToken.matches(Regex("[0-9a-f]{32}"))) {
    "WAREHOUSE_DEMO_ACCESS_TOKEN must be 32 lowercase hexadecimal characters"
}
val competitionDemoBaseUrl = providers.environmentVariable("WAREHOUSE_DEMO_BASE_URL")
    .orElse("https://warehouse-recognition.dkwzt87gbz.workers.dev").get().trim().trimEnd('/')
val competitionDemoUri = runCatching { URI(competitionDemoBaseUrl) }.getOrNull()
require(competitionDemoUri?.scheme == "https" && !competitionDemoUri?.host.isNullOrBlank() &&
    competitionDemoUri?.port == -1 && competitionDemoUri?.userInfo == null &&
    competitionDemoUri?.rawPath.isNullOrBlank() && competitionDemoUri?.rawQuery == null &&
    competitionDemoUri?.rawFragment == null) {
    "WAREHOUSE_DEMO_BASE_URL must be an HTTPS root URL without a path"
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

afterEvaluate {
    tasks.named<Test>("testDebugUnitTest") {
        testClassesDirs = files(
            layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest")
        )
        classpath = files(classpath, testClassesDirs)
    }
    tasks.register<JavaExec>("runDebugUnitTestsDirect") {
        dependsOn("transformDebugUnitTestClassesWithAsm")
        mainClass.set("org.junit.runner.JUnitCore")
        val testTask = tasks.named<Test>("testDebugUnitTest").get()
        classpath = files(
            testTask.classpath,
            layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest")
        )
        doFirst {
            val testRoot = layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest").get().asFile
            val testNames = fileTree(testRoot)
                .matching {
                    include("**/*Test.class")
                }
                .files
                .map { it.relativeTo(testRoot).invariantSeparatorsPath.removeSuffix(".class").replace('/', '.') }
                .sorted()
            args = testNames
        }
    }
}

android {
    namespace = "com.youshu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.youshu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.8.1"
        buildConfigField("String", "WAREHOUSE_DEMO_BASE_URL", "\"$competitionDemoBaseUrl\"")
        buildConfigField("String", "WAREHOUSE_DEMO_ACCESS_TOKEN", "\"$competitionDemoToken\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
}
