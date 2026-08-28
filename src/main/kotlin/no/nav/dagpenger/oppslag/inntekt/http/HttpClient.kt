package no.nav.dagpenger.oppslag.inntekt.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import no.nav.dagpenger.oppslag.inntekt.ObjectMapper
import java.time.Duration

internal fun httpClient(
    engine: HttpClientEngine =
        CIO.create {
            requestTimeout = Long.MAX_VALUE
        },
    httpMetricsBasename: String? = null,
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true

        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
        }

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(ObjectMapper.objectMapper))
        }
        httpMetricsBasename?.let { basename ->
            install(PrometheusMetricsPlugin) {
                baseName = basename
            }
        }
    }
