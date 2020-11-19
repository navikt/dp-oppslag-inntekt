package no.nav.dagpenger.oppslag.inntekt

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class InntektServiceTest {

    @Test
    fun `skal hente inntekter for riktig pakke`() {
        val testRapid = TestRapid()

        InntektService(testRapid)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
    }

    private val behovJson =
        """
    {
      "@event_name": "behov",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [
        {
          "id": "1",
          "behov": "ØnskerDagpengerFraDato"
        }],
      "@behov": [
        "ØnskerDagpengerFraDato"
      ],
      "InnsendtSøknadsId": "123"
    }
        """.trimIndent()
}
