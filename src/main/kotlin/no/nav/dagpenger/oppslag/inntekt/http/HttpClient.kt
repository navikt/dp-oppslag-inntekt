package no.nav.dagpenger.oppslag.inntekt.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.http.ContentType
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetrics
import no.nav.dagpenger.oppslag.inntekt.objectMapper
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

        install(Logging) {
            level = LogLevel.INFO
        }

        install(JsonFeature) {
            accept(ContentType.Application.Json)
            serializer = JacksonSerializer(objectMapper)
        }

        install(PrometheusMetrics) {
            httpMetricsBasename?.let {
                baseName = it
            }
        }
    }
}
