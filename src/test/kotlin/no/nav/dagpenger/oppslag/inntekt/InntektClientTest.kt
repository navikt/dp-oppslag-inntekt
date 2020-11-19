package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import no.nav.dagpenger.oppslag.inntektimport.InntektClient
import org.junit.jupiter.api.Test
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
                    // (request.body as TextContent).text `should strictly equal json` "min.json".getResourceAsTextOrFail()
                    assertEquals(Configuration.inntektApiUrl, request.url.toString())
                    assertEquals(Configuration.inntektApiKey, request.headers["X-API-KEY"])
                    respond("noe", HttpStatusCode.BadRequest)
                },
            ),
        ).hentKlassifisertInntekt("123", LocalDate.now())
        // response shouldBe NyPermitteringResponse(123456, NyPermitteringResponse.VedtakStatusKode.REGIS, null)
    }
}
