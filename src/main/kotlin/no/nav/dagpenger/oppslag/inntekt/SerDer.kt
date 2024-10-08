package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage

internal fun JsonMessage.aktorId(): String? =
    this["identer"].firstOrNull { it["type"].asText() == "aktørid" && !it["historisk"].asBoolean() }?.get("id")?.asText()

internal fun JsonMessage.fodselsnummer(): String? =
    this["identer"].firstOrNull { it["type"].asText() == "folkeregisterident" && !it["historisk"].asBoolean() }?.get("id")?.asText()

internal fun harAktørEllerFnr(jsonNode: JsonNode) {
    require(
        jsonNode.any { node -> node["type"].asText() == "aktørid" } ||
            jsonNode.any { node -> node["type"].asText() == "folkeregisterident" },
    ) { "Mangler aktørid eller folkeregisterident i identer" }
}
