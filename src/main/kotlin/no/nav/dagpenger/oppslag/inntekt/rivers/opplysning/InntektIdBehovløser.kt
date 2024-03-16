package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class InntektIdBehovløser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) :
    River.PacketListener {
    private val behov: String = "InntektId"

    init {
        River(rapidsConnection).apply {
            validate { it ->
                it.demandAllOrAny("@behov", listOf(behov))
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.requireKey(behov)
                it.requireKey("ident", "behandlingId")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val behovId = packet["@behovId"].asText()
        /*Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.behovId", behovId)
            setAttribute("app.behandlingId", behandlingId.toString())
        }*/

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId.toString(),
        ) {
            // @todo: Vi må hente ut inntektId basert på periode
            val virkningsTidspunkt = packet[behov]["SisteAvsluttendeKalenderMåned"].asLocalDate()
            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntekt(
                        søknadUUID = behandlingId,
                        aktørId = null,
                        fødselsnummer = packet["ident"].asText(),
                        virkningsTidspunkt = virkningsTidspunkt,
                        callId = behovId,
                    )
                }

            packet["@løsning"] = mapOf(behov to mapOf("verdi" to inntekt.inntektId()))
            packet["@final"] = true
            log.info { "Løst behov $behov" }
            context.publish(packet.toJson())
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.info { problems.toString() }
    }
}
