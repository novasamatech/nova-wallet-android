package io.novafoundation.nova.feature_multisig_operations.presentation.list.model

import io.novafoundation.nova.common.presentation.ColoredDrawable
import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallPreviewModel

data class PendingMultisigOperationModel(
    val id: String,
    val chain: ChainUi,
    val action: SigningAction?,
    val call: MultisigCallPreviewModel,
    val time: String?,
    val progress: String
) {

    class SigningAction(
        val text: ColoredText,
        val icon: ColoredDrawable?
    )
}

