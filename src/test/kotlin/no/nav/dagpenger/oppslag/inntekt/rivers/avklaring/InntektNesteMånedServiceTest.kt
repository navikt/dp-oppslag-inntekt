package no.nav.dagpenger.oppslag.inntekt.rivers.avklaring

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

internal class InntektNesteMånedServiceTest {
    private val behandlingId = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")
    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `skal sjekke om det finnes inntekt for neste måned`() {
        val inntekt =
            Inntekt(
                "123",
                listOf(
                    KlassifisertInntektMåned(
                        YearMonth.of(2021, 4),
                        listOf(KlassifisertInntekt(BigDecimal.ONE, InntektKlasse.ARBEIDSINNTEKT)),
                    ),
                ),
                sisteAvsluttendeKalenderMåned = YearMonth.of(2021, 4),
            )

        val inntektClient =
            mockk<InntektClient>().also {
                coEvery {
                    it.hentKlassifisertInntekt(
                        behandlingId = behandlingId,
                        fødselsnummer = "12345678911",
                        prøvingsdato = LocalDate.parse("2021-05-06"),
                        callId = any(),
                    )
                } returns inntekt
            }

        InntektNesteMånedService(testRapid, inntektClient)
        testRapid.sendTestMessage(behovForInntektNesteMåned)

        assertEquals(1, testRapid.inspektør.size)
        assertEquals(true, testRapid.inspektør.message(0)["@løsning"]["HarRapportertInntektNesteMåned"].asBoolean())
    }

    // language=JSON
    private val behovForInntektNesteMåned =
        """
        {
          "@event_name": "faktum_svar",
          "@opprettet": "2021-11-18T11:04:32.867824",
          "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
          "Virkningstidspunkt": "2021-05-02",
          "behandlingId" : "$behandlingId",
          "ident" : "12345678911",
          "FangstOgFiske": false,
          "fakta": [
            {
              "id": "9",
              "behov": "HarRapportertInntektNesteMåned"
            }
          ],
          "@behov": [
            "InntektSiste12Mnd", "InntektSiste3År", "HarRapportertInntektNesteMåned"
          ]
        }
        """.trimIndent()
}
