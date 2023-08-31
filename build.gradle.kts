plugins {
    kotlin("jvm")
    id("dagpenger.common")
    id("dagpenger.rapid-and-rivers")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-oppslag-inntekt"
    mainClass.set("no.nav.dagpenger.oppslag.inntekt.ApplicationKt")
}

val dpBibliotekerVersjon = Dagpenger.Biblioteker.version

dependencies {
    implementation("com.github.navikt.dp-biblioteker:ktor-client-metrics:$dpBibliotekerVersjon")
    implementation("com.github.navikt:dp-grunnbelop:2023.05.24-15.26.f42064d9fdc8")
    implementation("com.github.navikt:dagpenger-events:20230831.d11fdb")

    implementation(Bekk.nocommons)

    implementation(Konfig.konfig)
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:$dpBibliotekerVersjon")
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Ktor2.Client.library("auth-jvm"))
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("apache"))
    implementation(Ktor2.Client.library("core"))
    implementation(Ktor2.Client.library("logging-jvm"))
    implementation(Ktor2.Client.library("content-negotiation"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")
    implementation(RapidAndRiversKtor2)
    implementation(kotlin("stdlib"))

    testImplementation(Junit5.api)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor2.Client.library("mock-jvm"))
    testImplementation(kotlin("test"))
    testRuntimeOnly(Junit5.engine)
}