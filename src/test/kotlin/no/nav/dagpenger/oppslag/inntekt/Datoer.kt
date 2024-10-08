package no.nav.dagpenger.oppslag.inntekt

import java.time.LocalDate

private val mandagsfrø = LocalDate.of(2018, 1, 1)
val Int.mandag get() = mandagsfrø.plusWeeks(this.toLong() - 1)
val Int.tirsdag get() = this.mandag.plusDays(1)
val Int.onsdag get() = this.mandag.plusDays(2)
val Int.torsdag get() = this.mandag.plusDays(3)
val Int.fredag get() = this.mandag.plusDays(4)
val Int.lørdag get() = this.mandag.plusDays(5)
val Int.søndag get() = this.mandag.plusDays(6)

fun Int.januar(year: Int = 2018) = LocalDate.of(year, 1, this)

fun Int.februar(year: Int = 2018) = LocalDate.of(year, 2, this)

fun Int.mars(year: Int = 2018) = LocalDate.of(year, 3, this)

fun Int.april(year: Int = 2018) = LocalDate.of(year, 4, this)

fun Int.mai(year: Int = 2018) = LocalDate.of(year, 5, this)

fun Int.juni(year: Int = 2018) = LocalDate.of(year, 6, this)

fun Int.juli(year: Int = 2018) = LocalDate.of(year, 7, this)

fun Int.august(year: Int = 2018) = LocalDate.of(year, 8, this)

fun Int.september(year: Int = 2018) = LocalDate.of(year, 9, this)

fun Int.oktober(year: Int = 2018) = LocalDate.of(year, 10, this)

fun Int.november(year: Int = 2018) = LocalDate.of(year, 11, this)

fun Int.desember(year: Int = 2018) = LocalDate.of(year, 12, this)

val Int.januar get() = this.januar()
val Int.februar get() = this.februar()
val Int.mars get() = this.mars()
val Int.april get() = this.april()
val Int.mai get() = this.mai()
val Int.juni get() = this.juni()
val Int.juli get() = this.juli()
val Int.august get() = this.august()
val Int.september get() = this.september()
val Int.oktober get() = this.oktober()
val Int.november get() = this.november()
val Int.desember get() = this.desember()
