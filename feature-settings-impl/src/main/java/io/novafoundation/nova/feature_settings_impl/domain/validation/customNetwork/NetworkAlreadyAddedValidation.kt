package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepository
import java.lang.Exception
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class NetworkAlreadyAddedValidation<P, F>(
    private val nodeChainIdRepository: NodeChainIdRepository,
    private val failure: (P) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        return try {
            withTimeout(10.seconds) { nodeChainIdRepository.requestChainId() }

            valid()
        } catch (e: Exception) {
            validationError(failure(value))
        }
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.validateNetworkAlreadyAdded(
    nodeChainIdRepository: NodeChainIdRepository,
    failure: (P) -> F
) = validate(
    NetworkNodeIsAliveValidation(nodeChainIdRepository, failure)
)
