package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MultisigActionFormatterDelegate {

    suspend fun formatAction(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateResult?
}

class MultisigActionFormatterDelegateResult(
    val title: String,
    val subtitle: String?,
    val primaryValue: String?,
    val icon: Icon,
)
