plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_core:2.38.0")
    implementation(libs.annotation)
}
