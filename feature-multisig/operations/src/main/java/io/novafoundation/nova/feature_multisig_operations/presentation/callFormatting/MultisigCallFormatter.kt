package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

interface MultisigCallFormatter {

    suspend fun formatMultisigCall(
        call: GenericCall.Instance?,
        initialOrigin: AccountIdKey,
        chain: Chain,
    ): MultisigCallPreviewModel
}
