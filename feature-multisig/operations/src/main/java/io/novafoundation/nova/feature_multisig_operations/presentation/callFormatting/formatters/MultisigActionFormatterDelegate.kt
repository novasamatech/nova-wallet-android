package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetIdWithAmount
import io.novafoundation.nova.runtime.extrinsic.visitor.call.api.CallVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MultisigActionFormatterDelegate {

    suspend fun formatPreview(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegatePreviewResult?

    suspend fun formatDetails(visit: CallVisit, chain: Chain): MultisigActionFormatterDelegateDetailsResult?

    suspend fun formatMessageCall(visit: CallVisit, chain: Chain): String?
}

class MultisigActionFormatterDelegatePreviewResult(
    val title: String,
    val subtitle: String?,
    val primaryValue: String?,
    val icon: Icon,
)

class MultisigActionFormatterDelegateDetailsResult(
    val title: String,
    val primaryAmount: ChainAssetIdWithAmount?,
    val tableEntries: List<TableEntry>,
) {

    class TableEntry(
        val name: String,
        val value: TableValue
    )

    sealed class TableValue {

        class Account(val accountId: AccountIdKey, val chain: Chain) : TableValue()
    }
}
