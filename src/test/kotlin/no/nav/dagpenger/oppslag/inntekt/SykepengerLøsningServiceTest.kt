package no.nav.dagpenger.oppslag.inntekt

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

internal class SykepengerLøsningServiceTest {
    private val søknadUUID = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")
    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal besvare behov om inntekt inneholder sykepenger siste 36 mnd`() {
        val mockk = mockk<OppslagInntekt>(relaxed = true).also {
            every { it.inneholderSykepenger() } returns true
        }
        val inntektClient = mockk<InntektClient>().also {
            coEvery {
                it.hentKlassifisertInntekt(
                    søknadUUID,
                    "32542134",
                    LocalDate.parse("2020-11-18"),
                    callId = any()
                )
            } returns mockk
        }

        SykepengerLøsningService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        assertTrue(testRapid.inspektør.message(0)["@løsning"]["SykepengerSiste36Måneder"].asBoolean())
        coVerify {
            inntektClient.hentKlassifisertInntekt(
                søknadUUID,
                "32542134",
                LocalDate.parse("2020-11-18"),
                callId = any()
            )
        }
    }

    @Test
    fun `skal droppe behov hvor aktørid mangler`() {
        InntektService(testRapid, mockk())
        testRapid.sendTestMessage(behovUtenAktørIdJson)
        assertEquals(0, testRapid.inspektør.size)
    }

    // language=JSON
    private val behovJson =
        """{
  "@event_name": "faktum_svar",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "Virkningstidspunkt": "2020-11-18",
  "søknad_uuid": "$søknadUUID",
  "identer":[{"id":"32542134","type":"aktørid","historisk":false}],
  "FangstOgFiskeInntektSiste36mnd": false,
  "fakta": [
    {
      "id": "29",
      "behov": "SykepengerSiste36Måneder"
    }
  ],
  "@behov": [
    "SykepengerSiste36Måneder"
  ]
}
        """.trimIndent()

    // language=JSON
    private val behovUtenAktørIdJson =
        """{
  "@event_name": "faktum_svar",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "Virkningstidspunkt": "2020-11-18",
  "søknad_uuid": "$søknadUUID",
  "identer":[{"id":"32542134","type":"folkeregisterident","historisk":false}],
  "FangstOgFiskeInntektSiste36mnd": false,
  "fakta": [
    {
      "id": "29",
      "behov": "SykepengerSiste36Måneder"
    }
  ],
  "@behov": [
    "SykepengerSiste36Måneder"
  ]
}
        """.trimIndent()
}
