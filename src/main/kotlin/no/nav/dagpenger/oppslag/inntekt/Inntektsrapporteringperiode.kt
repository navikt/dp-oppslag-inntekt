package no.nav.dagpenger.oppslag.inntekt

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class Inntektsrapporteringperiode(private val dato: LocalDate) {
    fun fom(): LocalDate {
        val månederTilbake: Long = if (tom().month == dato.month) -1 else 0
        return rapporteringsfrist(dato, månedOffset = månederTilbake).plusDays(1)
    }

    fun tom() =
        if (dato <= førsteArbeidsdag(dato.withDayOfMonth(5)))
            rapporteringsfrist(dato)
        else rapporteringsfrist(dato, månedOffset = 1)

    private fun rapporteringsfrist(dato: LocalDate, månedOffset: Long = 0) =
        førsteArbeidsdag(dato.plusMonths(månedOffset).withDayOfMonth(5))

    private fun førsteArbeidsdag(inklusivDato: LocalDate) =
        generateSequence(inklusivDato) {
            it.plusDays(1)
        }.first { it.erArbeidsdag() }

    private fun LocalDate.erArbeidsdag() = NorwegianDateUtil.isWorkingDay(this.toDate())
    private fun LocalDate.toDate() = Date.from(this.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
}
