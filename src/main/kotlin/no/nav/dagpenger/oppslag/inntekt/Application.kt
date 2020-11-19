package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val configuration = Configuration()
    RapidApplication.create(configuration.kafka.rapidApplication).also {
        InntektService(it, InntektClient())
    }.start()
}
