
plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {

    implementation("no.nav.dagpenger:ktor-client-metrics:2026.09.21-06.22.ddf281baf78f")
    implementation("com.github.navikt:dp-inntekt-kontrakter:2_202609181789745424.d9cfdc")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.31.1")
    implementation("io.opentelemetry:opentelemetry-api:1.66.0")

    implementation("no.bekk.bekkopen:nocommons:0.17.0")

    implementation(libs.konfig)
    implementation("no.nav.dagpenger:oauth2-klient:2026.09.17-06.22.ccf7ed62c283")
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.client)
    implementation(libs.rapids.and.rivers)

    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)

    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
    testImplementation(kotlin("test"))
}
