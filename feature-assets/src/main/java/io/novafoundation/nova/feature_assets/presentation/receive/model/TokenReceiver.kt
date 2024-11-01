package io.novafoundation.nova.feature_assets.presentation.receive.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class TokenReceiver(
    val addressModel: AddressModel,
    val chain: ChainUi,
    val chainAssetIcon: Icon
)
