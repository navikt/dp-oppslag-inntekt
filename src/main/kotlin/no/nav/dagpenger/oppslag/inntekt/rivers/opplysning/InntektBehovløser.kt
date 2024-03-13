package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class InntektBehovløser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) :
    River.PacketListener {
    private val behov = listOf("InntektSiste12Mnd", "InntektSiste36Mnd")

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {

        River(rapidsConnection).apply {
            validate { it ->
                it.demandAllOrAny("@behov", behov)
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.interestedIn(*behov.toTypedArray())
                it.requireKey("ident", "behandlingId")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val behovId = packet["@behovId"].asText()

        val behov = packet["@behov"].map { it.asText() }

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId,
        ) {
            val inntektId = packet[behov.first()]["InntektId"].asText()
            val inntekt =
                runBlocking {
                    inntektClient.hentInntekt(
                        inntektId = inntektId,
                    )
                }

            val løsninger =
                behov.associate { behov ->
                    when (behov) {
                        "InntektSiste12Mnd" -> {
                            val inntektSiste12Mnd = inntekt.inntektSiste12mndMed(fangstOgFisk = false)
                            behov to mapOf("verdi" to inntektSiste12Mnd)
                        }

                        "InntektSiste36Mnd" -> {
                            val inntektSiste36Mnd = inntekt.inntektSiste36Mnd(fangstOgFisk = false)

                            behov to mapOf("verdi" to inntektSiste36Mnd)
                        }

                        else -> throw IllegalArgumentException("Ukjent behov $behov")
                    }
                }
            packet["@løsning"] = løsninger
            context.publish(packet.toJson())
            log.info { "Løst behov $behov" }
        }
    }
}
