package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.all
import no.nav.dagpenger.events.inntekt.v1.sumInntekt
import java.math.BigDecimal

internal class Inntekt(inntekt: Inntekt) {
    private val inntektsPerioder = inntekt.splitIntoInntektsPerioder()

    fun inntektSiste12mnd(): BigDecimal {
        return inntektsPerioder.first.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT))
    }

    fun inntektSiste3år(): BigDecimal {
        return inntektsPerioder.all().sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT))
    }
}
