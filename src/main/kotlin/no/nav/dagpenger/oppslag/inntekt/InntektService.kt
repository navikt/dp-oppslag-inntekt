package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class InntektService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
//                validate { it.demandAllOrAny("@behov", løserBehov) }
//                validate { it.requireKey("@id") }
                validate { it.requireKey("fnr") }
//                validate { it.requireKey("aktør_id") }
//                validate { it.requireKey("Virkningstidspunkt") }
//                validate { it.requireKey("fakta") }
            }
        }.register(this)
    }

    private val løserBehov = listOf(
        "InntektSiste3År",
        "InntektSiste12Mnd",
    )

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val fnr = packet["fnr"].asText()
        packet["@event_name"] = "faktum_svar"
        context.send(packet.toJson())
    }
}
