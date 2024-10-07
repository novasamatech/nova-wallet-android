package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

sealed class ReferendumCallModel {

    class GovernanceRequest(val amount: AmountModel) : ReferendumCallModel()
}
