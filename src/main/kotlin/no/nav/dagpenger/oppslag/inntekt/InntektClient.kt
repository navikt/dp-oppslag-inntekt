package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import mu.KotlinLogging
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import java.time.LocalDate
import java.util.UUID

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_api_metrics"),
    private val tokenProvider: () -> String,
) {
    suspend fun hentKlassifisertInntekt(søknadUUID: UUID, aktørId: String, virkningsTidspunkt: LocalDate): Inntekt {
        val inntekt = httpKlient.post<no.nav.dagpenger.events.inntekt.v1.Inntekt>(Configuration.inntektApiUrl) {
            header("Content-Type", "application/json")
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            body = InntektRequest(aktørId, RegelKontekst(id = søknadUUID.toString(), type = "saksbehandling"), virkningsTidspunkt)
            accept(ContentType.Application.Json)
        }
        sikkerLogg.info { inntekt }
        return Inntekt(inntekt)
    }
}

internal data class InntektRequest(val aktørId: String, val regelkontekst: RegelKontekst, val beregningsDato: LocalDate)

data class RegelKontekst(val id: String, val type: String)
