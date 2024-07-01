package io.novafoundation.nova.feature_settings_impl.domain.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias NetworkNodeValidationSystem = ValidationSystem<NetworkNodePayload, NetworkNodeFailure>

class NetworkNodePayload(
    val chain: Chain,
    val nodeUrl: String
)

sealed interface NetworkNodeFailure {

    class NodeAlreadyExists(val node: Chain.Node) : NetworkNodeFailure

    class WrongNetwork(val chain: Chain) : NetworkNodeFailure

    object NodeIsNotAlive : NetworkNodeFailure
}
