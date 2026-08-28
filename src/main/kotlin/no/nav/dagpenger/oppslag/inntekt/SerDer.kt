package no.nav.dagpenger.oppslag.inntekt

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

internal fun JsonMessage.ident(): String? = this["ident"].asString()
