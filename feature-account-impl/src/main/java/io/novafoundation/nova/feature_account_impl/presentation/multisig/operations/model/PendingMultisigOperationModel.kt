package io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.model

import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class PendingMultisigOperationModel(
    val id: String,
    val chain: ChainUi,
    val action: ColoredText,
    val operationTitle: String,
    val progress: String
)
