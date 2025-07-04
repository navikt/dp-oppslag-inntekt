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
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.asUUID
import java.time.YearMonth

internal class InntektNesteMånedService2(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger {}
        private val løserBehov =
            listOf(
                "HarRapportertInntektNesteMåned",
            )
    }

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

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()
        val callId = "dp-oppslag-inntekt:$behovId"
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext(
            "behovId" to behovId,
            "avklaringId" to packet["avklaringId"].asText(),
            "behandlingId" to behandlingId.toString(),
        ) {
            val prøvingsdato = packet["Virkningstidspunkt"].asLocalDate()
            val inntektsrapporteringsperiode = Inntektsrapporteringperiode(prøvingsdato)
            val nesteInntektsrapporteringsperiode = inntektsrapporteringsperiode.neste()
            val harInntekt =
                runBlocking {
                    inntektClient.harInntekt(
                        ident = packet["ident"].asText(),
                        måned = YearMonth.from(nesteInntektsrapporteringsperiode.fom()),
                    )
                }

            packet["@løsning"] = mapOf("HarRapportertInntektNesteMåned" to harInntekt)
            log.info { "Løst behov $løserBehov" }
            context.publish(packet.toJson())
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
