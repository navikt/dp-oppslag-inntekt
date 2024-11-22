package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

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
) : River.PacketListener {
    private val behov: String = "InntektId"

    init {
        River(rapidsConnection)
            .apply {
                validate { it ->
                    it.demandAllOrAny("@behov", listOf(behov))
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
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val behovId = packet["@behovId"].asText()
        if (behandlingId.toString() == "019353e1-0bc7-71f5-a93c-f753e988c275") {
            log.warn { "skipper behandling med id $behandlingId" }
            return
        }

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId.toString(),
        ) {
            // @todo: Vi må hente ut inntektId basert på opptjeningsperiode
            val virkningsdato = packet[behov]["Virkningsdato"].asLocalDate()
            val inntekt =
                runBlocking {
                    kotlin.runCatching {
                        inntektClient.hentKlassifisertInntekt(
                            søknadUUID = behandlingId,
                            aktørId = null,
                            fødselsnummer = packet["ident"].asText(),
                            virkningsTidspunkt = virkningsdato,
                            callId = behovId,
                        )
                    }
                        .onFailure {
                            log.error(it) { "Feil ved henting av inntekt" }
                            sikkerLogg.error(it) { "Feil ved henting av inntekt for pakke: ${packet.toJson()}" }
                        }
                        .getOrThrow()
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
    ) {
        log.trace { problems.toString() }
    }
}
