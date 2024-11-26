package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient

/**
 *  TODO - Erstattes av InntektBehovløser når dp-behandling er oppdatert
 *
 */
internal class LegacyInntektBehovløser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    private val behovSomLøses = listOf("InntektSiste12Mnd", "InntektSiste36Mnd", "Inntekt")

    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.InntektBehovløser")
    }

    init {

        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", behovSomLøses)
                }
                validate { it ->
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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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
                    kotlin
                        .runCatching {
                            inntektClient.hentInntekt(
                                inntektId = inntektId,
                            )
                        }.onFailure {
                            log.error(it) { "Feil ved henting av inntekt" }
                            sikkerLogg.error(it) { "Feil ved henting av inntekt for pakke: ${packet.toJson()}" }
                        }.getOrThrow()
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
