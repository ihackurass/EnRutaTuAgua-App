plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "pe.enrutatuagua.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.enrutatuagua"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // ANDROID-PLAN-04 (alcance acotado, sin login real): valores de
        // config de desarrollo para el rastreo GPS -- se pisan antes de cada
        // corrida e2e con el aguateroId/rutaId reales que devuelve el
        // backend sembrado y la IP LAN de la máquina de desarrollo (NO
        // localhost: el broker corre en la máquina dev, no en el celular).
        buildConfigField("long", "AGUATERO_ID_DEV", "3L")
        buildConfigField("long", "RUTA_ID_DEV", "29L")
        buildConfigField("String", "MQTT_BROKER_URL_DEV", "\"tcp://192.168.18.6:1883\"")

        // ANDROID-PLAN-02 (T1, sin login real): mismo criterio que los
        // valores de arriba -- se pisan antes de cada corrida e2e con la IP
        // LAN de la máquina de desarrollo (NO localhost).
        buildConfigField("String", "BASE_URL_DEV", "\"http://192.168.18.6:8080/\"")
        buildConfigField("String", "AGUATERO_DNI_DEV", "\"10000003\"")
        buildConfigField("String", "AGUATERO_CLAVE_DEV", "\"aguatero1234\"")
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

    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
}

// Los tests de UI (createComposeRule) dependen de ui-test-manifest (solo debug):
// la variante release no aporta cobertura extra y se desactiva (criterio NIA).
androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":designsystem"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.maplibre)
    implementation(libs.androidx.splashscreen)
    // NetworkModule (ANDROID-PLAN-02, T7): BuildConfig vive acá, así que el
    // módulo de Hilt que arma Retrofit/OkHttp también.
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":domain")))
}
