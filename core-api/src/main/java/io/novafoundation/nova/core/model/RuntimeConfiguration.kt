@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package io.novafoundation.nova.core.model

class RuntimeConfiguration(
    val genesisHash: String,
    val erasPerDay: Int,
    val addressByte: Short,
)
