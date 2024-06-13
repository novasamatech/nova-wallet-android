package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class FillableChainAccountModel(
    val filledAddressModel: AddressModel?,
    val chainUi: ChainUi
)
