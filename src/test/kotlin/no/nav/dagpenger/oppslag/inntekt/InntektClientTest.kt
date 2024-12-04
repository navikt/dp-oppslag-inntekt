package no.nav.dagpenger.oppslag.inntekt

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import mu.withLoggingContext
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.InntektsPerioder
import no.nav.dagpenger.inntekt.v1.sumInntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class InntektClientTest {
    @Test
    fun `http call`() =
        runBlocking {
            val id = "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5"
            val response =
                InntektClient(
                    httpClient(
                        engine =
                            MockEngine { request ->
                                assertEquals(HttpMethod.Post, request.method)
                                assertEquals("application/json", request.body.contentType.toString())
                                assertEquals(Configuration.inntektApiUrl, request.url.toString())
                                assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])

                                val requestBody =
                                    JsonMapper.objectMapper.readTree(ByteArrayInputStream((request.body.toByteArray())))
                                assertEquals("123", requestBody["aktørId"].asText())
                                assertEquals("fnr", requestBody["fødselsnummer"].asText())
                                assertEquals(id, requestBody["regelkontekst"]["id"].asText())
                                assertEquals("saksbehandling", requestBody["regelkontekst"]["type"].asText())
                                assertEquals(LocalDate.now(), requestBody["beregningsDato"].asLocalDate())
                                respond(inntektRespons, headers = headersOf("Content-Type", "application/json"))
                            },
                    ),
                    tokenProvider = { "token" },
                ).hentKlassifisertInntekt(UUID.fromString(id), "123", "fnr", LocalDate.now())

            val inntekter = response.splitIntoInntektsPerioder()
            assertEquals(BigDecimal("0"), inntekter.first.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT)))
            assertEquals(
                BigDecimal("18900"),
                summer36(inntekter),
            )
        }

    private fun summer36(inntekter: InntektsPerioder) =
        inntekter.second.sumInntekt(listOf(InntektKlasse.ARBEIDSINNTEKT)) +
            inntekter.third.sumInntekt(
                listOf(InntektKlasse.ARBEIDSINNTEKT),
            )

    @Test
    @Disabled("Vi kan ikke lage flere instanser av InntektClient uten at det feiler på grunn av prometheus metrikker")
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
                                    assertEquals(Configuration.inntektApiUrl + "/$id", request.url.toString())
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
}

val inntektRespons =
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
      ]
    }
    """.trimIndent()
