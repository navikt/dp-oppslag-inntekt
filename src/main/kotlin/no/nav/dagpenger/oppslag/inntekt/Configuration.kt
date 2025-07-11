package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

internal object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "inntekt.api.url" to "http://dp-inntekt-api/v2/inntekt",
                "inntekt.api.url.v3" to "http://dp-inntekt-api/v3/inntekt",
                "RAPID_APP_NAME" to "dp-oppslag-inntekt",
                "KAFKA_BROKERS" to "localhost:9092",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-inntekt-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "HTTP_PORT" to "8080",
            ),
        )

    val properties = systemProperties() overriding EnvironmentVariables overriding defaultProperties
    val inntektApiUrlV2 = properties[Key("inntekt.api.url", stringType)]
    val inntektApiUrlV3 = properties[Key("inntekt.api.url.v3", stringType)]

    fun asMap(): Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val dpInntektApiScope by lazy { properties[Key("DP_INNTEKT_API_SCOPE", stringType)] }

    val dpInntektApiTokenProvider by lazy {
        val azureAd = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAd.tokenEndpointUrl,
            authType = azureAd.clientSecret(),
            httpClient =
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        jackson {
                            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        }
                    }
                },
        )
    }
}
