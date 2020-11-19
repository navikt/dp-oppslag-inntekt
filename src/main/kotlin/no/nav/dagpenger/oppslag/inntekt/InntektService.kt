package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

private val String.inntektSiste12mnd: String
    get() = ""
private val String.inntektSiste3år: String
    get() = ""

internal class InntektService(rapidsConnection: RapidsConnection, private val inntektClient: InntektClient) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.requireKey("@id")
                it.requireKey("fnr")
                it.requireKey("aktør_id")
                it.requireKey("Virkningstidspunkt")
                it.requireKey("fakta")
            }
        }.register(this)
    }

    private val løserBehov = listOf(
        "InntektSiste3År",
        "InntektSiste12Mnd",
    )

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId = packet["aktør_id"].asText()
        val virkningsTidspunkt = packet["Virkningstidspunkt"].asLocalDate()

        val inntekt = inntektClient.hentKlassifisertInntekt(aktørId, virkningsTidspunkt).let {
            val inntektSiste3år = it.inntektSiste3år
            val inntektSiste12mnd = it.inntektSiste12mnd

            packet.leggPåSvar("InntektSiste3År", inntektSiste3år)
            packet.leggPåSvar("InntektSiste12Mnd", inntektSiste12mnd)
        }

        packet["@event_name"] = "faktum_svar"
        context.send(packet.toJson())
    }
}

private fun JsonMessage.leggPåSvar(faktaNavn: String, svar: Any) {
}
