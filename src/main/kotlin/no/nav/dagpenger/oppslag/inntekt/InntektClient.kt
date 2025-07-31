package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.serialization.JsonConvertException
import mu.KotlinLogging
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import org.slf4j.MDC
import java.time.LocalDate
import java.time.YearMonth

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_api_metrics"),
    private val tokenProvider: () -> String,
) {
    private val sikkerlogg = KotlinLogging.logger("tjenestekall.InntektClient")

    private val baseUrl = URLBuilder(Configuration.inntektApiUrl)
    private val inntektV2 =
        URLBuilder(baseUrl).appendPathSegments("v2", "inntekt").build()

    private val inntektV3 =
        URLBuilder(baseUrl).appendPathSegments("v3", "inntekt").build()

    suspend fun harInntekt(
        ident: String,
        måned: YearMonth,
    ): Boolean {
        val url = URLBuilder(inntektV3).appendPathSegments("harInntekt").build()
        val response =
            httpKlient.post(url) {
                accept(ContentType.Application.Json)

                header("Content-Type", "application/json")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")

                MDC.get("behovId")?.let {
                    header(HttpHeaders.XCorrelationId, it)
                    header(HttpHeaders.XRequestId, it)
                }

                setBody(HarInntektRequest(ident, måned))
            }

        val body = response.body<Boolean>()
        sikkerlogg.info { " Sjekket om $ident hadde inntekt (svar=$body) i måend=$måned" }

        return body
    }

    suspend fun hentKlassifisertInntektV3(request: KlassifisertInntektRequestDto): Inntekt {
        val url = URLBuilder(inntektV3).appendPathSegments("klassifisert").build()
        val response =
            httpKlient.post(url) {
                header("Content-Type", "application/json")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                accept(ContentType.Application.Json)
                setBody(
                    request,
                )
            }
        return hentInntekt(response)
    }

    suspend fun hentInntekt(inntektId: String): Inntekt {
        val url = URLBuilder(inntektV2).appendPathSegments("klassifisert", inntektId).build()
        val response =
            httpKlient.get(url) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                accept(ContentType.Application.Json)
                header(HttpHeaders.XCorrelationId, MDC.get("behovId"))
            }
        return hentInntekt(response)
    }

    private suspend fun hentInntekt(response: HttpResponse) =
        try {
            response.body<Inntekt>()
        } catch (e: JsonConvertException) {
            val body = response.bodyAsText()
            sikkerlogg.error { "Feil ved oppslag på inntekt. Respons: $body" }
            throw e
        }
}

internal data class KlassifisertInntektRequestDto(
    val personIdentifikator: String,
    val regelkontekst: RegelKontekst,
    val beregningsDato: LocalDate,
    val periodeFraOgMed: YearMonth,
    val periodeTilOgMed: YearMonth,
)

data class HarInntektRequest(
    val ident: String,
    val måned: YearMonth,
)

data class RegelKontekst(
    val id: String,
    val type: String,
)
