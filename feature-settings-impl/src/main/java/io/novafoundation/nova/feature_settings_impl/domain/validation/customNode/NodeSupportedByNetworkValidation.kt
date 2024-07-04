package io.novafoundation.nova.feature_settings_impl.domain.validation.customNode

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import java.lang.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class NodeSupportedByNetworkValidation<P, F>(
    private val nodeChainIdRequester: suspend (P) -> String,
    private val originalChainId: (P) -> String?,
    private val failure: (P) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val nodeChainId = nodeChainIdRequester(value)

        return validOrError(nodeChainId == originalChainId(value)) {
            failure(value)
        }
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.nodeSupportedByNetworkValidation(
    nodeChainIdRequester: suspend (P) -> String,
    originalChainId: (P) -> String?,
    failure: (P) -> F
) = validate(
    NodeSupportedByNetworkValidation(
        nodeChainIdRequester,
        originalChainId,
        failure
    )
)
