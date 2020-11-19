package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.oppslag.inntektimport.InntektClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(Configuration.kafka.rapidApplication).also {
        InntektService(it, InntektClient())
    }.start()
}
