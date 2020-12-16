package no.nav.dagpenger.oppslag.inntekt

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InntektsrapporteringperiodeTest {
    @Test
    fun `fom-dato er dagen etter til-og-med-dato`() {
        with(Inntektsrapporteringperiode(15.desember(2019))) {
            assertEquals(6.desember(2019), fom())
            assertEquals(6.januar(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.januar(2020))) {
            assertEquals(7.januar(2020), fom())
            assertEquals(5.februar(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.februar(2020))) {
            assertEquals(6.februar(2020), fom())
            assertEquals(5.mars(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.mars(2020))) {
            assertEquals(6.mars(2020), fom())
            assertEquals(6.april(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.april(2020))) {
            assertEquals(7.april(2020), fom())
            assertEquals(5.mai(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.august(2020))) {
            assertEquals(6.august(2020), fom())
            assertEquals(7.september(2020), tom())
        }
        with(Inntektsrapporteringperiode(15.september(2020))) {
            assertEquals(8.september(2020), fom())
            assertEquals(5.oktober(2020), tom())
        }
    }

    @Test
    fun grenseverdier() {
        with(Inntektsrapporteringperiode(15.mars(2015))) {
            assertEquals(6.mars(2015), fom())
            assertEquals(7.april(2015), tom())
        }
        with(Inntektsrapporteringperiode(15.april(2015))) {
            assertEquals(8.april(2015), fom())
            assertEquals(5.mai(2015), tom())
        }
        with(Inntektsrapporteringperiode(8.april(2015))) {
            assertEquals(8.april(2015), fom())
            assertEquals(5.mai(2015), tom())
        }
        with(Inntektsrapporteringperiode(7.april(2015))) {
            assertEquals(6.mars(2015), fom())
            assertEquals(7.april(2015), tom())
        }
        with(Inntektsrapporteringperiode(5.juni(2015))) {
            assertEquals(6.mai(2015), fom())
            assertEquals(5.juni(2015), tom())
        }
        with(Inntektsrapporteringperiode(7.april(2015))) {
            assertEquals(6.mars(2015), fom())
            assertEquals(7.april(2015), tom())
        }
        with(Inntektsrapporteringperiode(1.juni(2015))) {
            assertEquals(6.mai(2015), fom())
            assertEquals(5.juni(2015), tom())
        }
    }
}
