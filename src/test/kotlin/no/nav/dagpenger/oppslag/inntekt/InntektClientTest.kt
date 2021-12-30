package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class InntektClientTest {
    @Test
    fun `http call`() = runBlocking {
        val response = InntektClient(
            httpClient(
                engine = MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("application/json", request.body.contentType.toString())
                    assertEquals(Configuration.inntektApiUrl, request.url.toString())
                    assertEquals(Configuration.inntektApiKey, request.headers["X-API-KEY"])
                    assertEquals(
                        """{"aktørId":"123","regelkontekst":{"id":"41621ac0-f5ee-4cce-b1f5-88a79f25f1a5","type":"saksbehandling"},"beregningsDato":"2021-12-30"}""",
                        String((request.body as TextContent).bytes())
                    )
                    respond(inntektRespons, headers = headersOf("Content-Type", "application/json"))
                }
            ),
        ).hentKlassifisertInntekt(UUID.fromString("41621ac0-f5ee-4cce-b1f5-88a79f25f1a5"), "123", LocalDate.now())
        assertEquals(BigDecimal("0"), response.inntektSiste12mnd(false))
        assertEquals(BigDecimal("18900"), response.inntektSiste3år(false))
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
