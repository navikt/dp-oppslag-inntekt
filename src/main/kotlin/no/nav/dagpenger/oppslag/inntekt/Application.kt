package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.oppslag.inntekt.rivers.quiz.GrunnbeløpService
import no.nav.dagpenger.oppslag.inntekt.rivers.quiz.InntektNesteMånedService
import no.nav.dagpenger.oppslag.inntekt.rivers.quiz.InntektService
import no.nav.dagpenger.oppslag.inntekt.rivers.quiz.InntektsrapporteringsperiodeLøsningService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val inntektClient =
        InntektClient(
            tokenProvider = { Configuration.dpInntektApiTokenProvider.clientCredentials(Configuration.dpInntektApiScope).accessToken },
        )
    RapidApplication.create(Configuration.asMap()).also { rapidsConnection ->
        InntektService(rapidsConnection, inntektClient)
        InntektNesteMånedService(rapidsConnection, inntektClient)
        SykepengerLøsningService(rapidsConnection, inntektClient)
        InntektsrapporteringsperiodeLøsningService(rapidsConnection)
        GrunnbeløpService(rapidsConnection)
        // InntektBehovløser(rapidsConnection, inntektClient)
        // InntektIdBehovløser(rapidsConnection, inntektClient)
    }.start()
}
