import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("common")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-oppslag-inntekt"
    mainClass.set("no.nav.dagpenger.oppslag.inntekt.ApplicationKt")
}

dependencies {
    implementation("no.nav.dagpenger:ktor-client-metrics:2024.12.10-14.29.b14a663ac6da")
    implementation("no.nav.dagpenger:dp-grunnbelop:20240812.99.7f2e69")
    implementation("com.github.navikt:dp-inntekt-kontrakter:1_20231220.55a8a9")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.10.0")
    implementation("io.opentelemetry:opentelemetry-api:1.45.0")

    implementation("no.bekk.bekkopen:nocommons:0.16.0")

    implementation(libs.konfig)
    implementation("no.nav.dagpenger:oauth2-klient:2024.10.31-15.02.1d4f08a38d24")
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-serialization-jackson:${libs.versions.ktor.get()}")
    implementation(libs.rapids.and.rivers)

    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)

    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
    testImplementation(kotlin("test"))
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
