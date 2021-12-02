package io.novafoundation.nova.feature_wallet_impl.presentation.receive.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class TokenReceiver(
    val addressModel: AddressModel,
    val chain: ChainUi,
)
