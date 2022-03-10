package no.nav.dagpenger.oppslag.inntekt

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class InntektNesteMånedServiceTest {
    private val søknadUUID = UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5")


    private val testRapid = TestRapid()

    @AfterEach
    fun reset() {
        testRapid.reset()
    }


    @Test
    fun `skal sjekke om det finnes inntekt for neste måned`() {
        val inntekt = Inntekt(
            no.nav.dagpenger.events.inntekt.v1.Inntekt(
                "123",
                listOf(
                    KlassifisertInntektMåned(
                        YearMonth.of(2020, 11),
                        listOf(KlassifisertInntekt(BigDecimal.ONE, InntektKlasse.ARBEIDSINNTEKT))
                    ),
                    KlassifisertInntektMåned(
                        YearMonth.of(2020, 12),
                        listOf(KlassifisertInntekt(BigDecimal.ONE, InntektKlasse.ARBEIDSINNTEKT))
                    ),
                    KlassifisertInntektMåned(
                        YearMonth.of(2021, 5),
                        listOf(KlassifisertInntekt(BigDecimal.ONE, InntektKlasse.ARBEIDSINNTEKT))
                    )
                ),
                sisteAvsluttendeKalenderMåned = YearMonth.of(2021, 3)
            )
        )

        val inntektClient = mockk<InntektClient>().also {
            coEvery { it.hentKlassifisertInntekt(søknadUUID, "32542134", LocalDate.parse("2021-06-08")) } returns inntekt
        }

        InntektNesteMånedService(testRapid, inntektClient)
        testRapid.sendTestMessage(behovForInntektNesteMåned)

        assertEquals(1, testRapid.inspektør.size)
        assertEquals(true, testRapid.inspektør.message(0)["@løsning"]["HarRapportertInntektNesteMåned"].asBoolean())
    }


    @Test
    fun `skal droppe behov hvor aktørid mangler`() {
        InntektService(testRapid, mockk())
        testRapid.sendTestMessage(behovUtenAktørIdJson)
        assertEquals(0, testRapid.inspektør.size)
    }


    // language=JSON
    private val behovForInntektNesteMåned =
        """{
  "@event_name": "faktum_svar",
  "@opprettet": "2021-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "Virkningstidspunkt": "2021-05-06",
  "søknad_uuid": "$søknadUUID",
  "identer":[{"id":"32542134","type":"aktørid","historisk":false}],
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


    // language=JSON
    private val behovUtenAktørIdJson =
        """{
  "@event_name": "faktum_svar",
  "@opprettet": "2021-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "Virkningstidspunkt": "2021-05-06",
  "søknad_uuid": "$søknadUUID",
  "identer":[{"id":"32542134","type":"folkeregisterident","historisk":false}],
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
