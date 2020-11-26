package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class InntektService(rapidsConnection: RapidsConnection, private val inntektClient: InntektClient) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.demandValue("@event_name", "behov")
                it.requireKey("@id")
                it.requireKey("fnr")
                it.requireKey("aktør_id")
                it.requireKey("FangstOgFiske")
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
        val fangstOgFiske = packet["FangstOgFiske"].asBoolean()
        val virkningsTidspunkt = packet["Virkningstidspunkt"].asLocalDate()

        val inntekt = runBlocking {
            inntektClient.hentKlassifisertInntekt(aktørId, virkningsTidspunkt)
        }

        packet["fakta"]
            .map { (it as ObjectNode) to it["behov"].asText() }
            .filter { (_, behov) -> behov in løserBehov }
            .forEach { (faktum, behov) ->
                when (behov) {
                    "InntektSiste3År" -> inntekt.inntektSiste3år(fangstOgFiske)
                    "InntektSiste12Mnd" -> inntekt.inntektSiste12mnd(fangstOgFiske)
                    else -> throw IllegalArgumentException("Ukjent behov $behov")
                }.also {
                    faktum.put("svar", it)
                }
            }

        packet["@event_name"] = "faktum_svar"
        context.send(packet.toJson())
    }
}
