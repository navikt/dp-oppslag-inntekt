package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.oppslag.inntekt.rivers.avklaring.InntektNesteMånedService
import no.nav.dagpenger.oppslag.inntekt.rivers.avklaring.SykepengerLøsningService
import no.nav.dagpenger.oppslag.inntekt.rivers.opplysning.InntektBehovløser
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val inntektClient =
        InntektClient(
            tokenProvider = {
                Configuration.dpInntektApiTokenProvider.clientCredentials(Configuration.dpInntektApiScope).access_token
                    ?: throw RuntimeException("Klarte ikke hente token")
            },
        )
    RapidApplication
        .create(Configuration.asMap())
        .also { rapidsConnection ->
            InntektNesteMånedService(rapidsConnection, inntektClient)
            SykepengerLøsningService(rapidsConnection, inntektClient)
            InntektBehovløser(rapidsConnection, inntektClient)
        }.start()
}
