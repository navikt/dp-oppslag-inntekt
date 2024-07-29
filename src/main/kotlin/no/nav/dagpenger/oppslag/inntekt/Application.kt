package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.oppslag.inntekt.rivers.avklaring.InntektNesteMånedService
import no.nav.dagpenger.oppslag.inntekt.rivers.avklaring.SykepengerLøsningService
import no.nav.dagpenger.oppslag.inntekt.rivers.opplysning.InntektBehovløser
import no.nav.dagpenger.oppslag.inntekt.rivers.opplysning.InntektIdBehovløser
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val inntektClient =
        InntektClient(
            tokenProvider = { Configuration.dpInntektApiTokenProvider.clientCredentials(Configuration.dpInntektApiScope).accessToken },
        )
    RapidApplication
        .create(Configuration.asMap())
        .also { rapidsConnection ->
            InntektNesteMånedService(rapidsConnection, inntektClient)
            SykepengerLøsningService(rapidsConnection, inntektClient)
            InntektBehovløser(rapidsConnection, inntektClient)
            InntektIdBehovløser(rapidsConnection, inntektClient)
        }.start()
}
