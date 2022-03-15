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
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import mu.KotlinLogging
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.dagpenger.oppslag.inntekt.http.httpClient

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal object Configuration {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "inntekt.api.url" to "http://dp-inntekt-api/v2/inntekt/klassifisert",
            "RAPID_APP_NAME" to "dp-oppslag-inntekt",
            "KAFKA_BROKERS" to "localhost:9092",
            "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-inntekt-v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest",
            "HTTP_PORT" to "8080",
        )
    )

    private val properties = systemProperties() overriding EnvironmentVariables overriding defaultProperties

    val inntektApiUrl = properties[Key("inntekt.api.url", stringType)]

    fun asMap(): Map<String, String> = properties.list().reversed().fold(emptyMap()) { map, pair ->
        map + pair.second
    }

    val dpInntektApiScope by lazy { properties[Key("DP_INNTEKT_API_SCOPE", stringType)] }


    fun dpInntektApiTokenProvider(): CachedOauth2Client {
        val azureAd = OAuth2Config.AzureAd(properties)

        sikkerlogg.info { "Token endpoint url: ${azureAd.tokenEndpointUrl}" }
        return CachedOauth2Client(
            tokenEndpointUrl = azureAd.tokenEndpointUrl,
            authType = azureAd.clientSecret(),
            httpClient = HttpClient() {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    }
                }
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            sikkerlogg.info { message }
                        }
                    }
                    level = LogLevel.HEADERS
                }

                engine {
                    System.getenv("HTTP_PROXY")?.let {
                        sikkerlogg.info { "MED PROXY: $it" }
                        this.proxy = ProxyBuilder.http(it)
                    }
                }
            }
        )
    }

    // val dpInntektApiTokenProvider by lazy {
    //     val azureAd = OAuth2Config.AzureAd(properties)
    //
    //     sikkerlogg.info { "Token endpoint url: ${azureAd.tokenEndpointUrl}" }
    //     CachedOauth2Client(
    //         tokenEndpointUrl = azureAd.tokenEndpointUrl,
    //         authType = azureAd.clientSecret(),
    //     )
    // }

}
