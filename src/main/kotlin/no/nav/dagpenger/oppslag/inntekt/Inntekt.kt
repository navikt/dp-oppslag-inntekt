package no.nav.dagpenger.oppslag.inntekt

import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.all
import no.nav.dagpenger.events.inntekt.v1.sumInntekt
import java.time.YearMonth

internal class Inntekt(private val inntekt: Inntekt) {
    private val inntektsPerioder = inntekt.splitIntoInntektsPerioder()

    fun inntektSiste12mnd(fangstOgFisk: Boolean) =
        inntektsPerioder.first.sumInntekt(inntektsklasser(fangstOgFisk))

    fun inntektSiste3år(fangstOgFisk: Boolean) =
        inntektsPerioder.all().sumInntekt(inntektsklasser(fangstOgFisk))

    private fun inntektsklasser(fangstOgFisk: Boolean) =
        if (fangstOgFisk) listOf(InntektKlasse.ARBEIDSINNTEKT, InntektKlasse.FANGST_FISKE)
        else listOf(InntektKlasse.ARBEIDSINNTEKT)

    fun inneholderSykepenger() =
        inntekt.inntektsListe.any {
            it.klassifiserteInntekter.any {
                it.inntektKlasse in listOf(InntektKlasse.SYKEPENGER, InntektKlasse.SYKEPENGER_FANGST_FISKE)
            }
        }

    fun harRapportertInntektForMåned(måned: YearMonth) =
        inntekt.inntektsListe.any { it.årMåned == måned && it.klassifiserteInntekter.isNotEmpty() }
}
