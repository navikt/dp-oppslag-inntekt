package no.nav.dagpenger.oppslag.inntekt

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.util.UUID

internal class FangstOgFiskeInntektLøsningService(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.requireArray("identer") {
                    requireKey("type", "historisk", "id")
                }
                it.require("identer", ::harAktørEllerFnr)
                it.requireKey("Virkningstidspunkt")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("sikkerlogg")
    }

    private val løserBehov =
        listOf(
            "FangstOgFiskeInntektSiste36mnd",
        )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val callId = "dp-oppslag-inntekt-${UUID.randomUUID()}"

        withLoggingContext(
            "behovId" to packet["@behovId"].asText(),
            "søknad_uuid" to packet["søknad_uuid"].asText(),
            "callId" to callId,
        ) {
            val søknadUUID = packet["søknad_uuid"].asUUID()

            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntekt(
                        søknadUUID = søknadUUID,
                        aktørId = packet.aktorId(),
                        fødselsnummer = packet.fodselsnummer(),
                        virkningsTidspunkt = packet["Virkningstidspunkt"].asLocalDate(),
                        callId = callId,
                    )
                }
            val løsning =
                packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
                    behov to
                        when (behov) {
                            "FangstOgFiskeInntektSiste36mnd" -> inntekt.inneholderFangstOgFiske()
                            else -> throw IllegalArgumentException("Ukjent behov $behov")
                        }
                }.toMap()

            packet["@løsning"] = løsning
            log.info { "Løst behov for $søknadUUID" }
            context.publish(packet.toJson())
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.info { problems.toString() }
        sikkerlogg.info { problems.toExtendedReport() }
    }
}
