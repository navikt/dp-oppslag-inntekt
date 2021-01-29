package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val inntektClient = InntektClient()
    RapidApplication.create(Configuration.rapidApplication).also {
        InntektService(it, inntektClient)
        InntektNesteMånedService(it, inntektClient)
        SykepengerLøsningService(it, inntektClient)
        InntektsrapporteringsperiodeLøsningService(it)
        GrunnbeløpService(it)
    }.start()
}
