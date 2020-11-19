package no.nav.dagpenger.oppslag.inntekt.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.util.KtorExperimentalAPI
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetrics
import java.time.Duration

@KtorExperimentalAPI
internal fun httpClient(
    credentials: Pair<String, String>?,
    engine: HttpClientEngine = CIO.create { requestTimeout = Long.MAX_VALUE },
    httpMetricsBasename: String?
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

        install(PrometheusMetrics) {
            httpMetricsBasename?.let {
                baseName = it
            }
        }

        credentials?.let {
            install(Auth) {
                basic {
                    this.sendWithoutRequest = true
                    this.username = credentials.first
                    this.password = credentials.second
                }
            }
        }
    }
}
