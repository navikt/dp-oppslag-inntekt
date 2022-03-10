plugins {
    kotlin("jvm")
    id("dagpenger.common")
    id("dagpenger.rapid-and-rivers")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("no.nav.dagpenger.oppslag.inntekt.ApplicationKt")
}

dependencies {
    implementation(Dagpenger.Biblioteker.Ktor.Client.metrics)
    implementation(Dagpenger.Biblioteker.ktorUtils)
    implementation(Dagpenger.Grunnbeløp)
    implementation(Dagpenger.Events)

    implementation(Bekk.nocommons)

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