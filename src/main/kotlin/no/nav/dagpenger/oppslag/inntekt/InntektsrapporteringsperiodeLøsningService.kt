package no.nav.dagpenger.oppslag.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class InntektsrapporteringsperiodeLøsningService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.forbid("@løsning")
                it.requireKey("Virkningstidspunkt")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov = listOf(
        "InntektsrapporteringsperiodeFom",
        "InntektsrapporteringsperiodeTom",
    )

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val virkningstidspunkt = packet["Virkningstidspunkt"].asLocalDate()
        val periode = Inntektsrapporteringperiode(virkningstidspunkt)

        val løsning = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
            behov to when (behov) {
                "InntektsrapporteringsperiodeFom" -> periode.fom()
                "InntektsrapporteringsperiodeTom" -> periode.tom()
                else -> throw IllegalArgumentException("Ukjent behov $behov")
            }
        }.toMap()

        packet["@løsning"] = løsning

        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
