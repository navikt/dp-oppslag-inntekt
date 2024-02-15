package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import no.nav.helse.rapids_rivers.RapidsConnection

internal class InntektSiste36MndBehovløser(
    rapidsConnection: RapidsConnection,
    inntektClient: InntektClient,
) : InntektOpplysningsbehovLøser(
        rapidsConnection = rapidsConnection,
        inntektClient = inntektClient,
    ) {
    override val behov = "InntektSiste36Mnd"

    override fun løsning(inntekt: OppslagInntekt): Map<String, Any> {
        return mapOf(
            behov to inntekt.inntektSiste36Mnd(fangstOgFisk = false),
        )
    }
}
