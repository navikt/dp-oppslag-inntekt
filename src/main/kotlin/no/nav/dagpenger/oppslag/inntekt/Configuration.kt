package no.nav.dagpenger.oppslag.inntekt

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier

private val defaultProperties = ConfigurationMap(
    mapOf(
        "dp.inntekt.api.key" to "hunter2",
        "dp.inntekt.api.secret" to "hunter2",
        "inntekt.api.url" to "http://dp-inntekt-api/v1/inntekt/klassifisert",
        "RAPID_APP_NAME" to "dp-oppslag-inntekt",
        "KAFKA_BROKERS" to "localhost:9092",
        "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-inntekt-v1",
        "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
        "KAFKA_RESET_POLICY" to "latest",
        "HTTP_PORT" to "8080",
    )
)

private val config = systemProperties() overriding EnvironmentVariables overriding defaultProperties

internal object Configuration {
    val inntektApiKey = ApiKeyVerifier(config[Key("dp.inntekt.api.secret", stringType)])
        .generate(config[Key("dp.inntekt.api.key", stringType)])
    val inntektApiUrl = config[Key("inntekt.api.url", stringType)]

    fun asMap(): Map<String, String> = config.list().reversed().fold(emptyMap()) { map, pair ->
        map + pair.second
    }
}
