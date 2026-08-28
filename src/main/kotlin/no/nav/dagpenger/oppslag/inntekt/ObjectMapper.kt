package no.nav.dagpenger.oppslag.inntekt

import tools.jackson.module.kotlin.jacksonObjectMapper

object ObjectMapper {
    internal val objectMapper =
        jacksonObjectMapper()
}
