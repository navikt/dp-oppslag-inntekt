package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import no.nav.dagpenger.oppslag.inntektimport.InntektClient
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
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
                    respond(inntektRespons, headers = headersOf("Content-Type", "application/json"))
                },
            ),
        ).hentKlassifisertInntekt("123", LocalDate.now())
        assertEquals(BigDecimal("0"), response.inntektSiste12mnd(false))
    }
}

val inntektRespons =
    """
{
"inntektsId": "abc",
"inntektsListe": [],
"manueltRedigert": false,
"sisteAvsluttendeKalenderMåned": "2020-10"
}
    """.trimIndent()
