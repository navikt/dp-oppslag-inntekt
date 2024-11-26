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
import no.nav.dagpenger.oppslag.inntekt.asUUID

internal class InntektIdBehovløser(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    private val behov: String = "InntektId"

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
                    it.require("$behov.Virkningsdato") {
                        it.asLocalDate()
                    }
                    it.requireKey("ident", "behandlingId")
                }
            }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.InntektIdBehovløser")
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
            // @todo: Vi må hente ut inntektId basert på opptjeningsperiode

            if (behandlingId.toString() == "019353e1-0bc7-71f5-a93c-f753e988c275") {
                log.warn { "Skipper behandling " }
                return
            }

            val virkningsdato = packet[behov]["Virkningsdato"].asLocalDate()
            val inntekt =
                runBlocking {
                    kotlin
                        .runCatching {
                            inntektClient.hentKlassifisertInntekt(
                                søknadUUID = behandlingId,
                                aktørId = null,
                                fødselsnummer = packet["ident"].asText(),
                                virkningsTidspunkt = virkningsdato,
                                callId = behovId,
                            )
                        }.onFailure {
                            log.error(it) { "Feil ved henting av inntekt" }
                            sikkerLogg.error(it) { "Feil ved henting av inntekt for pakke: ${packet.toJson()}" }
                        }.getOrThrow()
                }

            packet["@løsning"] =
                mapOf(
                    behov to
                        mapOf(
                            "verdi" to inntekt.inntektId(),
                            // "gyldigFraOgMed" to virkningsdato,
                        ),
                )

            // TODO: Birgitte fanger ikke opp pakker med bare ett behov, så vi må sette @final = true
            if (packet["@behov"].size() == 1) packet["@final"] = true

            log.info { "Løst behov $behov" }
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
