package no.nav.dagpenger.oppslag.inntekt

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class SykepengerLøsningService(rapidsConnection: RapidsConnection, private val inntektClient: InntektClient) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.forbid("@løsning")
                it.requireKey("@id")
                it.requireKey("identer")
                it.requireKey("Virkningstidspunkt")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov = listOf(
        "SykepengerSiste36Måneder",
    )

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId =
            packet["identer"].first { it["type"].asText() == "aktørid" && !it["historisk"].asBoolean() }["id"].asText()
        val virkningstidspunkt = packet["Virkningstidspunkt"].asLocalDate()

        val inntekt = runBlocking {
            inntektClient.hentKlassifisertInntekt(aktørId, virkningstidspunkt)
        }

        val løsning = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
            behov to when (behov) {
                "SykepengerSiste36Måneder" -> inntekt.inneholderSykepenger()
                else -> throw IllegalArgumentException("Ukjent behov $behov")
            }
        }.toMap()

        packet["@løsning"] = løsning
        log.info { "Løst behov for ${packet["søknad_uuid"]}" }
        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
    }
}
