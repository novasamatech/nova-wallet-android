package io.novafoundation.nova.feature_account_impl.domain.multisig.calldata

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.flow.Flow

interface MultisigCallDataWatcher {

    val newMultisigEvents: Flow<MultiChainMultisigEvent>

    val callData: Flow<Map<MultiChainCallHash, GenericCall.Instance>>
}

class MultiChainMultisigEvent(
    val multisig: AccountIdKey,
    val callHash: CallHash,
    val chainId: ChainId
)

typealias MultiChainCallHash = Pair<ChainId, CallHash>
