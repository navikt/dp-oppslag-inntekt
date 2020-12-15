package no.nav.dagpenger.oppslag.inntekt

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Inntektsrapporteringperiode(private val dato: LocalDate) {
    fun fom(): LocalDate {
        var muligFom = (if (dato < dato.withDayOfMonth(5)) dato.minusMonths(1) else dato).withDayOfMonth(5)

        while (!NorwegianDateUtil.isWorkingDay(muligFom.asDate())) {
            muligFom = muligFom.plusDays(1)
        }

        return muligFom
    }

/*    fun tom(): LocalDate {

    }*/

    private fun LocalDate.asDate() = Date.from(this.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
}
