package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.all
import no.nav.dagpenger.events.inntekt.v1.sumInntekt
import java.time.LocalDate

internal class Inntekt(private val inntekt: Inntekt) {
    val inntektsPerioder = inntekt.splitIntoInntektsPerioder()

    fun inntektSiste12mnd(virkningstidspunkt: LocalDate): Double {
        inntektsPerioder.first.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT))

    }
    fun inntektSiste3år(virkningstidspunkt: LocalDate): Double {
        inntektsPerioder.all().sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT))
        // fangst og fikse?
    }

}

/*

fun splitIntoInntektsPerioder(): InntektsPerioder {
    return Triple(
            (0L..11L).map { i ->
                inntektsListe.find { it.årMåned == sisteAvsluttendeKalenderMåned.minusMonths(i) }
                        ?: KlassifisertInntektMåned(
                                sisteAvsluttendeKalenderMåned.minusMonths(i),
                                emptyList()
                        )
            }.sortedBy { it.årMåned },
            (12L..23L).map { i ->
                inntektsListe.find { it.årMåned == sisteAvsluttendeKalenderMåned.minusMonths(i) }
                        ?: KlassifisertInntektMåned(
                                sisteAvsluttendeKalenderMåned.minusMonths(i),
                                emptyList()
                        )
            }.sortedBy { it.årMåned },
            (24L..35L).map { i ->
                inntektsListe.find { it.årMåned == sisteAvsluttendeKalenderMåned.minusMonths(i) }
                        ?: KlassifisertInntektMåned(
                                sisteAvsluttendeKalenderMåned.minusMonths(i),
                                emptyList()
                        )
            }.sortedBy { it.årMåned }
    )
}

fun filterPeriod(from: YearMonth, to: YearMonth): Inntekt {
    if (from.isAfter(to)) throw IllegalArgumentException("Argument from=$from is after argument to=$to")
    return Inntekt(
            inntektsId,
            inntektsListe.filter { it.årMåned !in from..to },
            sisteAvsluttendeKalenderMåned = sisteAvsluttendeKalenderMåned
    )

*/
