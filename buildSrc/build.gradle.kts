plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.6.0"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.0.1")
}
