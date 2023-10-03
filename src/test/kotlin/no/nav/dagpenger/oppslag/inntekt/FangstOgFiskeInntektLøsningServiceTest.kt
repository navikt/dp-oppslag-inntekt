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

internal class FangstOgFiskeInntektLøsningServiceTest {
    private val søknadUUID = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")
    private val ident = "1234"

    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal besvare behov om inntekt inneholder fangst og fiske siste 36 mnd`() {
        val mockk =
            mockk<OppslagInntekt>(relaxed = true).also {
                every { it.inneholderFangstOgFiske() } returns true
            }
        val inntektClient =
            mockk<InntektClient>().also {
                coEvery {
                    it.hentKlassifisertInntekt(
                        søknadUUID,
                        ident,
                        null,
                        LocalDate.parse("2020-11-18"),
                        callId = any(),
                    )
                } returns mockk

                coEvery {
                    it.hentKlassifisertInntekt(
                        søknadUUID,
                        null,
                        ident,
                        LocalDate.parse("2020-11-18"),
                        callId = any(),
                    )
                } returns mockk
            }
        FangstOgFiskeInntektLøsningService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson(identType = "aktørid"))
        testRapid.sendTestMessage(behovJson(identType = "folkeregisterident"))

        assertEquals(2, testRapid.inspektør.size)
        assertTrue(testRapid.inspektør.message(0)["@løsning"]["FangstOgFiskeInntektSiste36mnd"].asBoolean())
        assertTrue(testRapid.inspektør.message(1)["@løsning"]["FangstOgFiskeInntektSiste36mnd"].asBoolean())

        coVerify(exactly = 1) {
            inntektClient.hentKlassifisertInntekt(
                søknadUUID,
                ident,
                null,
                LocalDate.parse("2020-11-18"),
                callId = any(),
            )
        }

        coVerify(exactly = 1) {
            inntektClient.hentKlassifisertInntekt(
                søknadUUID,
                null,
                ident,
                LocalDate.parse("2020-11-18"),
                callId = any(),
            )
        }
    }

    @Test
    fun `skal droppe behov hvor aktørid og fnr mangler`() {
        FangstOgFiskeInntektLøsningService(testRapid, mockk())
        testRapid.sendTestMessage(behovJson(identType = "hubba"))
        assertEquals(0, testRapid.inspektør.size)
    }

    // language=JSON
    private fun behovJson(identType: String): String {
        return """
            {
              "@event_name": "faktum_svar",
              "@opprettet": "2020-11-18T11:04:32.867824",
              "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
              "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
              "Virkningstidspunkt": "2020-11-18",
              "søknad_uuid": "$søknadUUID",
              "identer": [{
              "id": "$ident",
              "type": "$identType",
              "historisk": false
            } ],
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
}
