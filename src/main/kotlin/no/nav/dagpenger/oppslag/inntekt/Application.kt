package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val inntektClient =
        InntektClient(
            tokenProvider = { Configuration.dpInntektApiTokenProvider.clientCredentials(Configuration.dpInntektApiScope).accessToken },
        )
    RapidApplication.create(Configuration.asMap()).also {
        InntektService(it, inntektClient)
        InntektNesteMånedService(it, inntektClient)
        SykepengerLøsningService(it, inntektClient)
        InntektsrapporteringsperiodeLøsningService(it)
        GrunnbeløpService(it)
        FangstOgFiskeInntektLøsningService(it, inntektClient)
    }.start()
}
