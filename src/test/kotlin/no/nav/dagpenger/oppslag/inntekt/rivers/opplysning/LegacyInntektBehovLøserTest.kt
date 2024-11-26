package no.nav.dagpenger.oppslag.inntekt.rivers.opplysning

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.oppslag.inntekt.InntektClient
import no.nav.dagpenger.oppslag.inntekt.OppslagInntekt
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.assertEquals

internal class LegacyInntektBehovLøserTest {
    val inntektId = "inntektId"
    val inntekt =
        Inntekt(
            inntektsId = inntektId,
            sisteAvsluttendeKalenderMåned = YearMonth.of(2024, 8),
            inntektsListe =
                listOf(
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.of(2024, 8),
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
    val oppslagMock =
        mockk<OppslagInntekt>(relaxed = true).also {
            every { it.inntektId() } returns inntektId
            every { it.inntekt } returns inntekt
            every { it.inntektSiste12mndMed(false) } returns BigDecimal("111111")
            every { it.inntektSiste36Mnd(false) } returns BigDecimal("222222")
        }
    val inntektClient =
        mockk<InntektClient>().also {
            coEvery {
                it.hentInntekt(
                    "inntektId",
                )
            } returns oppslagMock
        }
    private val testRapid =
        TestRapid().also {
            LegacyInntektBehovløser(it, inntektClient)
        }

    @Test
    fun `løser inntekt siste 12 mnd og inntekst siste 36 mnd opplysningsbehov`() {
        testRapid.sendTestMessage(inntektSiste12og36MndbehovJson)
        val inspektør = testRapid.inspektør
        assertEquals(1, inspektør.size)
        inspektør.message(0).also { løsning ->
            løsning["@løsning"]["InntektSiste12Mnd"]["verdi"].asText().toBigDecimal().also {
                assertEquals(it, BigDecimal("111111"))
            }
        }
        inspektør.message(0).also { løsning ->
            løsning["@løsning"]["InntektSiste36Mnd"]["verdi"].asText().toBigDecimal().also {
                assertEquals(it, BigDecimal("222222"))
            }
        }
    }

    @Test
    fun `løser behov der en henter hele inntekten basert på inntektId`() {
        testRapid.sendTestMessage(inntektBehov)
        val inspektør = testRapid.inspektør
        assertEquals(1, inspektør.size)
        inspektør.message(0).also { løsning ->
            løsning["@løsning"]["Inntekt"]["verdi"].also {
                assertEquals(inntektId, it["inntektsId"].asText())
                assertEquals("2024-08", it["sisteAvsluttendeKalenderMåned"].asText())
                assertEquals("2024-08", it["inntektsListe"][0]["årMåned"].asText())
                assertEquals("ARBEIDSINNTEKT", it["inntektsListe"][0]["klassifiserteInntekter"][0]["inntektKlasse"].asText())
                assertEquals(111111.toBigDecimal(), it["inntektsListe"][0]["klassifiserteInntekter"][0]["beløp"].asText().toBigDecimal())
            }
        }
    }

    // language=JSON
    val inntektBehov =
        """
        {
          "@event_name" : "behov",
          "@behovId" : "584da288-813f-4283-90ee-6bf929b3ecd5",
          "@behov" : [ "Inntekt" ],
          "ident" : "11109233444",
          "behandlingId" : "01916e84-2286-701c-ad55-1c81b72d0674",
          "gjelderDato" : "2024-08-20",
          "søknadId" : "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid" : "01916e84-2286-701c-ad55-1c81b72d0674",
          "opprettet" : "2024-08-20T08:40:39.049295",
          "Inntekt" : {
            "InntektId" : "$inntektId",
            "InnsendtSøknadsId" : {
              "urn" : "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
            },
            "søknad_uuid" : "01916e84-2286-701c-ad55-1c81b72d0674"
          },
          "InntektId" : "$inntektId",
          "InnsendtSøknadsId" : {
            "urn" : "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "@final" : true,
          "@id" : "8942d30a-db8a-4e1c-82e5-cb70a5390af4",
          "@opprettet" : "2024-08-20T08:44:53.091272",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "8942d30a-db8a-4e1c-82e5-cb70a5390af4",
            "time" : "2024-08-20T08:44:53.091272"
          } ]
        }
        """.trimIndent()

    // language=JSON
    private val inntektSiste12og36MndbehovJson =
        """
        {
          "@event_name": "behov",
          "@behovId": "d79bd133-94bb-4cfe-92ef-6b578191462f",
          "@behov": [
            "InntektSiste12Mnd",
            "InntektSiste36Mnd"
          ],
          "ident": "12345678911",
          "behandlingId": "018dac22-2664-7724-95a6-2cd1ed9d3a07",
          "InntektSiste12Mnd": {
            "InntektId": "$inntektId"
          },
          "InntektSiste36Mnd": {
            "InntektId": "$inntektId"
          },
          "@id": "f0d97980-93e0-4638-8a61-337978466d7e",
          "@opprettet": "2024-02-15T10:38:55.821778",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "f0d97980-93e0-4638-8a61-337978466d7e",
              "time": "2024-02-15T10:38:55.821778"
            }
          ]
        }

        """.trimIndent()
}
