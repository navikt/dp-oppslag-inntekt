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
        "application.id" to "dp-oppslag-inntekt-v1",
        "dp.inntekt.api.key" to "hunter2",
        "inntekt.api.url" to "http://dp-inntekt-api/v1/inntekt/klassifisert",
        "dp.inntekt.api.secret" to "hunter2",
        "kafka.bootstrap.servers" to "localhost:9092",
        "kafka.reset.policy" to "latest",
        "kafka.topic" to "privat-dagpenger-behov-v2",
        "nav.truststore.password" to "foo",
        "nav.truststore.path" to "bla/bla",
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
    )
)

private val config = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties
    "prod-fss" -> systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties
    else -> systemProperties() overriding EnvironmentVariables overriding defaultProperties
}

object Configuration {

    private val apiKeyVerifier = ApiKeyVerifier(config[Key("dp.inntekt.api.secret", stringType)])
    val inntektApiKey = apiKeyVerifier.generate(config[Key("dp.inntekt.api.key", stringType)])

    val inntektApiUrl = config[Key("inntekt.api.url", stringType)]
    val rapidApplication: Map<String, String> = mapOf(
        "RAPID_APP_NAME" to "dp-oppslag-inntekt",
        "KAFKA_BOOTSTRAP_SERVERS" to config[Key("kafka.bootstrap.servers", stringType)],
        "KAFKA_RESET_POLICY" to config[Key("kafka.reset.policy", stringType)],
        "KAFKA_RAPID_TOPIC" to config[Key("kafka.topic", stringType)],
        "KAFKA_CONSUMER_GROUP_ID" to config.get(Key("application.id", stringType)),
        "NAV_TRUSTSTORE_PATH" to config[Key("nav.truststore.path", stringType)],
        "NAV_TRUSTSTORE_PASSWORD" to config[Key("nav.truststore.password", stringType)],
    ) + System.getenv().filter {
        it.key.startsWith("NAIS_")
    }
}
