package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepository

typealias CustomNetworkValidationSystem = ValidationSystem<CustomNetworkPayload, CustomNetworkFailure>
typealias CustomNetworkValidationSystemBuilder = ValidationSystemBuilder<CustomNetworkPayload, CustomNetworkFailure>

class CustomNetworkPayload(
    val nodeUrl: String,
    val chainName: String,
    val tokenSymbol: String,
    val chainId: String?,
    val blockExplorerUrl: String?,
    val coingeckoLinkUrl: String?
)

sealed interface CustomNetworkFailure {

    class NetworkAlreadyAdded() : CustomNetworkFailure

    class WrongNetwork(val chainName: String) : CustomNetworkFailure

    object NodeIsNotAlive : CustomNetworkFailure

    class BlockExplorerBadFormat() : CustomNetworkFailure

    class CoingeckoLinkBadFormat() : CustomNetworkFailure
}

fun CustomNetworkValidationSystemBuilder.validateNetworkNodeIsAlive(
    nodeHealthStateCheckRequest: suspend (CustomNetworkPayload) -> Unit
) = validateNetworkNodeIsAlive(nodeHealthStateCheckRequest, failure = { CustomNetworkFailure.NodeIsNotAlive })
