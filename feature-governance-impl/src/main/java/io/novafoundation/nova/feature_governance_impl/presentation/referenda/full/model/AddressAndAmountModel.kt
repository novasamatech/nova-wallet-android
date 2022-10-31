package io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class AddressAndAmountModel(
    val addressModel: AddressModel,
    val amountModel: AmountModel?
)
