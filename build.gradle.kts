import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id(Shadow.shadow) version Shadow.version
}

buildscript {
    repositories {
        jcenter()
    }
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-oppslag-inntekt"
    mainClassName = "no.nav.dagpenger.oppslag.inntekt.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    implementation(Dagpenger.Biblioteker.Ktor.Client.metrics)
    implementation(Dagpenger.Biblioteker.ktorUtils)
    implementation(Dagpenger.Events)
    implementation(Konfig.konfig)
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Ktor.library("client-auth-jvm"))
    implementation(Ktor.library("client-cio"))
    implementation(Ktor.library("client-logging-jvm"))
    implementation(Ktor.library("client-jackson"))
    implementation(Ktor.serverNetty)
    implementation(RapidAndRivers)
    implementation(kotlin("stdlib"))

    testImplementation(Junit5.api)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor.library("client-mock-jvm"))
    testImplementation(kotlin("test"))
    testRuntimeOnly(Junit5.engine)
}

spotless {
    kotlin {
        ktlint(Ktlint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint(Ktlint.version)
    }
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("jar") {
    dependsOn("test")
}
tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
