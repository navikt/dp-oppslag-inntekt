package no.nav.dagpenger.oppslag.inntekt

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

internal fun JsonMessage.fodselsnummer(): String? =
    this.ident()
        ?: this["identer"]
            .firstOrNull { it["type"].asText() == "folkeregisterident" && !it["historisk"].asBoolean() }
            ?.get("id")
            ?.asText()

internal fun JsonMessage.ident(): String? = this["ident"].asText()

internal fun harAktørEllerFnr(jsonNode: JsonNode) {
    require(
        jsonNode.any { node -> node["type"].asText() == "aktørid" } ||
            jsonNode.any { node -> node["type"].asText() == "folkeregisterident" },
    ) { "Mangler aktørid eller folkeregisterident i identer" }
}
