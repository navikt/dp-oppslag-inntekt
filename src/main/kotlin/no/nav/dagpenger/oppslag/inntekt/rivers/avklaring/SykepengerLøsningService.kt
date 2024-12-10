package no.nav.dagpenger.oppslag.inntekt.rivers.avklaring

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.asUUID

internal class SykepengerLøsningService(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAllOrAny("@behov", løserBehov)
                }
                validate {

                    it.forbid("@løsning")
                    it.requireKey("@id", "@behovId")
                    it.requireKey("Virkningstidspunkt", "ident")
                    it.interestedIn("avklaringId", "behandlingId")
                }
            }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov =
        listOf(
            "SykepengerSiste36Måneder",
        )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()
        val callId = "dp-oppslag-inntekt-$behovId"
        val behandlingId = packet["behandlingId"].asUUID()
        withLoggingContext(
            "behovId" to behovId,
            "avklaringId" to packet["avklaringId"].asText(),
            "behandlingId" to behandlingId.toString(),
            "callId" to callId,
        ) {
            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntekt(
                        behandlingId = behandlingId,
                        fødselsnummer = packet["ident"].asText(),
                        virkningsTidspunkt = packet["Virkningstidspunkt"].asLocalDate(),
                        callId = callId,
                    )
                }
            val løsning =
                packet["@behov"].map { it.asText() }.filter { it in løserBehov }.associateWith { behov ->
                    when (behov) {
                        "SykepengerSiste36Måneder" ->
                            inntekt
                                .inneholderSykepenger()
                                .also { måneder ->
                                    if (måneder.isNotEmpty()) {
                                        log.info { "Måneder med sykepenger: ${måneder.joinToString { it.årMåned.toString() }}" }
                                    }
                                }.isNotEmpty()

                        else -> throw IllegalArgumentException("Ukjent behov $behov")
                    }
                }

            packet["@løsning"] = løsning
            log.info { "Løst behov $løserBehov" }
            context.publish(packet.toJson())
        }
    }

    private fun Inntekt.inneholderSykepenger() =
        inntektsListe.filter { inntektMåned ->
            inntektMåned.klassifiserteInntekter.any {
                it.inntektKlasse in listOf(InntektKlasse.SYKEPENGER, InntektKlasse.SYKEPENGER_FANGST_FISKE)
            }
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.trace { problems.toString() }
    }
}
