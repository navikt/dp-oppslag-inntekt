
plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {

    implementation("no.nav.dagpenger:ktor-client-metrics:2025.11.27-14.20.4aa9aa3d3d14")
    implementation("com.github.navikt:dp-inntekt-kontrakter:2_20251211.17f9d7")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.22.0")
    implementation("io.opentelemetry:opentelemetry-api:1.57.0")

    implementation("no.bekk.bekkopen:nocommons:0.16.0")

    implementation(libs.konfig)
    implementation("no.nav.dagpenger:oauth2-klient:2025.11.27-14.20.4aa9aa3d3d14")
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-serialization-jackson:${libs.versions.ktor.get()}")
    implementation(libs.rapids.and.rivers)

    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)

    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
    testImplementation(kotlin("test"))
}
