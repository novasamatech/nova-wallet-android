package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class MultisigCallDetailsModel(
    val title: String,
    val primaryAmount: AmountModel?,
    val tableEntries: List<TableEntry>,
    val onBehalfOf: AddressModel?
) {

    class TableEntry(
        val name: String,
        val value: TableValue
    )

    sealed class TableValue {

        class Account(val addressModel: AddressModel, val chain: Chain) : TableValue()
    }
}
