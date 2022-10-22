plugins {
    kotlin("jvm")
    id("dagpenger.common")
    id("dagpenger.rapid-and-rivers")
}

repositories {
    mavenCentral()
}

application {
    applicationName = "dp-oppslag-inntekt"
    mainClass.set("no.nav.dagpenger.oppslag.inntekt.ApplicationKt")
}

dependencies {
    implementation("com.github.navikt.dp-biblioteker:ktor-client-metrics:2022.06.02-09.13.7b5fc99c5517")
    implementation(Dagpenger.Grunnbeløp)
    implementation(Dagpenger.Events)

    implementation(Bekk.nocommons)

    implementation(Konfig.konfig)
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:2022.10.22-09.05.6fcf3395aa4f")
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