package no.nav.dagpenger.oppslag.inntekt.rivers.avklaring

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

internal class SykepengerLøsningServiceTest {
    private val behandlingId = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")
    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal besvare Ja behov der inntekt inneholder sykepenger siste 36 mnd`() {
        val inntekt =
            Inntekt(
                inntektsId = "inntektId",
                sisteAvsluttendeKalenderMåned = YearMonth.of(2020, 8),
                inntektsListe =
                    listOf(
                        KlassifisertInntektMåned(
                            årMåned = YearMonth.of(2020, 11),
                            klassifiserteInntekter =
                                listOf(
                                    KlassifisertInntekt(
                                        inntektKlasse = InntektKlasse.SYKEPENGER,
                                        beløp = BigDecimal("111111"),
                                    ),
                                ),
                        ),
                    ),
            )
        val inntektClient =
            mockk<InntektClient>().also {
                coEvery {
                    it.hentKlassifisertInntekt(
                        behandlingId = behandlingId,
                        fødselsnummer = "12345678911",
                        virkningsTidspunkt = LocalDate.parse("2020-11-18"),
                        callId = any(),
                    )
                } returns inntekt
            }

        SykepengerLøsningService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        assertTrue(testRapid.inspektør.message(0)["@løsning"]["SykepengerSiste36Måneder"].asBoolean())
        coVerify {
            inntektClient.hentKlassifisertInntekt(
                behandlingId = behandlingId,
                fødselsnummer = "12345678911",
                virkningsTidspunkt = LocalDate.parse("2020-11-18"),
                callId = any(),
            )
        }
    }

    @Test
    fun `skal besvare Nei på behov der inntekt ikke inneholder sykepenger siste 36 mnd`() {
        val inntekt =
            Inntekt(
                inntektsId = "inntektId",
                sisteAvsluttendeKalenderMåned = YearMonth.of(2020, 8),
                inntektsListe =
                    listOf(
                        KlassifisertInntektMåned(
                            årMåned = YearMonth.of(2020, 11),
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
        val inntektClient =
            mockk<InntektClient>().also {
                coEvery {
                    it.hentKlassifisertInntekt(
                        behandlingId = behandlingId,
                        fødselsnummer = "12345678911",
                        virkningsTidspunkt = LocalDate.parse("2020-11-18"),
                        callId = any(),
                    )
                } returns inntekt
            }

        SykepengerLøsningService(testRapid, inntektClient)

        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        assertFalse(testRapid.inspektør.message(0)["@løsning"]["SykepengerSiste36Måneder"].asBoolean())
        coVerify {
            inntektClient.hentKlassifisertInntekt(
                behandlingId = behandlingId,
                fødselsnummer = "12345678911",
                virkningsTidspunkt = LocalDate.parse("2020-11-18"),
                callId = any(),
            )
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
          "Virkningstidspunkt": "2020-11-18",
          "behandlingId": "$behandlingId",
          "ident" : "12345678911",
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
