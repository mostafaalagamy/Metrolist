plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_core:2.37.0")
    implementation(libs.annotation)
}
