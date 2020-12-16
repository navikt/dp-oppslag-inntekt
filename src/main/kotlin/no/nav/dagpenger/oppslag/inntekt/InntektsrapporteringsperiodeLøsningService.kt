package no.nav.dagpenger.oppslag.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class InntektsrapporteringsperiodeLøsningService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", løserBehov)
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

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val virkningstidspunkt = packet["Virkningstidspunkt"].asLocalDate()
        val periode = Inntektsrapporteringperiode(virkningstidspunkt)

        packet["@løsning"] = mapOf(
            "InntektsrapporteringsperiodeFom" to periode.fom(),
            "InntektsrapporteringsperiodeTom" to periode.tom(),
        )

        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
    }
}
