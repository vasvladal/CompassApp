pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // No explicit versionCatalogs {} block needed: Gradle automatically
    // registers gradle/libs.versions.toml as the "libs" catalog because it's
    // at the default convention path. Declaring it again here would call
    // from(...) twice on the same catalog and fail with:
    //   "you can only call the 'from' method a single time"
}

rootProject.name = "CompassApp"
include(":shared")
include(":app")
