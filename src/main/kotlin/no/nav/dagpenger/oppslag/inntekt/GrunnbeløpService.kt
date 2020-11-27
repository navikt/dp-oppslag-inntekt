package no.nav.dagpenger.oppslag.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.math.BigDecimal

class GrunnbeløpService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
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

    private val løserBehov = listOf(
        "3G",
        "1_5G"
    )

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val G = getGrunnbeløpForRegel(Regel.Minsteinntekt).forDato(packet["Virkningstidspunkt"].asLocalDate()).verdi

        val løsning = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
            behov to when (behov) {
                "3G" -> G * BigDecimal(3)
                "1_5G" -> G * BigDecimal(1.5)
                else -> throw IllegalArgumentException("Ukjent behov $behov")
            }
        }.toMap()

        packet["@løsning"] = løsning
        log.info { "Løst behov for ${packet["søknad_uuid"]}" }
        context.send(packet.toJson())
    }
}
