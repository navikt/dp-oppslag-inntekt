package no.nav.dagpenger.oppslag.inntekt.rivers.quiz

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GrunnbeløpServiceTest {
    @Test
    fun `Grunnbeløp`() {
        val testRapid = TestRapid()
        GrunnbeløpService(testRapid)

        testRapid.sendTestMessage(behovJson)
        Assertions.assertEquals(1, testRapid.inspektør.size)

        val message = testRapid.inspektør.message(0)
        Assertions.assertEquals("faktum_svar", message["@event_name"].asText())
        Assertions.assertTrue(message["@løsning"].has("Grunnbeløp"))
    }
}

// language=JSON
private val behovJson =
    """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [
        {
          "behov": "Grunnbeløp"
        }
      ],
      "@behov": [
        "Grunnbeløp"
      ],
      "Virkningstidspunkt": "2020-01-01"
    }
    """.trimIndent()
