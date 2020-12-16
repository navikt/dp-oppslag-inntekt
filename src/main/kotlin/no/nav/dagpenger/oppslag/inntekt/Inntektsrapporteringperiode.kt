package no.nav.dagpenger.oppslag.inntekt

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class Inntektsrapporteringperiode(private val dato: LocalDate) {
    fun fom(): LocalDate {
        val månederTilbake: Long = if (tom().month == dato.month) 2 else 1
        return rapporteringsfrist(dato.minusMonths(månederTilbake)).plusDays(1)
    }

    fun tom() =
        if (dato <= førsteArbeidsdag(dato.withDayOfMonth(5)))
            førsteArbeidsdag(dato.withDayOfMonth(5))
        else rapporteringsfrist(dato)

    private fun rapporteringsfrist(dato: LocalDate) =
        førsteArbeidsdag(dato.plusMonths(1).withDayOfMonth(5))

    private fun førsteArbeidsdag(inklusivDato: LocalDate) =
        generateSequence(inklusivDato) {
            it.plusDays(1)
        }.first { it.erArbeidsdag() }

    private fun LocalDate.erArbeidsdag() = NorwegianDateUtil.isWorkingDay(this.toDate())
    private fun LocalDate.toDate() = Date.from(this.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
}
