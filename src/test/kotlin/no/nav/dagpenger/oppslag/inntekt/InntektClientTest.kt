package no.nav.dagpenger.oppslag.inntekt

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.InntektsPerioder
import no.nav.dagpenger.inntekt.v1.sumInntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

class InntektClientTest {
    @Test
    fun `hent inntekt fra v3 endepunktet`() =
        runBlocking {
            val response =
                InntektClient(
                    httpClient(
                        engine =
                            MockEngine { request ->
                                assertEquals(HttpMethod.Post, request.method)
                                assertEquals("application/json", request.body.contentType.toString())
                                assertEquals("http://dp-inntekt-api/v3/inntekt/klassifisert", request.url.toString())
                                assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])

                                val requestBody =
                                    JsonMapper.objectMapper.readTree(ByteArrayInputStream((request.body.toByteArray())))
                                assertEquals("123", requestBody["personIdentifikator"].asText())
                                assertEquals(LocalDate.now(), requestBody["beregningsDato"].asLocalDate())
                                assertEquals(
                                    "${YearMonth.now().minusMonths(36)}",
                                    requestBody["periodeFraOgMed"].asText(),
                                )
                                assertEquals("${YearMonth.now()}", requestBody["periodeTilOgMed"].asText())
                                with(requestBody["regelkontekst"]) {
                                    assertEquals("12345", this["id"].asText())
                                    assertEquals("saksbehandling", this["type"].asText())
                                }
                                respond(inntektRespons, headers = headersOf("Content-Type", "application/json"))
                            },
                    ),
                    tokenProvider = { "token" },
                ).hentKlassifisertInntektV3(
                    KlassifisertInntektRequestDto(
                        personIdentifikator = "123",
                        regelkontekst =
                            RegelKontekst(
                                id = "12345",
                                type = "saksbehandling",
                            ),
                        beregningsDato = LocalDate.now(),
                        periodeFraOgMed = YearMonth.now().minusMonths(36),
                        periodeTilOgMed = YearMonth.now(),
                    ),
                )

            val inntekter = response.splitIntoInntektsPerioder()
            assertEquals(BigDecimal("0"), inntekter.first.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT)))
            assertEquals(
                BigDecimal("18900"),
                summer36(inntekter),
            )
        }

    @Test
    fun `http call med inntektId`() =
        withLoggingContext("behovId" to "foobar") {
            runBlocking {
                val id = "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5"
                val response =
                    InntektClient(
                        httpClient(
                            engine =
                                MockEngine { request ->
                                    assertEquals(HttpMethod.Get, request.method)
                                    assertEquals("http://dp-inntekt-api/v2/inntekt/klassifisert/$id", request.url.toString())
                                    assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
                                    assertEquals("foobar", request.headers[HttpHeaders.XCorrelationId])

                                    respond(inntektRespons, headers = headersOf("Content-Type", "application/json"))
                                },
                        ),
                        tokenProvider = { "token" },
                    ).hentInntekt(id)
                val inntekter = response.splitIntoInntektsPerioder()
                assertEquals(BigDecimal("0"), inntekter.first.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT)))
                assertEquals(
                    BigDecimal("18900"),
                    summer36(inntekter),
                )
            }
        }

    @Test
    fun `sjekk om bruker har inntekt`() =
        withLoggingContext("behovId" to "foobar") {
            runBlocking {
                val ident = "123123123"
                val prøvingsdato = LocalDate.now()

                val response =
                    InntektClient(
                        httpClient(
                            engine =
                                MockEngine { request ->
                                    assertEquals(HttpMethod.Post, request.method)
                                    assertEquals(
                                        "http://dp-inntekt-api/v3/inntekt/harInntekt",
                                        request.url.toString(),
                                    )
                                    assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
                                    assertEquals("foobar", request.headers[HttpHeaders.XCorrelationId])

                                    respond("true", headers = headersOf("Content-Type", "application/json"))
                                },
                        ),
                        tokenProvider = { "token" },
                    ).harInntekt(ident, YearMonth.from(prøvingsdato))

                assertTrue(response)
            }
        }

    private fun summer36(inntekter: InntektsPerioder) =
        inntekter.second.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT)) +
            inntekter.third.sumInntekt(
                listOf(InntektKlasse.ARBEIDSINNTEKT),
            )
}

@Language("JSON")
private val inntektRespons: String =
    """
    {
      "inntektsId": "12345",
      "sisteAvsluttendeKalenderMåned": "2020-10",
      "inntektsListe": [
        {
          "årMåned": "2018-08",
          "klassifiserteInntekter": [
            {
              "beløp": "18900",
              "inntektKlasse": "ARBEIDSINNTEKT"
            }
          ]
        }
      ],
      "begrunnelseManueltRedigert": null
    }
    """.trimIndent()
