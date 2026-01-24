@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "Metrolist"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":lastfm")
include(":betterlyrics")
include(":simpmusic")

// Use a local copy of MetroExtractor by uncommenting the lines below.
// We assume, that Metrolist and MetroExtractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in innertube/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.extractor)
// To:
//      implementation("com.github.mostafaalagamy:MetroExtractor")
//includeBuild("../MetroExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.mostafaalagamy:MetroExtractor")).using(project(":extractor"))
//    }
//}
