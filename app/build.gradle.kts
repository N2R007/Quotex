import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.quantvision.trdqnt"
    minSdk = 24
    targetSdk = 36
    val configuredVersionCode = project.findProperty("APP_VERSION_CODE")?.toString()?.toIntOrNull()
        ?: System.getenv("APP_VERSION_CODE")?.toIntOrNull()
        ?: 3
    val configuredVersionName = project.findProperty("APP_VERSION_NAME")?.toString()
        ?: System.getenv("APP_VERSION_NAME")
        ?: "1.2"

    versionCode = configuredVersionCode
    versionName = configuredVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // Load from release-signing.properties if available, otherwise check environment variables
      val signingPropsFile = rootProject.file("release-signing.properties")
      val signingProps = Properties()
      if (signingPropsFile.exists()) {
        FileInputStream(signingPropsFile).use { signingProps.load(it) }
      }

      val keystorePath = signingProps.getProperty("KEYSTORE_PATH")
        ?: System.getenv("KEYSTORE_PATH")
        ?: project.findProperty("KEYSTORE_PATH")?.toString()

      val sPassword = signingProps.getProperty("STORE_PASSWORD")
        ?: System.getenv("STORE_PASSWORD")
        ?: project.findProperty("STORE_PASSWORD")?.toString()

      val kAlias = signingProps.getProperty("KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
        ?: project.findProperty("KEY_ALIAS")?.toString()
        ?: "upload"

      val kPassword = signingProps.getProperty("KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")
        ?: project.findProperty("KEY_PASSWORD")?.toString()

      if (!keystorePath.isNullOrBlank()) {
        val ksFile = file(keystorePath)
        storeFile = ksFile
        storePassword = sPassword
        keyAlias = kAlias
        keyPassword = kPassword
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.getByName("release")
      if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
        signingConfig = releaseSigning
      } else {
        signingConfig = null
      }
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.mlkit.text.recognition)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Release safety: Fail release tasks if keystore is missing; never use debug signing for release
val releaseKeystoreConfigured = android.signingConfigs.getByName("release").storeFile?.exists() == true

tasks.matching { it.name in listOf("assembleRelease", "bundleRelease") }.configureEach {
  doFirst {
    if (!releaseKeystoreConfigured) {
      throw GradleException(
        "RELEASE SIGNING ERROR: Release build cannot proceed without a valid keystore.\n" +
        "Please configure release-signing.properties in the project root with KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD, " +
        "or export KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD as environment variables.\n" +
        "Debug keystore fallback for release builds is strictly forbidden for security and Play Store compliance."
      )
    }
  }
}
