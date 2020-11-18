package no.nav.dagpenger.oppslag.inntekt

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val DAGPENGER_BEHOV_TOPIC_V2 = "privat-dagpenger-behov-v2"
private val LØNNSKOMPENSESJONSVEDTAK_TOPIC = "privat-permittering-lonnskomp-vedtak"

private val localProperties = ConfigurationMap(
    mapOf(
        "application.id" to "dp-oppslag-inntekt",
        "application.profile" to "LOCAL",
        "kafka.bootstrap.servers" to "localhost:9092",
        "kafka.topic" to DAGPENGER_BEHOV_TOPIC_V2,
        "kafka.extra.topic" to LØNNSKOMPENSESJONSVEDTAK_TOPIC,
        "kafka.reset.policy" to "latest",
        "application.profile" to Profile.LOCAL.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to "topic",
        "username" to "username",
        "password" to "pass",
        "kafka.reset.policy" to "earliest",
        "nav.truststore.path" to "bla/bla",
        "nav.truststore.password" to "foo",
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.id" to "dp-oppslag-inntekt-dev-v1",
        "application.profile" to "DEV",
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "application.profile" to Profile.DEV.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to DAGPENGER_BEHOV_TOPIC_V2,
        "kafka.extra.topic" to LØNNSKOMPENSESJONSVEDTAK_TOPIC,
        "kafka.reset.policy" to "earliest",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.id" to "dp-oppslag-inntekt-v1",
        "application.profile" to "PROD}",
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "application.profile" to Profile.PROD.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to DAGPENGER_BEHOV_TOPIC_V2,
        "kafka.extra.topic" to LØNNSKOMPENSESJONSVEDTAK_TOPIC,
        "kafka.reset.policy" to "earliest",
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> systemProperties() overriding EnvironmentVariables overriding localProperties
}

data class Configuration(
    val application: Application = Application(),
    val database: Database = Database(),
    val vault: Vault = Vault(),
    val kafka: Kafka = Kafka()
) {
    val testbrukere: Map<String, String> =
        if (application.profile != Profile.PROD)
            mapOf(
                "flere.arbeidsforhold.fnr" to config().getOrElse(
                    Key("flere.arbeidsforhold.fnr", stringType),
                    "testBruker1"
                ),
                "happy.path.fnr" to config().getOrElse(Key("happy.path.fnr", stringType), "testBruker2")
            ) else {
            emptyMap()
        }

    data class Application(
        val id: String = config()[Key("application.id", stringType)],
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)]
    )

    data class Kafka(
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val topic: String = config()[Key("kafka.topic", stringType)],
        val extraTopic: String = config()[Key("kafka.extra.topic", stringType)],
        val consumerGroupId: String = config().get(Key("application.id", stringType)),
        val trustStorePath: String = config()[Key("nav.truststore.path", stringType)],
        val trustStorePassword: String = config()[Key("nav.truststore.password", stringType)],
        val kafkaResetPolicy: String = config()[Key("kafka.reset.policy", stringType)],
        val rapidApplication: Map<String, String> = mapOf(
            "RAPID_APP_NAME" to "dp-oppslag-inntekt",
            "KAFKA_BOOTSTRAP_SERVERS" to brokers,
            "KAFKA_RESET_POLICY" to "earliest",
            "KAFKA_RAPID_TOPIC" to topic,
            "KAFKA_EXTRA_TOPIC" to extraTopic,
            "KAFKA_CONSUMER_GROUP_ID" to consumerGroupId,
            "NAV_TRUSTSTORE_PATH" to trustStorePath,
            "NAV_TRUSTSTORE_PASSWORD" to trustStorePassword,
            "KAFKA_RESET_POLICY" to kafkaResetPolicy
        ) + System.getenv().filter { it.key.startsWith("NAIS_") }
    )

    data class Database(
        val host: String = config()[Key("database.host", stringType)],
        val port: String = config()[Key("database.port", stringType)],
        val name: String = config()[Key("database.name", stringType)],
        val user: String? = config().getOrNull(Key("database.user", stringType)),
        val password: String? = config().getOrNull(Key("database.password", stringType)),
        val flywayLocations: List<String> = config().getOrNull(Key("flyway.locations", stringType))?.split(",")
            ?: listOf("db/migration")
    )

    data class Vault(
        val mountPath: String = config()[Key("vault.mountpath", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
