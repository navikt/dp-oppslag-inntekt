package no.nav.dagpenger.oppslag.inntektimport

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import no.nav.dagpenger.oppslag.inntekt.Configuration
import no.nav.dagpenger.oppslag.inntekt.Inntekt
import no.nav.dagpenger.oppslag.inntekt.http.httpClient
import java.time.LocalDate

internal class InntektClient(
    private val httpKlient: HttpClient = httpClient()
) {
    suspend fun hentKlassifisertInntekt(aktørId: String, virkningsTidspunkt: LocalDate): Inntekt {
        val inntekt = httpKlient.post<no.nav.dagpenger.events.inntekt.v1.Inntekt>(Configuration.inntektApiUrl) {
            this.headers.append("Content-Type", "application/json")
            this.headers.append("X-API-KEY", Configuration.inntektApiKey)
            this.body = InntektRequest(aktørId, "-3000", virkningsTidspunkt)
        }

        return Inntekt(inntekt)
    }
}

private data class InntektRequest(val aktørId: String, val vedtakId: String, val beregningsDato: LocalDate)
