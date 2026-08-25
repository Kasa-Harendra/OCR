pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = java.net.URI("https://raw.githubusercontent.com/objectbox/objectbox-java/main/maven") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven { url = java.net.URI("https://raw.githubusercontent.com/objectbox/objectbox-java/main/maven") }
    }
}

rootProject.name = "OnDeviceRAG"
include(":app")
