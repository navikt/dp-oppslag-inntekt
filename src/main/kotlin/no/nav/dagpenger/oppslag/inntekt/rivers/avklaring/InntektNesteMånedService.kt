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
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.asUUID
import java.time.LocalDate
import java.time.YearMonth

internal class InntektNesteMånedService(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
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
            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntektV2(
                        behandlingId = behandlingId,
                        fødselsnummer = packet["ident"].asText(),
                        prøvingsdato = nesteInntektsrapporteringsperiode.fom(),
                        callId = callId,
                    )
                }

            val nesteMåned = YearMonth.from(nesteInntektsrapporteringsperiode.fom())
            logger.info {
                """For prøvingsdato=$prøvingsdato så ble nesteMåned=$nesteMåned av 
                |fom=${nesteInntektsrapporteringsperiode.fom()}, 
                |tom=${nesteInntektsrapporteringsperiode.tom()}
                """.trimMargin()
            }
            val nyLøsning =
                runCatching {
                    runBlocking {
                        inntektClient.harInntekt(
                            ident = packet["ident"].asText(),
                            måned = nesteMåned,
                        )
                    }
                }

            val harInntekt = inntekt.harInntektFor(inntektsrapporteringsperiode.fom())

            if (nyLøsning.isSuccess && nyLøsning.getOrNull() == harInntekt) {
                logger.info { "Ny og gammel løsning for å sjekke inntekt i neste måned($nesteMåned) har likt svar: $harInntekt" }
            } else {
                logger.warn {
                    """
                    Ny løsning for inntekt neste måned($nesteMåned) fikk ikke samme svar. 
                    Ny=${nyLøsning.getOrNull()}, gammel=$harInntekt, exception=${nyLøsning.exceptionOrNull()}
                    """.trimIndent()
                }
            }

            val løsning =
                packet["@behov"]
                    .map { it.asText() }
                    .filter { it in løserBehov }
                    .associateWith { behov ->
                        when (behov) {
                            "HarRapportertInntektNesteMåned" -> {
                                harInntekt
                            }

                            else -> throw IllegalArgumentException("Ukjent behov $behov")
                        }
                    }

            packet["@løsning"] = løsning
            logger.info { "Løst behov $løserBehov" }
            context.publish(packet.toJson())
        }
    }

    private fun Inntekt.harInntektFor(fom: LocalDate): Boolean =
        inntektsListe.any {
            it.årMåned == YearMonth.from(fom) && it.klassifiserteInntekter.isNotEmpty()
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.trace { problems.toString() }
    }
}
