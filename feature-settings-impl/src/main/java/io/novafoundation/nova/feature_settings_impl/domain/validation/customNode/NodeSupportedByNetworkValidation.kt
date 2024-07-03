package io.novafoundation.nova.feature_settings_impl.domain.validation.customNode

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.runtime.ext.networkType
import java.lang.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class NodeSupportedByNetworkValidation(
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
    private val coroutineScope: CoroutineScope
) : Validation<NetworkNodePayload, NetworkNodeFailure> {

    override suspend fun validate(value: NetworkNodePayload): ValidationStatus<NetworkNodeFailure> {
        val nodeChainIdRepository = nodeChainIdRepositoryFactory.create(value.chain.networkType(), value.nodeUrl, coroutineScope)

        return try {
            val requestedChainId = withTimeout(10.seconds) {
                nodeChainIdRepository.requestChainId()
            }

            validOrError(requestedChainId == value.chain.id) {
                NetworkNodeFailure.WrongNetwork(value.chain)
            }
        } catch (e: Exception) {
            validationError(NetworkNodeFailure.NodeIsNotAlive)
        }
    }
}

fun ValidationSystemBuilder<NetworkNodePayload, NetworkNodeFailure>.nodeSupportedByNetworkValidation(
    nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
    coroutineScope: CoroutineScope
) = validate(
    NodeSupportedByNetworkValidation(nodeChainIdRepositoryFactory, coroutineScope)
)
