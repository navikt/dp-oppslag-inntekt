package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import no.nav.dagpenger.oppslag.inntekt.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal abstract class InntektOpplysningsbehovLøser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it ->
                it.demandAllOrAny("@behov", listOf(behov))
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.require(behov) {
                    require(it.has("Virkningsdato")) { "Mangler Virkningsdato" }
                }
                it.requireKey("ident", "behandlingId")
            }
        }.register(this)
    }

    abstract val behov: String

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val behovId = packet["@behovId"].asText()

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId.toString(),
        ) {
            val virkningsTidspunkt = packet[behov]["Virkningsdato"].asLocalDate()
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

            packet["@løsning"] = løsning(inntekt)
            log.info { "Løst behov $behov for $behandlingId" }
            context.publish(packet.toJson())
        }
    }

    abstract fun løsning(inntekt: OppslagInntekt): Map<String, Any>

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.info { problems.toString() }
    }
}
