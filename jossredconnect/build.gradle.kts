plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {

    // OkHttp (para JossRedClient)
    implementation(libs.okhttp) // o la versión más reciente

    // Si también necesitas interceptores para logging (opcional)
    implementation(libs.logging.interceptor)

}