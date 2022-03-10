package no.nav.dagpenger.oppslag.inntekt

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.YearMonth

internal class InntektNesteMånedService(rapidsConnection: RapidsConnection, private val inntektClient: InntektClient) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.forbid("@løsning")
                it.requireKey("@id")
                it.requireArray("identer") {
                    requireKey("type", "historisk", "id")
                }
                it.require("identer") { identer ->
                    if(!identer.any { ident -> ident["type"].asText() == "aktørid" }) throw IllegalArgumentException("Mangler aktørid i identer")
                }
                it.requireKey("Virkningstidspunkt")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov = listOf(
        "HarRapportertInntektNesteMåned",
    )

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadUUID = packet["søknad_uuid"].asUUID()
        val aktørId =
            packet["identer"].first { it["type"].asText() == "aktørid" && !it["historisk"].asBoolean() }["id"].asText()

        val inntektsrapporteringsperiode = Inntektsrapporteringperiode(packet["Virkningstidspunkt"].asLocalDate())

        val inntekt = runBlocking {
            inntektClient.hentKlassifisertInntekt(søknadUUID = søknadUUID, aktørId = aktørId, virkningsTidspunkt = inntektsrapporteringsperiode.neste().fom())
        }

        val løsning = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
            behov to when (behov) {
                "HarRapportertInntektNesteMåned" -> inntekt.harRapportertInntektForMåned(YearMonth.from(inntektsrapporteringsperiode.fom()))
                else -> throw IllegalArgumentException("Ukjent behov $behov")
            }
        }.toMap()

        packet["@løsning"] = løsning
        log.info { "Løst behov for $søknadUUID" }
        context.publish(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.info { problems.toString() }
    }
}
