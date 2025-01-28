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
import io.ktor.http.Url
import io.ktor.http.appendEncodedPathSegments
import io.ktor.serialization.JsonConvertException
import mu.KotlinLogging
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import org.slf4j.MDC
import java.time.LocalDate
import java.util.UUID

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_api_metrics"),
    private val tokenProvider: () -> String,
) {
    private val sikkerLogg = KotlinLogging.logger("tjenestekall.InntektClient")

    suspend fun hentKlassifisertInntekt(
        behandlingId: UUID,
        aktørId: String? = null,
        fødselsnummer: String? = null,
        prøvingsdato: LocalDate,
        callId: String? = null,
    ): Inntekt {
        sikkerLogg.info { "Henter inntekt for aktørId=$aktørId, fødselsnummer=$fødselsnummer" }
        val response =
            httpKlient.post(Url(Configuration.inntektApiUrl)) {
                header("Content-Type", "application/json")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                callId?.let { header(HttpHeaders.XCorrelationId, it) }
                callId?.let { header(HttpHeaders.XRequestId, it) }
                accept(ContentType.Application.Json)
                setBody(
                    InntektRequest(
                        aktørId = aktørId,
                        fødselsnummer = fødselsnummer,
                        regelkontekst = RegelKontekst(id = behandlingId.toString(), type = "saksbehandling"),
                        beregningsDato = prøvingsdato,
                    ),
                )
            }

        val inntekt = hentInntekt(response)
        sikkerLogg.info {
            """
            |Hentet inntekt med id=${inntekt.inntektsId}, 
            |sisteAvsluttedeKalenderMåned=${inntekt.sisteAvsluttendeKalenderMåned}
            """.trimMargin()
        }
        return inntekt
    }

    suspend fun hentInntekt(inntektId: String): Inntekt {
        val url = URLBuilder(Configuration.inntektApiUrl).appendEncodedPathSegments(inntektId).build()
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
            sikkerLogg.error { "Feil ved oppslag på inntekt. Respons: $body" }
            throw e
        }
}

internal data class InntektRequest(
    val aktørId: String?,
    val fødselsnummer: String? = null,
    val regelkontekst: RegelKontekst,
    val beregningsDato: LocalDate,
) {
    init {
        require(aktørId != null || fødselsnummer !== null) {
            "Enten aktørId eller fødselsnummer må være satt"
        }
    }
}

data class RegelKontekst(
    val id: String,
    val type: String,
)
