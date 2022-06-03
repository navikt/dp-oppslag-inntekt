package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.path
import mu.KotlinLogging
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import java.time.LocalDate
import java.util.UUID

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_api_metrics"),
    private val tokenProvider: () -> String,
) {
    suspend fun hentKlassifisertInntekt(søknadUUID: UUID, aktørId: String, virkningsTidspunkt: LocalDate): OppslagInntekt {
       val inntekt =  httpKlient.post(Url(Configuration.inntektApiUrl)) {
            header("Content-Type", "application/json")
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            accept(ContentType.Application.Json)
            setBody(InntektRequest(aktørId, RegelKontekst(id = søknadUUID.toString(), type = "saksbehandling"), virkningsTidspunkt))
        }.body<Inntekt>()
        sikkerLogg.info { inntekt }
        return OppslagInntekt(inntekt)
    }
}

internal data class InntektRequest(val aktørId: String, val regelkontekst: RegelKontekst, val beregningsDato: LocalDate)

data class RegelKontekst(val id: String, val type: String)
