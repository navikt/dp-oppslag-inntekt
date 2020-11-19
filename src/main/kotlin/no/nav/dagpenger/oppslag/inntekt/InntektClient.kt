package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

internal class InntektClient {
    val mapper = jacksonObjectMapper()
    fun hentKlassifisertInntekt(aktørId: String, virkningsTidspunkt: LocalDate): Inntekt {

        /*val (_, response, result) = with(url.httpPost()) {
            header("Content-Type" to "application/json")
            header("X-API-KEY", apiKey)
            body(jsonBody)
            responseObject(moshiDeserializerOf(adapter))
        }*/


        val inntekt = mapper.readValue("", object : TypeReference<no.nav.dagpenger.events.inntekt.v1.Inntekt>(){})

        return Inntekt(inntekt)
    }
}


