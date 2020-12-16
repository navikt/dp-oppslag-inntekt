package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class InntektsrapporteringsperiodeLøsningServiceTest {

    @Test
    fun `Løser behov for Innteksrapporteringsperiode `() {

        val testRapid = TestRapid()
        InntektsrapporteringsperiodeLøsningService(testRapid)

        testRapid.sendTestMessage(behovJson)
        Assertions.assertEquals(1, testRapid.inspektør.size)

        val message = testRapid.inspektør.message(0)
        Assertions.assertEquals("faktum_svar", message["@event_name"].asText())
        Assertions.assertTrue(message["@løsning"].has("InntektsrapporteringsperiodeFom"))
        Assertions.assertTrue(message["@løsning"].has("InntektsrapporteringsperiodeTom"))
    }
}

@Language("json")
private val behovJson =
    """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [
        {
          "behov": "InntektsrapporteringsperiodeFom"
        },
        {
          "behov": "InntektsrapporteringsperiodeTom"
        }
      ],
      "@behov": [
        "InntektsrapporteringsperiodeFom", "InntektsrapporteringsperiodeTom"
      ],
      "Virkningstidspunkt": "2020-12-15"
    }
    """.trimIndent()
