package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class LocalMultisigCallDataWatcher(
    private val chainRegistry: ChainRegistry,
    multisigOperationLocalCallRepository: MultisigOperationLocalCallRepository
) : MultisigCallDataWatcher {

    override val newMultisigEvents = MutableSharedFlow<MultiChainMultisigEvent>(extraBufferCapacity = 0)

    override val callData = multisigOperationLocalCallRepository.callsFlow().map { operations ->
        operations.associateBy { it.chainId to it.callHash.fromHex().intoKey() }
            .mapValues { (_, value) ->
                val runtime = getRuntime(value.chainId) ?: return@mapValues null
                GenericCall.fromHex(runtime, value.callInstance)
            }.filterNotNull()
    }

    private suspend fun getRuntime(chainId: ChainId) = runCatching { chainRegistry.getRuntime(chainId) }.getOrNull()
}
