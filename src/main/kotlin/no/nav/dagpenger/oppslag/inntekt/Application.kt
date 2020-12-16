package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(Configuration.rapidApplication).also {
        InntektService(it, InntektClient())
        InntektsrapporteringsperiodeLøsningService(it)
        GrunnbeløpService(it)
    }.start()
}
