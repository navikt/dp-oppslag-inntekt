package no.nav.dagpenger.oppslag.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

class GrunnbeløpService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", listOf("Grunnbeløp"))
                it.forbid("@løsning")
                it.requireKey("@id")
                it.requireKey("Virkningstidspunkt")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val G = getGrunnbeløpForRegel(Regel.Minsteinntekt).forDato(packet["Virkningstidspunkt"].asLocalDate()).verdi

        packet["@løsning"] = mapOf(
            "Grunnbeløp" to G,
        )

        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
