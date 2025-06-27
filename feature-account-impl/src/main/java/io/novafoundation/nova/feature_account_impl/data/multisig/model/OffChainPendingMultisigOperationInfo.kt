package io.novafoundation.nova.feature_account_impl.data.multisig.model

import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlin.time.Duration

class OffChainPendingMultisigOperationInfo(
    val timestamp: Duration,
    val callHash: CallHash,
    val callData: GenericCall.Instance?
)
