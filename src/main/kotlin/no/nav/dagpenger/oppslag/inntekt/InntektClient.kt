package no.nav.dagpenger.oppslag.inntekt

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.http.ContentType
import mu.KotlinLogging
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import java.time.LocalDate

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_api_metrics")
) {
    suspend fun hentKlassifisertInntekt(aktørId: String, virkningsTidspunkt: LocalDate): Inntekt {
        val inntekt = httpKlient.post<no.nav.dagpenger.events.inntekt.v1.Inntekt>(Configuration.inntektApiUrl) {
            this.headers.append("Content-Type", "application/json")
            this.headers.append("X-API-KEY", Configuration.inntektApiKey)
            this.body = InntektRequest(aktørId, RegelKontekst(id = "-3000", type = "SAKSBEHANDLING"), virkningsTidspunkt)
            accept(ContentType.Application.Json)
        }
        sikkerLogg.info { inntekt }
        return Inntekt(inntekt)
    }
}

internal data class InntektRequest(val aktørId: String, val regelkontekst: RegelKontekst, val beregningsDato: LocalDate)

data class RegelKontekst(val id: String, val type: String)
