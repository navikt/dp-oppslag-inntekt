package no.nav.dagpenger.oppslag.inntekt

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

internal class FangstOgFiskeInntektLøsningServiceTest {
    @Test
    fun `skal besvare behov om inntekt inneholder fangst og fiske siste 36 mnd`() {

        val testRapid = TestRapid()
        val mockk = mockk<Inntekt>(relaxed = true).also {
            every { it.inneholderFangstOgFiske() } returns true
        }
        val inntektClient = mockk<InntektClient>().also {
            coEvery { it.hentKlassifisertInntekt("32542134", LocalDate.parse("2020-11-18")) } returns mockk
        }

        FangstOgFiskeInntektLøsningService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        assertTrue(testRapid.inspektør.message(0)["@løsning"]["FangstOgFiskeInntektSiste36mnd"].asBoolean())
        coVerify { inntektClient.hentKlassifisertInntekt("32542134", LocalDate.parse("2020-11-18")) }
    }

    private val behovJson =
        """{
  "@event_name": "faktum_svar",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "Virkningstidspunkt": "2020-11-18",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "identer":[{"id":"32542134","type":"aktørid","historisk":false}],
  "fakta": [
    {
      "id": "29",
      "behov": "FangstOgFiskeInntektSiste36mnd"
    }
  ],
  "@behov": [
    "FangstOgFiskeInntektSiste36mnd"
  ]
}
        """.trimIndent()
}
