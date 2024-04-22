package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InntektIdBehovløserTest {
    val inntektId = "InntektID"

    val oppslagMock =
        mockk<OppslagInntekt>(relaxed = true).also {
            every { it.inntektId() } returns inntektId
        }
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
            } returns oppslagMock
        }

    private val testRapid =
        TestRapid().also {
            InntektIdBehovløser(it, inntektClient)
        }

    @Test
    fun `løser inntektId opplysningsbehov`() {
        testRapid.sendTestMessage(behovJson)
        val inspektør = testRapid.inspektør
        assertEquals(1, inspektør.size)
        inspektør.message(0).also { løsning ->
            løsning["@løsning"]["InntektId"]["verdi"].asText().also {
                assertEquals(it, inntektId)
            }
        }
    }

    // language=JSON
    private val behovJson =
        """
        {
          "@event_name": "behov",
          "@behovId": "d79bd133-94bb-4cfe-92ef-6b578191462f",
          "@behov": [
            "InntektId"
          ],
          "ident": "12345678911",
          "behandlingId": "018dac22-2664-7724-95a6-2cd1ed9d3a07",
          "InntektId": {
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
