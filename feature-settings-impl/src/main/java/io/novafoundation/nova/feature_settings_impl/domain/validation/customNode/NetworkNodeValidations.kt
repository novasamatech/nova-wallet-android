package io.novafoundation.nova.feature_settings_impl.domain.validation.customNode

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias NetworkNodeValidationSystem = ValidationSystem<NetworkNodePayload, NetworkNodeFailure>
typealias NetworkNodeValidationSystemBuilder = ValidationSystemBuilder<NetworkNodePayload, NetworkNodeFailure>

class NetworkNodePayload(
    val chain: Chain,
    val nodeUrl: String
)

sealed interface NetworkNodeFailure {

    class NodeAlreadyExists(val node: Chain.Node) : NetworkNodeFailure

    class WrongNetwork(val chain: Chain) : NetworkNodeFailure

    object NodeIsNotAlive : NetworkNodeFailure
}

fun NetworkNodeValidationSystemBuilder.validateNetworkNodeIsAlive(
    nodeHealthStateCheckRequest: suspend (NetworkNodePayload) -> Unit
) = validateNetworkNodeIsAlive(nodeHealthStateCheckRequest, failure = { NetworkNodeFailure.NodeIsNotAlive })

fun NetworkNodeValidationSystemBuilder.validateNodeSupportedByNetwork(
    nodeChainIdRequester: suspend (NetworkNodePayload) -> String
) = nodeSupportedByNetworkValidation(
    nodeChainIdRequester = { nodeChainIdRequester(it) },
    originalChainId = { it.chain.id },
    failure = { NetworkNodeFailure.WrongNetwork(it.chain) }
)
