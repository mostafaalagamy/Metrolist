plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {

    // OkHttp (to JossRedClient)
    implementation(libs.okhttp) // o la versión más reciente

    // If you also need interceptors for logging (optional)
    implementation(libs.logging.interceptor)

}
