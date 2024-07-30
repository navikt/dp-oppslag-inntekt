package no.nav.dagpenger.oppslag.inntekt.rivers.avklaring

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.aktorId
import no.nav.dagpenger.oppslag.inntekt.asUUID
import no.nav.dagpenger.oppslag.inntekt.fodselsnummer
import no.nav.dagpenger.oppslag.inntekt.harAktørEllerFnr
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.YearMonth

internal class InntektNesteMånedService(
    rapidsConnection: RapidsConnection,
    private val inntektClient: InntektClient,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandAllOrAny("@behov", løserBehov)
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

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov =
        listOf(
            "HarRapportertInntektNesteMåned",
        )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
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
                                    inntekt.harRapportertInntektForMåned(
                                        YearMonth.from(
                                            inntektsrapporteringsperiode.fom(),
                                        ),
                                    )

                                else -> throw IllegalArgumentException("Ukjent behov $behov")
                            }
                    }.toMap()

            packet["@løsning"] = løsning
            log.info { "Løst behov $løserBehov" }
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
