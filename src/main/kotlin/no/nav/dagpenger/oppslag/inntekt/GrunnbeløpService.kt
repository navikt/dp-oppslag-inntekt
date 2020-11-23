package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.databind.node.ObjectNode
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
                it.requireKey("@id")
                it.requireKey("fakta")
                it.requireKey("Virkningstidspunkt")
            }
        }.register(this)
    }

    private val løserBehov = listOf(
        "3G",
        "1_5G"
    )

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val G = getGrunnbeløpForRegel(Regel.Minsteinntekt).forDato(packet["Virkningstidspunkt"].asLocalDate()).verdi

        packet["fakta"]
            .map { (it as ObjectNode) to it["behov"].asText() }
            .filter { (_, behov) -> behov in løserBehov }
            .forEach { (faktum, behov) ->
                when (behov) {
                    "3G" -> G * BigDecimal(3)
                    "1_5G" -> G * BigDecimal(1.5)
                    else -> throw IllegalArgumentException("Ukjent behov $behov")
                }.also {
                    faktum.put("svar", it)
                }
            }

        packet["@event_name"] = "faktum_svar"
        context.send(packet.toJson())
    }
}
