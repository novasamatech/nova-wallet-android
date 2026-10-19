package io.novafoundation.nova.feature_wallet_api.domain.model

/** Shared by submission and history; matches iOS SubtensorNovaFeeConstants. */
object BittensorServiceFeeRecipients {

    const val CURRENT_ADDRESS = "5EAjYyJ8JaxyvHTSNuFp8fqjerTETjqhoM7MdZux2Ehzjxdq"

    val historicalAddresses: List<String> = emptyList()

    val historyAddresses: List<String> = listOf(CURRENT_ADDRESS) + historicalAddresses
}
