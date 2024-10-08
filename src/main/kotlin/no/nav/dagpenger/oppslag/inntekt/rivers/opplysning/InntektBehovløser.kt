package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import io.opentelemetry.instrumentation.annotations.WithSpan
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
) : River.PacketListener {
    private val behovSomLøses = listOf("InntektSiste12Mnd", "InntektSiste36Mnd", "Inntekt")

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {

        River(rapidsConnection)
            .apply {
                validate { it ->
                    it.demandAllOrAny("@behov", behovSomLøses)
                    it.forbid("@løsning")
                    it.requireKey("@id", "@behovId")
                    it.interestedIn(*behovSomLøses.toTypedArray())
                    it.requireKey("ident", "behandlingId")
                }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val behovId = packet["@behovId"].asText()
        /*Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.behovId", behovId)
            setAttribute("app.behandlingId", behandlingId.toString())
        }*/

        val behovSomSkalLøses = packet["@behov"].map { it.asText() }.filter { it in behovSomLøses }

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId,
        ) {
            val inntektId = packet[behovSomSkalLøses.first()]["InntektId"].asText()
            val inntekt =
                runBlocking {
                    inntektClient.hentInntekt(
                        inntektId = inntektId,
                    )
                }

            val løsninger =
                behovSomSkalLøses.associate { behov ->
                    when (behov) {
                        "InntektSiste12Mnd" -> {
                            val inntektSiste12Mnd = inntekt.inntektSiste12mndMed(fangstOgFisk = false)
                            behov to mapOf("verdi" to inntektSiste12Mnd)
                        }

                        "InntektSiste36Mnd" -> {
                            val inntektSiste36Mnd = inntekt.inntektSiste36Mnd(fangstOgFisk = false)

                            behov to mapOf("verdi" to inntektSiste36Mnd)
                        }
                        "Inntekt" -> {
                            behov to mapOf("verdi" to inntekt.inntekt)
                        }

                        else -> throw IllegalArgumentException("Ukjent behov $behov")
                    }
                }
            packet["@løsning"] = løsninger
            context.publish(packet.toJson())
            log.info { "Løst behov $behovSomSkalLøses" }
        }
    }
}
