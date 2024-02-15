package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.oppslag.inntekt.rivers.opplysning.InntektSiste12MndBehovløser
import no.nav.dagpenger.oppslag.inntekt.rivers.opplysning.InntektSiste36MndBehovløser
import no.nav.dagpenger.oppslag.inntekt.rivers.quiz.FangstOgFiskeInntektLøsningService
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
        FangstOgFiskeInntektLøsningService(rapidsConnection, inntektClient)
        InntektSiste12MndBehovløser(rapidsConnection, inntektClient)
        InntektSiste36MndBehovløser(rapidsConnection, inntektClient)
    }.start()
}
