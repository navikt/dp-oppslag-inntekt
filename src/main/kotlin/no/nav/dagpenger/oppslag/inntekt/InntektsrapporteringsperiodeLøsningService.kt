package no.nav.dagpenger.oppslag.inntekt

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.withMDC

internal class InntektsrapporteringsperiodeLøsningService(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@behov", løserBehov)
                it.forbid("@løsning")
                it.requireKey("@id", "@behovId")
                it.requireKey("Behandlingsdato")
                it.interestedIn("søknad_uuid")
            }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val løserBehov =
        listOf(
            "InntektsrapporteringsperiodeFom",
            "InntektsrapporteringsperiodeTom",
        )

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadUUID = packet["søknad_uuid"].asUUID()
        withMDC(
            mapOf(
                "behovId" to packet["@behovId"].asText(),
                "søknad_uuid" to søknadUUID.toString(),
            ),
        ) {
            val virkningstidspunkt = packet["Behandlingsdato"].asLocalDate()
            val periode = Inntektsrapporteringperiode(virkningstidspunkt)

            val løsning =
                packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
                    behov to
                        when (behov) {
                            "InntektsrapporteringsperiodeFom" -> periode.fom()
                            "InntektsrapporteringsperiodeTom" -> periode.tom()
                            else -> throw IllegalArgumentException("Ukjent behov $behov")
                        }
                }.toMap()

            packet["@løsning"] = løsning

            context.publish(packet.toJson())
            log.info { "Løst behov for $søknadUUID" }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.info { problems.toString() }
    }
}
