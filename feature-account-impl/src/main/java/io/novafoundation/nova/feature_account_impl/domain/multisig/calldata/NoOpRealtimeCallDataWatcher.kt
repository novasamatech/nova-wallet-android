package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

object NoOpRealtimeCallDataWatcher : RealtimeCallDataWatcher {
    override val newMultisigEvents: Flow<MultiChainMultisigEvent> = emptyFlow()

    override val realtimeCallData: StateFlow<Map<MultiChainCallHash, GenericCall.Instance>> = MutableStateFlow(emptyMap())
}
