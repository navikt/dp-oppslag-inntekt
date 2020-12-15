package no.nav.dagpenger.oppslag.inntekt

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class InntektsrapporteringperiodeTest {
    @Test
    fun `fom-dato er 5 eller første arbeidsdag etter dette`() {
        LocalDate.of(2020, 12, 15).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 12, 7), fom)
        }
        LocalDate.of(2020, 12, 1).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
        LocalDate.of(2020, 11, 30).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
        LocalDate.of(2020, 11, 5).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
        LocalDate.of(2015, 4, 10).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2015, 4, 7), fom)
        }
        LocalDate.of(2015, 4, 6).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2015, 3, 5), fom)
        }
    }
    @Test
    fun `fom-daassdato er 5 eller første arbeidsdag etter dette`() {
        LocalDate.of(2020, 12, 15).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 12, 7), fom)
        }
        LocalDate.of(2020, 12, 1).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
        LocalDate.of(2020, 11, 30).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
        LocalDate.of(2020, 11, 5).also {
            val fom = Inntektsrapporteringperiode(it).fom()
            assertEquals(LocalDate.of(2020, 11, 5), fom)
        }
    }
}