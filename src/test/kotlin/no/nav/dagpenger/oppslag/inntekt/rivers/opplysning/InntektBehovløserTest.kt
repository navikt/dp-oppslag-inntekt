package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.JsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class InntektBehovløserTest {
    val inntektId = "inntektId"
    val inntekt =
        Inntekt(
            inntektsId = inntektId,
            sisteAvsluttendeKalenderMåned = YearMonth.of(2024, 8),
            inntektsListe =
                listOf(
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.of(2024, 8),
                        klassifiserteInntekter =
                            listOf(
                                KlassifisertInntekt(
                                    inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                    beløp = BigDecimal("111111"),
                                ),
                            ),
                    ),
                ),
        )

    private val inntektClient =
        mockk<InntektClient>().also {
            coEvery {
                it.hentKlassifisertInntekt(
                    søknadUUID = any(),
                    aktørId = any(),
                    fødselsnummer = "12345678911",
                    virkningsTidspunkt = LocalDate.parse("2024-01-01"),
                    callId = any(),
                )
            } returns inntekt
        }

    private val testRapid =
        TestRapid().also {
            InntektBehovløser(it, inntektClient)
        }

    @Test
    fun `løser inntekt opplysningsbehov`() {
        testRapid.sendTestMessage(behovJson)
        val inspektør = testRapid.inspektør
        assertEquals(1, inspektør.size)
        inspektør.message(0).also { løsning ->
            val json = løsning["@løsning"]["Inntekt"]["verdi"]
            val behovet = JsonMapper.objectMapper.readValue<Inntekt>(json.toString())

            assertEquals(inntektId, behovet.inntektsId)
            assertEquals(inntekt.sisteAvsluttendeKalenderMåned, behovet.sisteAvsluttendeKalenderMåned)
            assertEquals(inntekt.inntektsListe, behovet.inntektsListe)
            assertEquals(inntekt.manueltRedigert, behovet.manueltRedigert)
        }
    }

    // language=JSON
    private val behovJson =
        """
        {
          "@event_name": "behov",
          "@behovId": "d79bd133-94bb-4cfe-92ef-6b578191462f",
          "@behov": [
            "Inntekt"
          ],
          "ident": "12345678911",
          "behandlingId": "018dac22-2664-7724-95a6-2cd1ed9d3a07",
          "Inntekt": {
            "Virkningsdato": "2024-01-01"
          },
        
          "@id": "f0d97980-93e0-4638-8a61-337978466d7e",
          "@opprettet": "2024-02-15T10:38:55.821778",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "f0d97980-93e0-4638-8a61-337978466d7e",
              "time": "2024-02-15T10:38:55.821778"
            }
          ]
        }

        """.trimIndent()
}
