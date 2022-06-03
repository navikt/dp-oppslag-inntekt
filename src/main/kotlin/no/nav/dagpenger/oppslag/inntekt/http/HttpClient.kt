package no.nav.dagpenger.oppslag.inntekt.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import no.nav.dagpenger.oppslag.inntekt.JsonMapper

import java.time.Duration

internal fun httpClient(
    engine: HttpClientEngine = CIO.create { requestTimeout = Long.MAX_VALUE },
    httpMetricsBasename: String? = null
): HttpClient {
    return HttpClient(engine) {

        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
        }

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.objectMapper))
        }

        install(PrometheusMetricsPlugin) {
            httpMetricsBasename?.let {
                baseName = it
            }
        }
    }
}
