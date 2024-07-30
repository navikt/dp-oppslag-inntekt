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
import java.util.UUID

internal class SykepengerLøsningService(
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
                    it.interestedIn("søknad_uuid", "avklaringId", "behandlingId")
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
    ) {
        val søknadUUID = packet["søknad_uuid"].asUUID()
        val callId = "dp-oppslag-inntekt-${UUID.randomUUID()}"

        withLoggingContext(
            "behovId" to packet["@behovId"].asText(),
            "avklaringId" to packet["avklaringId"].asText(),
            "behandlingId" to packet["behandlingId"].asText(),
            "søknad_uuid" to søknadUUID.toString(),
            "callId" to callId,
        ) {
            val inntekt =
                runBlocking {
                    inntektClient.hentKlassifisertInntekt(
                        søknadUUID = søknadUUID,
                        aktørId = packet.aktorId(),
                        fødselsnummer = packet.fodselsnummer(),
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

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.trace { problems.toString() }
    }
}
