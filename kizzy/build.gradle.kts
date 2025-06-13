plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.json)
    testImplementation(libs.junit)
}
