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
import no.nav.dagpenger.oppslag.inntekt.aktorId
import no.nav.dagpenger.oppslag.inntekt.asUUID
import no.nav.dagpenger.oppslag.inntekt.fodselsnummer
import no.nav.dagpenger.oppslag.inntekt.harAktørEllerFnr
import java.time.LocalDate
import java.time.YearMonth

internal class InntektNesteMånedService(
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
                    it.requireArray("identer") {
                        requireKey("type", "historisk", "id")
                    }
                    it.require("identer", ::harAktørEllerFnr)
                    it.requireKey("Virkningstidspunkt")
                    it.interestedIn("søknad_uuid")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadUUID = packet["søknad_uuid"].asUUID()
        val behovId = packet["@behovId"].asText()
        val callId = "dp-oppslag-inntekt:$behovId"

        withLoggingContext(
            "behovId" to behovId,
            "søknad_uuid" to søknadUUID.toString(),
            "callId" to callId,
        ) {
            val inntektsrapporteringsperiode = Inntektsrapporteringperiode(packet["Virkningstidspunkt"].asLocalDate())
            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntekt(
                        søknadUUID = søknadUUID,
                        aktørId = packet.aktorId(),
                        fødselsnummer = packet.fodselsnummer(),
                        virkningsTidspunkt = inntektsrapporteringsperiode.neste().fom(),
                        callId = callId,
                    )
                }
            val løsning =
                packet["@behov"]
                    .map { it.asText() }
                    .filter { it in løserBehov }
                    .map { behov ->
                        behov to
                            when (behov) {
                                "HarRapportertInntektNesteMåned" ->
                                    inntekt.harInntektFor(inntektsrapporteringsperiode.fom())

                                else -> throw IllegalArgumentException("Ukjent behov $behov")
                            }
                    }.toMap()

            packet["@løsning"] = løsning
            log.info { "Løst behov $løserBehov" }
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
        log.trace { problems.toString() }
    }
}
