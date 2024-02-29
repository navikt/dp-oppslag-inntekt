package no.nav.dagpenger.oppslag.inntekt.rivers.quiz

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

internal class InntektServiceTest {
    private val søknadUUID = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")
    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal hente inntekter for riktig pakke`() {
        val mockk =
            mockk<OppslagInntekt>(relaxed = true).also {
                every { it.inntektSiste12mndMed(false) } returns BigDecimal.ONE
                every { it.inntektSiste3årMed(false) } returns BigDecimal("2.0123543")
            }
        val inntektClient =
            mockk<InntektClient>().also {
                coEvery {
                    it.hentKlassifisertInntekt(
                        søknadUUID,
                        "32542134",
                        "32542134",
                        LocalDate.parse("2020-11-18"),
                        callId = any(),
                    )
                } returns mockk
            }

        InntektService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        assertEquals(
            BigDecimal.ONE,
            testRapid.inspektør.message(0)["@løsning"]["InntektSiste12Mnd"].asText().toBigDecimal(),
        )
        assertEquals(
            BigDecimal("2.0123543"),
            testRapid.inspektør.message(0)["@løsning"]["InntektSiste3År"].asText().toBigDecimal(),
        )
        coVerify {
            inntektClient.hentKlassifisertInntekt(
                søknadUUID,
                "32542134",
                "32542134",
                LocalDate.parse("2020-11-18"),
                callId = any(),
            )
        }
    }

    @Test
    fun `skal droppe behov hvor aktørid mangler`() {
        InntektService(testRapid, mockk())
        testRapid.sendTestMessage(behovUtenIdent)
        assertEquals(0, testRapid.inspektør.size)
    }

    // language=JSON
    private val behovJson =
        """
        {
          "@event_name": "faktum_svar",
          "@opprettet": "2020-11-18T11:04:32.867824",
          "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "Virkningstidspunkt": "2020-11-18",
          "søknad_uuid": "$søknadUUID",
          "identer": [
            {
              "id": "32542134",
              "type": "aktørid",
              "historisk": false
            },
            {"id":"32542134","type":"folkeregisterident","historisk":false}
          ],
          "FangstOgFiskeInntektSiste36mnd": false,
          "fakta": [
            {
              "id": "7",
              "behov": "InntektSiste12Mnd"
            },
            {
              "id": "8",
              "behov": "InntektSiste3År"
            },
            {
              "id": "9",
              "behov": "hubba"
            }
          ],
          "@behov": [
            "InntektSiste12Mnd",
            "InntektSiste3År"
          ]
        }
        """.trimIndent()

    // language=JSON
    private val behovUtenIdent =
        """
        {
          "@event_name": "faktum_svar",
          "@opprettet": "2020-11-18T11:04:32.867824",
          "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "Virkningstidspunkt": "2020-11-18",
          "søknad_uuid": "$søknadUUID",
          "identer":[],
          "FangstOgFiskeInntektSiste36mnd": false,
          "fakta": [
            {
              "id": "7",
              "behov": "InntektSiste12Mnd"
            },
            {
              "id": "8",
              "behov": "InntektSiste3År"
            },
            {
              "id": "9",
              "behov": "hubba"
            }
          ],
          "@behov": [
            "InntektSiste12Mnd", "InntektSiste3År"
          ]
        }
        """.trimIndent()
}
