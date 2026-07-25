plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.javax.inject)   // solo la anotación @Inject, sin framework
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testFixturesImplementation(libs.coroutines.core)
}
