package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

internal class InntektOpplysningsbehovLøserTest {
    val inntektId = "InntektID"
    val oppslagMock =
        mockk<OppslagInntekt>(relaxed = true).also {
            every { it.inntektId() } returns inntektId
            every { it.inntektSiste12mndMed(false) } returns BigDecimal("111111")
            every { it.inntektSiste36Mnd(false) } returns BigDecimal("222222")
        }
    val inntektClient =
        mockk<InntektClient>().also {
            coEvery {
                it.hentKlassifisertInntekt(
                    any(),
                    null,
                    "12345678911",
                    LocalDate.parse("2024-02-15"),
                    callId = any(),
                )
            } returns oppslagMock
        }
    private val testRapid =
        TestRapid().also {
            InntektSiste12MndBehovløser(it, inntektClient)
            InntektSiste36MndBehovløser(it, inntektClient)
        }

    @Test
    fun `løser inntekt opplysningsbehov`() {
        testRapid.sendTestMessage(behovJson)
        val inspektør = testRapid.inspektør
        assertEquals(2, inspektør.size)
        inspektør.message(0).also { løsning ->
            løsning["@løsning"]["InntektSiste12Mnd"].asText().toBigDecimal().also {
                assertEquals(it, BigDecimal("111111"))
            }
            assertEquals(inntektId, løsning["inntektId"].asText())
        }
        inspektør.message(1).also { løsning ->
            løsning["@løsning"]["InntektSiste36Mnd"].asText().toBigDecimal().also {
                assertEquals(it, BigDecimal("222222"))
            }
            assertEquals(inntektId, løsning["inntektId"].asText())
        }
    }

    // language=JSON
    private val behovJson =
        """
        {
          "@event_name": "behov",
          "@behovId": "d79bd133-94bb-4cfe-92ef-6b578191462f",
          "@behov": [
            "InntektSiste12Mnd",
            "InntektSiste36Mnd"
          ],
          "ident": "12345678911",
          "behandlingId": "018dac22-2664-7724-95a6-2cd1ed9d3a07",
          "InntektSiste12Mnd": {
            "Virkningsdato": "2024-02-15"
          },
          "InntektSiste36Mnd": {
            "Virkningsdato": "2024-02-15"
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
