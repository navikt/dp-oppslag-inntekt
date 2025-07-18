package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.KlassifisertInntektRequestDto
import no.nav.dagpenger.oppslag.inntekt.RegelKontekst
import no.nav.dagpenger.oppslag.inntekt.asUUID
import java.time.LocalDate
import java.time.YearMonth

internal class InntektBehovløser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    private val behov: String = "Inntekt"

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf(behov))
                }
                validate { it ->
                    it.forbid("@løsning")
                    it.requireKey("@id", "@behovId")
                    it.requireKey(behov)
                    it.require("$behov.Prøvingsdato") {
                        it.asLocalDate()
                    }
                    it.requireKey("ident", "behandlingId")
                }
            }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.InntektBehovløser")
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val behovId = packet["@behovId"].asText()

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId.toString(),
        ) {
            // @todo: Vi må hente ut inntekt basert på opptjeningsperiode
            val prøvingsdato = packet[behov]["Prøvingsdato"].asLocalDate()
            val inntekt =
                if (System.getenv("NAIS_CLUSTER_NAME") != "prod-gcp") {
                    log.info { "henter inntekt fra v3 endepunktet" }
                    runBlocking {
                        runCatching {
                            inntektClient.hentKlassifisertInntektV3(
                                KlassifisertInntektRequestDto(
                                    personIdentifikator = packet["ident"].asText(),
                                    regelkontekst =
                                        RegelKontekst(
                                            id = behandlingId.toString(),
                                            type = "saksbehandling",
                                        ),
                                    beregningsDato = prøvingsdato,
                                    periodeFraOgMed =
                                        packet[behov]["OpptjeningsperiodeFraOgMed"]
                                            .asLocalDate()
                                            .toYearMonth(),
                                    periodeTilOgMed =
                                        packet[behov]["SisteAvsluttendeKalenderMåned"]
                                            .asLocalDate()
                                            .toYearMonth(),
                                ),
                            )
                        }.onFailure {
                            log.error(it) { "Feil ved henting av inntekt" }
                            sikkerLogg.error(it) { "Feil ved henting av inntekt for pakke: ${packet.toJson()}" }
                        }.getOrThrow()
                    }
                } else {
                    runBlocking {
                        runCatching {
                            inntektClient.hentKlassifisertInntektV2(
                                behandlingId = behandlingId,
                                fødselsnummer = packet["ident"].asText(),
                                prøvingsdato = prøvingsdato,
                                callId = behovId,
                            )
                        }.onFailure {
                            log.error(it) { "Feil ved henting av inntekt" }
                            sikkerLogg.error(it) { "Feil ved henting av inntekt for pakke: ${packet.toJson()}" }
                        }.getOrThrow()
                    }
                }

            packet["@løsning"] =
                mapOf(
                    behov to
                        mapOf(
                            "verdi" to inntekt,
                        ),
                )

            // TODO: Birgitte fanger ikke opp pakker med bare ett behov, så vi må sette @final = true
            if (packet["@behov"].size() == 1) packet["@final"] = true

            log.info { "Løst behov $behov" }
            context.publish(packet.toJson())
        }
    }

    private fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(this.year, this.monthValue)

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.trace { problems.toString() }
    }
}
