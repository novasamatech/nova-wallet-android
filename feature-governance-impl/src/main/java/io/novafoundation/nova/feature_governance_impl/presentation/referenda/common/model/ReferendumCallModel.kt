package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

sealed class ReferendumCallModel {

    sealed class GovernanceRequest(val amount: AmountModel) : ReferendumCallModel() {

        class AmountOnly(amount: AmountModel) : GovernanceRequest(amount)

        class Full(amount: AmountModel, val beneficiary: AddressModel) : GovernanceRequest(amount)
    }
}
