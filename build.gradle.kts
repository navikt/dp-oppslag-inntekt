plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-oppslag-inntekt"
    mainClass.set("no.nav.dagpenger.oppslag.inntekt.ApplicationKt")
}

val dpBibliotekerVersjon = libs.versions.dp.biblioteker.get()

dependencies {
    implementation("com.github.navikt.dp-biblioteker:ktor-client-metrics:$dpBibliotekerVersjon")
    implementation("com.github.navikt:dp-grunnbelop:2023.05.24-15.26.f42064d9fdc8")
    implementation("com.github.navikt:dp-inntekt-kontrakter:1_20231220.55a8a9")
    implementation("com.github.navikt:dagpenger-events:20240403.665b71")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.3.0")
    implementation("io.opentelemetry:opentelemetry-api:1.37.0")

    implementation("no.bekk.bekkopen:nocommons:0.16.0")

    implementation(libs.konfig)
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:$dpBibliotekerVersjon")
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-serialization-jackson:${libs.versions.ktor.get()}")
    implementation(libs.rapids.and.rivers)

    testImplementation(libs.mockk)

    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
    testImplementation(kotlin("test"))
}
