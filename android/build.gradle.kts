buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://raw.githubusercontent.com/objectbox/objectbox-java/main/maven") }
    }
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
