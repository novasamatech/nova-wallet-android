package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById

class NetworkAlreadyAddedValidation<P, F>(
    private val chainRegistry: ChainRegistry,
    private val chainIdRequester: suspend (P) -> String,
    private val ignoreChainModifying: (P) -> Boolean,
    private val defaultNetworkFailure: (P, Chain) -> F,
    private val customNetworkWarning: (P, Chain) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val chainId = chainIdRequester(value)

        val chain = chainRegistry.chainsById()[chainId]
        if (chain != null && !ignoreChainModifying(value)) {
            return when (chain.source) {
                Chain.Source.DEFAULT -> validationError(defaultNetworkFailure(value, chain))
                Chain.Source.CUSTOM -> validationError(customNetworkWarning(value, chain))
            }
        }

        return valid()
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.validateNetworkNotAdded(
    chainRegistry: ChainRegistry,
    chainIdRequester: suspend (P) -> String,
    ignoreChainModifying: (P) -> Boolean,
    defaultNetworkFailure: (P, Chain) -> F,
    customNetworkFailure: (P, Chain) -> F
) = validate(
    NetworkAlreadyAddedValidation(chainRegistry, chainIdRequester, ignoreChainModifying, defaultNetworkFailure, customNetworkFailure)
)
