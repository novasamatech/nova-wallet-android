package io.novafoundation.nova.feature_settings_impl.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import java.lang.Exception
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class AliveNetworkNodeValidation(
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory
) : Validation<NetworkNodePayload, NetworkNodeFailure> {

    override suspend fun validate(value: NetworkNodePayload): ValidationStatus<NetworkNodeFailure> {
        val nodeChainIdRepository = nodeChainIdRepositoryFactory.create(value.chain, value.nodeUrl)

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

fun ValidationSystemBuilder<NetworkNodePayload, NetworkNodeFailure>.validNodeUrl(
    nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory
) = validate(
    AliveNetworkNodeValidation(nodeChainIdRepositoryFactory)
)
